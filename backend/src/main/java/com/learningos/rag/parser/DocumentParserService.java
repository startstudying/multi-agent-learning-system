package com.learningos.rag.parser;

import com.learningos.rag.domain.KbDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

@Service
public class DocumentParserService {

    private static final int MAX_DOCX_ENTRIES = 64;
    private static final int MAX_DOCX_XML_BYTES = 2 * 1024 * 1024;
    private static final int ZIP_LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50;
    private static final int ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014B50;
    private static final int ZIP_END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50;
    private static final int ZIP_METHOD_STORED = 0;
    private static final int ZIP_METHOD_DEFLATED = 8;
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern DOCX_BODY_ELEMENT = Pattern.compile(
            "(<w:p(?:\\s[^>]*)?>.*?</w:p>)|(<w:tbl(?:\\s[^>]*)?>.*?</w:tbl>)",
            Pattern.DOTALL
    );
    private static final Pattern DOCX_TABLE_ROW = Pattern.compile("<w:tr(?:\\s[^>]*)?>(.*?)</w:tr>", Pattern.DOTALL);
    private static final Pattern DOCX_TABLE_CELL = Pattern.compile("<w:tc(?:\\s[^>]*)?>(.*?)</w:tc>", Pattern.DOTALL);
    private static final Pattern DOCX_CELL_PARAGRAPH = Pattern.compile("<w:p(?:\\s[^>]*)?>(.*?)</w:p>", Pattern.DOTALL);
    private static final Pattern DOCX_HEADING_STYLE = Pattern.compile(
            "<w:pStyle\\b[^>]*\\bw:val=[\"']Heading([1-6])[\"'][^>]*/?>",
            Pattern.DOTALL
    );
    private static final Pattern DOCX_TOC_STYLE = Pattern.compile(
            "<w:pStyle\\b[^>]*\\bw:val=[\"']TOC[^\"']*[\"'][^>]*/?>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DOCX_TOC_FIELD = Pattern.compile(
            "<w:instrText\\b[^>]*>\\s*TOC\\b.*?</w:instrText>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern DOCX_PAGE_BREAK = Pattern.compile(
            "<w:lastRenderedPageBreak\\s*/>|<w:br\\b[^>]*\\bw:type=[\"']page[\"'][^>]*/?>",
            Pattern.DOTALL
    );
    private static final Pattern DOCX_TEXT = Pattern.compile("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", Pattern.DOTALL);
    private static final Pattern DOCX_TEXT_EVENT = Pattern.compile(
            "<w:t(?:\\s[^>]*)?>(.*?)</w:t>|<w:tab\\s*/>|<w:br\\b([^>]*)/?>|<w:lastRenderedPageBreak\\s*/>",
            Pattern.DOTALL
    );
    private static final Pattern DOCX_PAGE_BREAK_ATTRIBUTE = Pattern.compile("\\bw:type=[\"']page[\"']");
    private static final Pattern PDF_PAGE_OBJECT = Pattern.compile("/Type\\s*/Page(?!s)\\b");
    private static final Pattern PDF_STRING = Pattern.compile("\\(((?:\\\\.|[^\\\\()])*)\\)", Pattern.DOTALL);
    private static final Pattern PDF_TEXT_OBJECT = Pattern.compile(
            "\\(((?:\\\\.|[^\\\\()])*)\\)\\s*Tj",
            Pattern.DOTALL
    );
    private static final Pattern PDF_TEXT_ARRAY = Pattern.compile("\\[(.*?)]\\s*TJ", Pattern.DOTALL);

    private final Map<DocumentParser, DocumentFormatParser> parserRegistry;
    private final OcrFallbackService ocrFallbackService;

    public DocumentParserService() {
        this(List.of(), new NoopOcrFallbackService());
    }

    public DocumentParserService(List<DocumentFormatParser> parsers) {
        this(parsers, new NoopOcrFallbackService());
    }

