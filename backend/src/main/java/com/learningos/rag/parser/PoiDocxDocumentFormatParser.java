package com.learningos.rag.parser;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PoiDocxDocumentFormatParser implements DocumentFormatParser {

    private static final Pattern DOCX_TEXT_EVENT = Pattern.compile(
            "<w:t(?:\\s[^>]*)?>(.*?)</w:t>|<w:tab\\s*/>|<w:br\\b([^>]*)/?>|<w:lastRenderedPageBreak\\s*/>",
            Pattern.DOTALL
    );
    private static final Pattern DOCX_PAGE_BREAK_ATTRIBUTE = Pattern.compile("\\bw:type=[\"']page[\"']");

    @Override
    public DocumentParser format() {
        return DocumentParser.DOCX;
    }

    @Override
    public ParsedDocument parse(ParseInput input) throws IOException {
        ParserResourceLimits.requireInputWithinLimit(input);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(input.bytes()))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            if (paragraphs.size() > ParserResourceLimits.MAX_DOCX_PARAGRAPHS) {
                throw new IllegalArgumentException("DOCX_PARAGRAPH_LIMIT_EXCEEDED");
            }
            return parseBodyElements(document.getBodyElements());
        }
    }

    private ParsedDocument parseBodyElements(List<IBodyElement> bodyElements) {
        List<ParsedSection> sections = new ArrayList<>();
        String[] headingStack = new String[6];
        String currentTitle = null;
        Integer currentHeadingLevel = null;
        List<String> currentHeadingPath = List.of();
        int pageNum = 1;
        int extractedChars = 0;
        for (IBodyElement element : bodyElements) {
            if (element instanceof XWPFTable table) {
                DocxTableContent tableContent = extractTableContent(table, pageNum);
                pageNum = tableContent.nextPageNum();
                if (!tableContent.content().isBlank()) {
                    extractedChars += tableContent.content().length();
                    if (extractedChars > ParserResourceLimits.MAX_EXTRACTED_CHARS) {
                        throw new IllegalArgumentException("PARSER_OUTPUT_LIMIT_EXCEEDED");
                    }
                    sections.add(tableSection(
                            currentTitle,
                            currentHeadingLevel,
                            currentHeadingPath,
                            tableContent.content(),
                            tableContent.pageNum()
                    ));
                }
                continue;
            }
            if (!(element instanceof XWPFParagraph paragraph) || isTocParagraph(paragraph)) {
                continue;
            }
            DocxParagraphContent paragraphContent = extractParagraphContent(paragraph, pageNum);
            pageNum = paragraphContent.nextPageNum();
            Integer headingLevel = headingLevel(paragraph);
            if (headingLevel != null) {
                String title = paragraphContent.joinedText();
                if (title.isBlank()) {
                    continue;
                }
                currentHeadingLevel = headingLevel;
                currentTitle = title;
                headingStack[headingLevel - 1] = currentTitle;
                for (int index = headingLevel; index < headingStack.length; index++) {
                    headingStack[index] = null;
                }
                currentHeadingPath = headingPath(headingStack);
                continue;
            }
            for (DocxTextSegment segment : paragraphContent.segments()) {
                extractedChars += segment.content().length();
                if (extractedChars > ParserResourceLimits.MAX_EXTRACTED_CHARS) {
                    throw new IllegalArgumentException("PARSER_OUTPUT_LIMIT_EXCEEDED");
                }
                sections.add(new ParsedSection(
                        currentTitle,
                        currentHeadingLevel,
                        currentHeadingPath,
                        segment.content(),
                        segment.pageNum()
                ));
            }
        }
        return new ParsedDocument(format(), List.copyOf(sections));
    }

    private ParsedSection tableSection(
            String currentTitle,
            Integer currentHeadingLevel,
            List<String> currentHeadingPath,
            String content,
            Integer pageNum
    ) {
        return new ParsedSection(
                currentTitle,
                currentHeadingLevel,
                currentHeadingPath,
                content,
                pageNum,
                pageNum == null ? "NONE" : "PARSER_INFERRED",
                null,
                null,
                null,
                "TABLE_TEXT"
        );
    }

    private Integer headingLevel(XWPFParagraph paragraph) {
        String style = paragraph == null ? null : paragraph.getStyle();
        if (style == null || style.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?i)^heading\\s*([1-6])$").matcher(style.replace("_", ""));
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private boolean isTocParagraph(XWPFParagraph paragraph) {
        String style = paragraph == null ? null : paragraph.getStyle();
        if (style != null && style.trim().toUpperCase(Locale.ROOT).replace("_", "").startsWith("TOC")) {
            return true;
        }
        String xml = paragraph == null ? "" : paragraph.getCTP().xmlText();
        return xml.toLowerCase(Locale.ROOT).contains("instrtext")
                && xml.toUpperCase(Locale.ROOT).contains("TOC");
    }

    private DocxParagraphContent extractParagraphContent(XWPFParagraph paragraph, int initialPageNum) {
        List<DocxTextSegment> segments = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int pageNum = initialPageNum;
        Matcher matcher = DOCX_TEXT_EVENT.matcher(paragraph.getCTP().xmlText());
        while (matcher.find()) {
            String token = matcher.group();
            if (matcher.group(1) != null) {
                builder.append(matcher.group(1)).append(' ');
                continue;
            }
            if (token.startsWith("<w:tab")) {
                builder.append(' ');
                continue;
            }
            if (token.startsWith("<w:lastRenderedPageBreak")) {
                addSegment(segments, builder, pageNum);
                pageNum++;
                continue;
            }
            if (token.startsWith("<w:br") && matcher.group(2) != null && DOCX_PAGE_BREAK_ATTRIBUTE.matcher(matcher.group(2)).find()) {
                addSegment(segments, builder, pageNum);
                pageNum++;
                continue;
            }
            builder.append(' ');
        }
        addSegment(segments, builder, pageNum);
        return new DocxParagraphContent(List.copyOf(segments), pageNum);
    }

    private DocxTableContent extractTableContent(XWPFTable table, int initialPageNum) {
        List<String> rowTexts = new ArrayList<>();
        int pageNum = initialPageNum;
        for (XWPFTableRow row : table.getRows()) {
            List<String> cellTexts = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                StringBuilder cellBuilder = new StringBuilder();
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    if (isTocParagraph(paragraph)) {
                        continue;
                    }
                    DocxParagraphContent paragraphContent = extractParagraphContent(paragraph, pageNum);
                    pageNum = paragraphContent.nextPageNum();
                    ParserTextSanitizer.appendCleaned(cellBuilder, paragraphContent.joinedText());
                }
                String cellText = ParserTextSanitizer.clean(cellBuilder.toString());
                if (!cellText.isBlank()) {
                    cellTexts.add(cellText);
                }
            }
            if (!cellTexts.isEmpty()) {
                rowTexts.add(String.join(" | ", cellTexts));
            }
        }
        String content = ParserTextSanitizer.clean(String.join("; ", rowTexts));
        return new DocxTableContent(content, initialPageNum, pageNum);
    }

    private void addSegment(List<DocxTextSegment> segments, StringBuilder builder, int pageNum) {
        String content = ParserTextSanitizer.clean(unescapeXml(builder.toString()));
        if (!content.isBlank()) {
            segments.add(new DocxTextSegment(content, pageNum));
        }
        builder.setLength(0);
    }

    private List<String> headingPath(String[] headingStack) {
        List<String> path = new ArrayList<>();
        for (String heading : headingStack) {
            if (heading != null && !heading.isBlank()) {
                path.add(heading);
            }
        }
        return List.copyOf(path);
    }

    private String unescapeXml(String value) {
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private record DocxParagraphContent(List<DocxTextSegment> segments, int nextPageNum) {

        private String joinedText() {
            StringBuilder builder = new StringBuilder();
            for (DocxTextSegment segment : segments) {
                ParserTextSanitizer.appendCleaned(builder, segment.content());
            }
            return builder.toString();
        }
    }

    private record DocxTextSegment(String content, int pageNum) {
    }

    private record DocxTableContent(String content, int pageNum, int nextPageNum) {
    }
}
