package com.learningos.rag.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PdfBoxDocumentFormatParser implements DocumentFormatParser {

    private static final double TABLE_LINE_MIN = 2;
    private static final double TABLE_TAB_RATIO = 0.35;

    private final OcrFallbackService ocrFallbackService;

    public PdfBoxDocumentFormatParser(OcrFallbackService ocrFallbackService) {
        this.ocrFallbackService = ocrFallbackService == null ? new NoopOcrFallbackService() : ocrFallbackService;
    }

    @Override
    public DocumentParser format() {
        return DocumentParser.PDF;
    }

    @Override
    public ParsedDocument parse(ParseInput input) throws IOException {
        ParserResourceLimits.requireInputWithinLimit(input);
        List<ParsedSection> sections = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(input.bytes())) {
            int pages = document.getNumberOfPages();
            if (pages > ParserResourceLimits.MAX_PDF_PAGES) {
                throw new IllegalArgumentException("PDF_PAGE_LIMIT_EXCEEDED");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder extracted = new StringBuilder();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String content = ParserTextSanitizer.clean(stripper.getText(document));
                if (content.isBlank()) {
                    continue;
                }
                if (extracted.length() + content.length() > ParserResourceLimits.MAX_EXTRACTED_CHARS) {
                    throw new IllegalArgumentException("PARSER_OUTPUT_LIMIT_EXCEEDED");
                }
                extracted.append(content);
                sections.add(buildSection(content, page));
            }
        }
        if (sections.isEmpty()) {
            return parseOcrFallback(input);
        }
        return new ParsedDocument(format(), List.copyOf(sections));
    }

    private ParsedSection buildSection(String content, int page) {
        String contentKind = detectContentKind(content);
        Double layoutConfidence = estimateLayoutConfidence(content, contentKind);
        return new ParsedSection(
                null,
                null,
                List.of(),
                content,
                page,
                "RENDERED_PAGE",
                null,
                layoutConfidence,
                null,
                contentKind
        );
    }

    private String detectContentKind(String content) {
        String[] lines = content.split("\\R");
        if (lines.length < TABLE_LINE_MIN) {
            return "TEXT";
        }
        long tabLines = 0;
        long pipeLines = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (line.contains("\t")) {
                tabLines++;
            }
            if (line.contains("|") && line.chars().filter(ch -> ch == '|').count() >= 2) {
                pipeLines++;
            }
        }
        long nonBlank = java.util.Arrays.stream(lines).filter(line -> line != null && !line.isBlank()).count();
        if (nonBlank == 0) {
            return "TEXT";
        }
        double tabRatio = (double) tabLines / nonBlank;
        double pipeRatio = (double) pipeLines / nonBlank;
        if (tabRatio >= TABLE_TAB_RATIO || pipeRatio >= TABLE_TAB_RATIO) {
            return "TABLE";
        }
        if (content.toLowerCase(Locale.ROOT).contains("table of contents")
                || content.toLowerCase(Locale.ROOT).contains("contents")) {
            return "TOC";
        }
        return "TEXT";
    }

    private Double estimateLayoutConfidence(String content, String contentKind) {
        if (content == null || content.isBlank()) {
            return null;
        }
        double score = switch (contentKind) {
            case "TABLE" -> 0.82;
            case "TOC" -> 0.75;
            default -> 0.9;
        };
        if (content.length() < 40) {
            score -= 0.15;
        }
        return Math.max(0.5, Math.min(0.98, score));
    }

    private ParsedDocument parseOcrFallback(ParseInput input) {
        OcrFallbackResult result = ocrFallbackService.extractText(input);
        if (result.status() == OcrFallbackResult.Status.SUCCEEDED && !result.text().isBlank()) {
            String content = ParserTextSanitizer.clean(result.text());
            if (!content.isBlank()) {
                if (content.length() > ParserResourceLimits.MAX_EXTRACTED_CHARS) {
                    throw new IllegalArgumentException("PARSER_OUTPUT_LIMIT_EXCEEDED");
                }
                return new ParsedDocument(format(), List.of(new ParsedSection(
                        null,
                        null,
                        List.of(),
                        content,
                        1,
                        "OCR_FALLBACK",
                        null,
                        0.55,
                        result.confidence(),
                        "OCR_TEXT"
                )));
            }
        }
        return new ParsedDocument(format(), List.of());
    }
}