    public DocumentParserService(List<DocumentFormatParser> parsers, OcrFallbackService ocrFallbackService) {
        this.ocrFallbackService = ocrFallbackService == null ? new NoopOcrFallbackService() : ocrFallbackService;
        this.parserRegistry = buildRegistry(parsers);
    }

    @Autowired
    public DocumentParserService(
            ObjectProvider<DocumentFormatParser> parsers,
            ObjectProvider<OcrFallbackService> ocrFallbackService
    ) {
        this(
                parsers == null ? List.of() : parsers.orderedStream().toList(),
                ocrFallbackService == null
                        ? new NoopOcrFallbackService()
                        : ocrFallbackService.getIfAvailable(NoopOcrFallbackService::new)
        );
    }

    public ParsedDocument parse(KbDocument document, byte[] bytes) {
        byte[] safeBytes = bytes == null ? new byte[0] : bytes;
        DocumentParser parser = parserFor(document);
        DocumentFormatParser provider = parserRegistry.get(parser);
        ParseInput input = new ParseInput(
                document == null ? null : document.getId(),
                document == null ? null : document.getName(),
                document == null ? null : document.getContentType(),
                document == null || document.getSizeBytes() == null ? safeBytes.length : document.getSizeBytes(),
                safeBytes
        );
        try {
            if (provider == null) {
                throw parseFailed(new IllegalArgumentException("UNREGISTERED_PARSER"));
            }
            ParsedDocument parsedDocument = provider.parse(input);
            if (parsedDocument == null) {
                throw parseFailed(new IllegalArgumentException("NULL_PARSED_DOCUMENT"));
            }
            return parsedDocument;
        } catch (DocumentParseException exception) {
            throw exception;
        } catch (IOException exception) {
            throw parseFailed(exception);
        } catch (IllegalArgumentException exception) {
            throw parseFailed(exception);
        } catch (RuntimeException exception) {
            throw parseFailed(exception);
        }
    }

    private Map<DocumentParser, DocumentFormatParser> buildRegistry(List<DocumentFormatParser> parsers) {
        Map<DocumentParser, DocumentFormatParser> registry = new EnumMap<>(DocumentParser.class);
        for (DocumentFormatParser parser : defaultParsers()) {
            registry.put(parser.format(), parser);
        }
        if (parsers != null) {
            for (DocumentFormatParser parser : parsers) {
                if (parser == null || parser.format() == null) {
                    continue;
                }
                registry.put(parser.format(), parser);
            }
        }
        return Map.copyOf(registry);
    }

    private List<DocumentFormatParser> defaultParsers() {
        return List.of(
                new MarkdownDocumentFormatParser(),
                new TextDocumentFormatParser(),
                new PdfLightweightDocumentFormatParser(),
                new DocxLightweightDocumentFormatParser()
        );
    }

    private ParsedDocument parseMarkdown(String text, DocumentParser parser) {
        String normalizedText = stripBom(text);
        List<ParsedSection> sections = new ArrayList<>();
        String currentTitle = null;
        Integer currentHeadingLevel = null;
        List<String> currentHeadingPath = List.of();
        String[] headingStack = new String[6];
        StringBuilder currentContent = new StringBuilder();
        for (String line : normalizedText.split("\\R")) {
            Matcher matcher = MARKDOWN_HEADING.matcher(line.trim());
            if (matcher.matches()) {
                addSection(sections, currentTitle, currentHeadingLevel, currentHeadingPath, currentContent.toString(), parser);
                currentHeadingLevel = matcher.group(1).length();
                currentTitle = clean(matcher.group(2));
                headingStack[currentHeadingLevel - 1] = currentTitle;
                for (int index = currentHeadingLevel; index < headingStack.length; index++) {
                    headingStack[index] = null;
                }
                currentHeadingPath = headingPath(headingStack);
                currentContent.setLength(0);
                continue;
            }
            currentContent.append(line).append('\n');
        }
        addSection(sections, currentTitle, currentHeadingLevel, currentHeadingPath, currentContent.toString(), parser);
        return new ParsedDocument(parser, sections);
    }

    private ParsedDocument parseText(String text, DocumentParser parser) {
        String cleaned = clean(text);
        if (cleaned.isBlank()) {
            return new ParsedDocument(parser, List.of());
        }
        return new ParsedDocument(parser, List.of(new ParsedSection(
                null,
                null,
                List.of(),
                cleaned,
                parser == DocumentParser.PDF ? 1 : null
        )));
    }

    private void addSection(
            List<ParsedSection> sections,
            String title,
            Integer headingLevel,
            List<String> headingPath,
            String content,
            DocumentParser parser
    ) {
        String cleaned = clean(content);
        if (!cleaned.isBlank()) {
            sections.add(new ParsedSection(
                    title,
                    headingLevel,
                    headingPath == null ? List.of() : List.copyOf(headingPath),
                    cleaned,
                    parser == DocumentParser.PDF ? 1 : null
            ));
        }
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

    private DocumentParser parserFor(KbDocument document) {
        String name = lower(document == null ? null : document.getName());
        String contentType = lower(document == null ? null : document.getContentType());
        if (contentType.contains("markdown") || name.endsWith(".md") || name.endsWith(".markdown")) {
            return DocumentParser.MARKDOWN;
        }
        if (contentType.contains("pdf") || name.endsWith(".pdf")) {
            return DocumentParser.PDF;
        }
        if (contentType.contains("wordprocessingml") || name.endsWith(".docx")) {
            return DocumentParser.DOCX;
        }
        return DocumentParser.TXT;
    }

    private final class MarkdownDocumentFormatParser implements DocumentFormatParser {

        @Override
        public DocumentParser format() {
            return DocumentParser.MARKDOWN;
        }

        @Override
        public ParsedDocument parse(ParseInput input) {
            return parseMarkdown(decodeUtf8Text(input.bytes()), format());
        }
    }

    private final class TextDocumentFormatParser implements DocumentFormatParser {

        @Override
        public DocumentParser format() {
            return DocumentParser.TXT;
        }

        @Override
        public ParsedDocument parse(ParseInput input) {
            return parseText(decodeUtf8Text(input.bytes()), format());
        }
    }

    private final class PdfLightweightDocumentFormatParser implements DocumentFormatParser {

        @Override
        public DocumentParser format() {
            return DocumentParser.PDF;
        }

        @Override
        public ParsedDocument parse(ParseInput input) {
            return parsePdf(input, format());
        }
    }

    private final class DocxLightweightDocumentFormatParser implements DocumentFormatParser {

        @Override
        public DocumentParser format() {
            return DocumentParser.DOCX;
        }

        @Override
        public ParsedDocument parse(ParseInput input) throws IOException {
            return parseDocx(input.bytes(), format());
        }
    }

    private ParsedDocument parseDocx(byte[] bytes, DocumentParser parser) throws IOException {
        String xml = extractDocxXml(bytes);
        if (xml.isBlank()) {
            return new ParsedDocument(parser, List.of());
        }
        List<ParsedSection> sections = new ArrayList<>();
        String[] headingStack = new String[6];
        String currentTitle = null;
        Integer currentHeadingLevel = null;
        List<String> currentHeadingPath = List.of();
        int pageNum = 1;
        Matcher bodyMatcher = DOCX_BODY_ELEMENT.matcher(xml);
        while (bodyMatcher.find()) {
            String tableXml = bodyMatcher.group(2);
            if (tableXml != null) {
                DocxTableContent tableContent = extractDocxTableContent(tableXml, pageNum);
                pageNum = tableContent.nextPageNum();
                if (!tableContent.content().isBlank()) {
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
            String paragraphXml = bodyMatcher.group(1);
            if (isDocxTocParagraph(paragraphXml)) {
                continue;
            }
            DocxParagraphContent paragraph = extractDocxParagraphContent(paragraphXml, pageNum);
            pageNum = paragraph.nextPageNum();
            Matcher headingMatcher = DOCX_HEADING_STYLE.matcher(paragraphXml);
            if (headingMatcher.find()) {
                String text = joinDocxSegments(paragraph.segments());
                if (text.isBlank()) {
                    continue;
                }
                currentHeadingLevel = Integer.parseInt(headingMatcher.group(1));
                currentTitle = clean(text);
                headingStack[currentHeadingLevel - 1] = currentTitle;
                for (int index = currentHeadingLevel; index < headingStack.length; index++) {
                    headingStack[index] = null;
                }
                currentHeadingPath = headingPath(headingStack);
                continue;
            }
            for (DocxTextSegment segment : paragraph.segments()) {
                sections.add(new ParsedSection(
                        currentTitle,
                        currentHeadingLevel,
                        currentHeadingPath,
                        segment.content(),
                        segment.pageNum()
                ));
            }
        }
        return new ParsedDocument(parser, sections);
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

    private String extractDocxXml(byte[] bytes) throws IOException {
        if (bytes.length == 0) {
            return "";
        }
        if (!looksLikeZip(bytes)) {
            throw parseFailed(new IOException("DOCX_NOT_ZIP"));
        }
        ZipDirectory directory = readZipDirectory(bytes);
        if (directory.documentEntry() == null) {
            return "";
        }
        byte[] xmlBytes = readZipEntryBody(bytes, directory.documentEntry());
        return new String(xmlBytes, StandardCharsets.UTF_8);
    }

    private String extractDocxParagraphText(String xml) {
        Matcher matcher = DOCX_TEXT.matcher(xml);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            builder.append(matcher.group(1)).append(' ');
        }
        return clean(unescapeXml(builder.toString()));
    }

    private boolean isDocxTocParagraph(String paragraphXml) {
        if (paragraphXml == null || paragraphXml.isBlank()) {
            return false;
        }
        return DOCX_TOC_STYLE.matcher(paragraphXml).find()
                || DOCX_TOC_FIELD.matcher(paragraphXml).find();
    }

    private DocxParagraphContent extractDocxParagraphContent(String xml, int initialPageNum) {
        List<DocxTextSegment> segments = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int pageNum = initialPageNum;
        Matcher matcher = DOCX_TEXT_EVENT.matcher(xml);
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
                addDocxTextSegment(segments, builder, pageNum);
                pageNum++;
                continue;
            }
            if (token.startsWith("<w:br") && DOCX_PAGE_BREAK_ATTRIBUTE.matcher(matcher.group(2)).find()) {
                addDocxTextSegment(segments, builder, pageNum);
                pageNum++;
                continue;
            }
            builder.append(' ');
        }
        addDocxTextSegment(segments, builder, pageNum);
        return new DocxParagraphContent(List.copyOf(segments), pageNum);
    }

    private DocxTableContent extractDocxTableContent(String tableXml, int initialPageNum) {
        List<String> rows = new ArrayList<>();
        int pageNum = initialPageNum;
        Matcher rowMatcher = DOCX_TABLE_ROW.matcher(tableXml);
        while (rowMatcher.find()) {
            List<String> cells = new ArrayList<>();
            Matcher cellMatcher = DOCX_TABLE_CELL.matcher(rowMatcher.group(1));
            while (cellMatcher.find()) {
                StringBuilder cellBuilder = new StringBuilder();
                Matcher paragraphMatcher = DOCX_CELL_PARAGRAPH.matcher(cellMatcher.group(1));
                while (paragraphMatcher.find()) {
                    String paragraphXml = paragraphMatcher.group(1);
                    if (isDocxTocParagraph(paragraphXml)) {
                        continue;
                    }
                    DocxParagraphContent paragraph = extractDocxParagraphContent(paragraphXml, pageNum);
                    pageNum = paragraph.nextPageNum();
                    ParserTextSanitizer.appendCleaned(cellBuilder, joinDocxSegments(paragraph.segments()));
                }
                String cellText = clean(cellBuilder.toString());
                if (!cellText.isBlank()) {
                    cells.add(cellText);
                }
            }
            if (!cells.isEmpty()) {
                rows.add(String.join(" | ", cells));
            }
        }
        return new DocxTableContent(clean(String.join("; ", rows)), initialPageNum, pageNum);
    }

    private void addDocxTextSegment(List<DocxTextSegment> segments, StringBuilder builder, int pageNum) {
        String content = clean(unescapeXml(builder.toString()));
        if (!content.isBlank()) {
            segments.add(new DocxTextSegment(content, pageNum));
        }
        builder.setLength(0);
    }

    private String joinDocxSegments(List<DocxTextSegment> segments) {
        StringBuilder builder = new StringBuilder();
        for (DocxTextSegment segment : segments) {
            builder.append(segment.content()).append(' ');
        }
        return clean(builder.toString());
    }

    private ParsedDocument parsePdf(ParseInput input, DocumentParser parser) {
        String raw = new String(input.bytes(), StandardCharsets.ISO_8859_1);
        Matcher pageMatcher = PDF_PAGE_OBJECT.matcher(raw);
        List<Integer> pageStarts = new ArrayList<>();
        while (pageMatcher.find()) {
            pageStarts.add(pageMatcher.start());
        }
        if (pageStarts.isEmpty()) {
            String content = extractPdfText(raw);
            return content.isBlank() ? parseOcrFallback(input, parser) : parseText(content, parser);
        }
        List<ParsedSection> sections = new ArrayList<>();
        for (int index = 0; index < pageStarts.size(); index++) {
            int start = pageStarts.get(index);
            int end = index + 1 < pageStarts.size() ? pageStarts.get(index + 1) : raw.length();
            String content = extractPdfText(raw.substring(start, end));
            if (!content.isBlank()) {
                sections.add(new ParsedSection(
                        null,
                        null,
                        List.of(),
                        content,
                        index + 1
                ));
            }
        }
        if (sections.isEmpty()) {
            return parseOcrFallback(input, parser);
        }
        return new ParsedDocument(parser, sections);
    }

    private ParsedDocument parseOcrFallback(ParseInput input, DocumentParser parser) {
        OcrFallbackResult result = ocrFallbackService.extractText(input);
        if (result.status() == OcrFallbackResult.Status.SUCCEEDED && !result.text().isBlank()) {
            String content = clean(result.text());
            if (!content.isBlank()) {
                return new ParsedDocument(parser, List.of(new ParsedSection(
                        null,
                        null,
                        List.of(),
                        content,
                        1,
                        "OCR_FALLBACK",
                        null,
                        null,
                        result.confidence(),
                        "OCR_TEXT"
                )));
            }
        }
        return new ParsedDocument(parser, List.of());
    }

    private String extractPdfText(byte[] bytes) {
        return extractPdfText(new String(bytes, StandardCharsets.ISO_8859_1));
    }

    private String extractPdfText(String raw) {
        Matcher matcher = PDF_TEXT_OBJECT.matcher(raw);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            builder.append(unescapePdfString(matcher.group(1))).append(' ');
        }
        Matcher arrayMatcher = PDF_TEXT_ARRAY.matcher(raw);
        while (arrayMatcher.find()) {
            Matcher stringMatcher = PDF_STRING.matcher(arrayMatcher.group(1));
            while (stringMatcher.find()) {
                builder.append(unescapePdfString(stringMatcher.group(1)));
            }
            builder.append(' ');
        }
        return clean(builder.toString());
    }

    private String decodeUtf8Text(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            String text = decoder.decode(ByteBuffer.wrap(bytes)).toString();
            if (looksLikeBinaryText(text)) {
                throw parseFailed(new IllegalArgumentException("BINARY_TEXT_REJECTED"));
            }
            return text;
        } catch (CharacterCodingException exception) {
            throw parseFailed(exception);
        }
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\uFEFF", "")
                .replace('\u0000', ' ')
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String unescapeXml(String value) {
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private String unescapePdfString(String value) {
        return value
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\\\", "\\");
    }

    private boolean looksLikeBinaryText(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int suspiciousControls = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\n' || current == '\r' || current == '\t') {
                continue;
            }
            if (current == '\u0000') {
                return true;
            }
            if (Character.isISOControl(current)) {
                suspiciousControls++;
            }
        }
        return suspiciousControls >= 4 || suspiciousControls * 100 / value.length() > 5;
    }

    private boolean looksLikeZip(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private ZipDirectory readZipDirectory(byte[] bytes) throws IOException {
        int eocdOffset = findEndOfCentralDirectory(bytes);
        if (eocdOffset < 0 || eocdOffset + 22 > bytes.length) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        int eocdCommentLength = readUnsignedShort(bytes, eocdOffset + 20);
        if (eocdOffset + 22 + eocdCommentLength != bytes.length) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        int diskNumber = readUnsignedShort(bytes, eocdOffset + 4);
        int centralDirectoryDisk = readUnsignedShort(bytes, eocdOffset + 6);
        int entriesOnDisk = readUnsignedShort(bytes, eocdOffset + 8);
        int totalEntries = readUnsignedShort(bytes, eocdOffset + 10);
        if (diskNumber != 0 || centralDirectoryDisk != 0 || entriesOnDisk != totalEntries) {
            throw parseFailed(new IOException("DOCX_UNSUPPORTED_ZIP"));
        }
        if (totalEntries > MAX_DOCX_ENTRIES) {
            throw parseFailed(new IOException("DOCX_ENTRY_LIMIT_EXCEEDED"));
        }
        long centralDirectorySize = readUnsignedInt(bytes, eocdOffset + 12);
        long centralDirectoryOffset = readUnsignedInt(bytes, eocdOffset + 16);
        if (centralDirectorySize == 0xFFFF_FFFFL || centralDirectoryOffset == 0xFFFF_FFFFL) {
            throw parseFailed(new IOException("DOCX_ZIP64_UNSUPPORTED"));
        }
        if (centralDirectoryOffset > eocdOffset || centralDirectorySize > eocdOffset - centralDirectoryOffset) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }

        int offset = (int) centralDirectoryOffset;
        int end = offset + (int) centralDirectorySize;
        ZipEntryMetadata documentEntry = null;
        for (int index = 0; index < totalEntries; index++) {
            if (offset + 46 > end || readInt(bytes, offset) != ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
                throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
            }
            int flags = readUnsignedShort(bytes, offset + 8);
            int method = readUnsignedShort(bytes, offset + 10);
            long compressedSize = readUnsignedInt(bytes, offset + 20);
            long uncompressedSize = readUnsignedInt(bytes, offset + 24);
            int nameLength = readUnsignedShort(bytes, offset + 28);
            int extraLength = readUnsignedShort(bytes, offset + 30);
            int commentLength = readUnsignedShort(bytes, offset + 32);
            int diskStart = readUnsignedShort(bytes, offset + 34);
            long localHeaderOffset = readUnsignedInt(bytes, offset + 42);
            if (compressedSize == 0xFFFF_FFFFL
                    || uncompressedSize == 0xFFFF_FFFFL
                    || localHeaderOffset == 0xFFFF_FFFFL
                    || diskStart != 0) {
                throw parseFailed(new IOException("DOCX_ZIP64_UNSUPPORTED"));
            }
            int nextOffset = offset + 46 + nameLength + extraLength + commentLength;
            if (nextOffset < offset || nextOffset > end) {
                throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
            }
            String name = new String(bytes, offset + 46, nameLength, StandardCharsets.UTF_8);
            if ("word/document.xml".equals(name)) {
                if (documentEntry != null) {
                    throw parseFailed(new IOException("DOCX_DUPLICATE_DOCUMENT_XML"));
                }
                documentEntry = new ZipEntryMetadata(
                        name,
                        flags,
                        method,
                        compressedSize,
                        uncompressedSize,
                        (int) localHeaderOffset
                );
            }
            offset = nextOffset;
        }
        if (offset != end) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        return new ZipDirectory(totalEntries, documentEntry);
    }

    private byte[] readZipEntryBody(byte[] bytes, ZipEntryMetadata entry) throws IOException {
        if ((entry.flags() & 1) != 0) {
            throw parseFailed(new IOException("DOCX_ENCRYPTED_ZIP"));
        }
        if (entry.uncompressedSize() > MAX_DOCX_XML_BYTES) {
            throw parseFailed(new IOException("DOCX_XML_LIMIT_EXCEEDED"));
        }
        int localHeaderOffset = entry.localHeaderOffset();
        if (localHeaderOffset < 0
                || localHeaderOffset + 30 > bytes.length
                || readInt(bytes, localHeaderOffset) != ZIP_LOCAL_FILE_HEADER_SIGNATURE) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        int method = readUnsignedShort(bytes, localHeaderOffset + 8);
        if (method != entry.method()) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        int nameLength = readUnsignedShort(bytes, localHeaderOffset + 26);
        int extraLength = readUnsignedShort(bytes, localHeaderOffset + 28);
        int dataStart = localHeaderOffset + 30 + nameLength + extraLength;
        if (dataStart < localHeaderOffset || entry.compressedSize() > bytes.length - dataStart) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        String localName = new String(bytes, localHeaderOffset + 30, nameLength, StandardCharsets.UTF_8);
        if (!entry.name().equals(localName)) {
            throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
        }
        int compressedSize = (int) entry.compressedSize();
        if (entry.method() == ZIP_METHOD_STORED) {
            if (entry.compressedSize() != entry.uncompressedSize()) {
                throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
            }
            byte[] xmlBytes = new byte[compressedSize];
            System.arraycopy(bytes, dataStart, xmlBytes, 0, compressedSize);
            return xmlBytes;
        }
        if (entry.method() != ZIP_METHOD_DEFLATED) {
            throw parseFailed(new IOException("DOCX_UNSUPPORTED_ZIP_METHOD"));
        }
        Inflater inflater = new Inflater(true);
        try (InputStream compressed = new ByteArrayInputStream(bytes, dataStart, compressedSize);
             InputStream inflated = new InflaterInputStream(compressed, inflater)) {
            byte[] xmlBytes = inflated.readNBytes(MAX_DOCX_XML_BYTES + 1);
            if (xmlBytes.length > MAX_DOCX_XML_BYTES) {
                throw parseFailed(new IOException("DOCX_XML_LIMIT_EXCEEDED"));
            }
            if (xmlBytes.length != entry.uncompressedSize()) {
                throw parseFailed(new IOException("DOCX_MALFORMED_ZIP"));
            }
            return xmlBytes;
        } finally {
            inflater.end();
        }
    }

    private int findEndOfCentralDirectory(byte[] bytes) {
        int start = Math.max(0, bytes.length - 65_557);
        for (int index = bytes.length - 22; index >= start; index--) {
            if (readInt(bytes, index) == ZIP_END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
                return index;
            }
        }
        return -1;
    }

    private int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private int readUnsignedShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private long readUnsignedInt(byte[] bytes, int offset) {
        return readInt(bytes, offset) & 0xFFFF_FFFFL;
    }

    private String stripBom(String value) {
        return value == null ? "" : value.replace("\uFEFF", "");
    }

    private DocumentParseException parseFailed(Throwable cause) {
        return new DocumentParseException("DOCUMENT_PARSE_FAILED", cause);
    }

    private record ZipDirectory(int entryCount, ZipEntryMetadata documentEntry) {
    }

    private record DocxParagraphContent(List<DocxTextSegment> segments, int nextPageNum) {
    }

    private record DocxTextSegment(String content, int pageNum) {
    }

    private record DocxTableContent(String content, int pageNum, int nextPageNum) {
    }

    private record ZipEntryMetadata(
            String name,
            int flags,
            int method,
            long compressedSize,
            long uncompressedSize,
            int localHeaderOffset
    ) {
    }
}
