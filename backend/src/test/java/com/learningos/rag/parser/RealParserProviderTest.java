package com.learningos.rag.parser;

import com.learningos.rag.domain.KbDocument;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealParserProviderTest {

    @Test
    void pdfBoxProviderExtractsTextPerPageWithoutIndexingRawOperators() throws Exception {
        PdfBoxDocumentFormatParser provider = new PdfBoxDocumentFormatParser(new NoopOcrFallbackService());
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(
                document("real.pdf", "application/pdf"),
                pdfBytes("First page retrieval note.", "Second page citation detail.")
        );

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).hasSize(2);
        assertThat(parsed.sections().get(0))
                .extracting(ParsedSection::content, ParsedSection::pageNum)
                .containsExactly("First page retrieval note.", 1);
        assertThat(parsed.sections().get(1))
                .extracting(ParsedSection::content, ParsedSection::pageNum)
                .containsExactly("Second page citation detail.", 2);
        assertThat(parsed.sections())
                .allSatisfy(section -> assertThat(section.content()).doesNotContain("BT", "Tj", "%PDF"));
    }

    @Test
    void pdfBoxProviderReturnsEmptySectionsForImageOnlyPdfWithNoopOcr() throws Exception {
        PdfBoxDocumentFormatParser provider = new PdfBoxDocumentFormatParser(new NoopOcrFallbackService());
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(document("blank.pdf", "application/pdf"), blankPdfBytes());

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).isEmpty();
    }

    @Test
    void pdfBoxProviderUsesConfiguredOcrFallbackTextForImageOnlyPdf() throws Exception {
        OcrFallbackService ocr = new ConfigurableOcrFallbackService(
                new com.learningos.config.RagParserOcrProperties(true, "fake"),
                List.of(ocrProvider("fake", input -> new OcrFallbackResult(
                        OcrFallbackResult.Status.SUCCEEDED,
                        "OCR_PROVIDER_SUCCEEDED",
                        "OCR recognized scan text.",
                        0.82
                )))
        );
        PdfBoxDocumentFormatParser provider = new PdfBoxDocumentFormatParser(ocr);
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(document("scan.pdf", "application/pdf"), blankPdfBytes());

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).singleElement()
                .extracting(
                        ParsedSection::content,
                        ParsedSection::pageNum,
                        ParsedSection::pageNumSource,
                        ParsedSection::contentKind,
                        ParsedSection::ocrConfidence
                )
                .containsExactly("OCR recognized scan text.", 1, "OCR_FALLBACK", "OCR_TEXT", 0.82);
    }

    @Test
    void pdfBoxProviderUsesProcessOcrProviderForImageOnlyPdf() throws Exception {
        OcrFallbackService ocr = new ConfigurableOcrFallbackService(
                new com.learningos.config.RagParserOcrProperties(true, "process",
                        new com.learningos.config.RagParserOcrProperties.ProcessProperties("", java.time.Duration.ofSeconds(1), 4096)),
                List.of(new ProcessOcrFallbackProvider(new com.learningos.config.RagParserOcrProperties.ProcessProperties(
                        "python -c \"import sys; sys.stdin.buffer.read(); print('Process OCR scan text.')\"",
                        java.time.Duration.ofSeconds(2),
                        4096
                )))
        );
        PdfBoxDocumentFormatParser provider = new PdfBoxDocumentFormatParser(ocr);
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(document("scan.pdf", "application/pdf"), blankPdfBytes());

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).singleElement()
                .extracting(ParsedSection::content, ParsedSection::pageNum)
                .containsExactly("Process OCR scan text.", 1);
    }

    @Test
    void pdfBoxProviderKeepsImageOnlyPdfEmptyWhenConfiguredOcrProviderFails() throws Exception {
        OcrFallbackService ocr = new ConfigurableOcrFallbackService(
                new com.learningos.config.RagParserOcrProperties(true, "fake"),
                List.of(ocrProvider("fake", input -> {
                    throw new IllegalStateException("C:\\secret\\scan.pdf apiKey=sk-live-secret raw learner text");
                }))
        );
        PdfBoxDocumentFormatParser provider = new PdfBoxDocumentFormatParser(ocr);
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(document("scan.pdf", "application/pdf"), blankPdfBytes());

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).isEmpty();
    }

    @Test
    void pdfBoxProviderFailureIsMappedToSafeParserCode() {
        PdfBoxDocumentFormatParser provider = new PdfBoxDocumentFormatParser(new NoopOcrFallbackService());
        DocumentParserService service = new DocumentParserService(List.of(provider));

        assertThatThrownBy(() -> service.parse(
                document("broken.pdf", "application/pdf"),
                "not a pdf C:\\secret\\file.pdf apiKey=sk-test".getBytes(StandardCharsets.UTF_8)
        ))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void poiDocxProviderExtractsHeadingHierarchyPageBreaksAndSeparators() throws Exception {
        PoiDocxDocumentFormatParser provider = new PoiDocxDocumentFormatParser();
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(
                document("real.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                docxBytes()
        );

        assertThat(parsed.parser()).isEqualTo(DocumentParser.DOCX);
        assertThat(parsed.sections()).hasSize(2);
        assertThat(parsed.sections().get(0))
                .extracting(
                        ParsedSection::title,
                        ParsedSection::headingLevel,
                        ParsedSection::headingPath,
                        ParsedSection::content,
                        ParsedSection::pageNum
                )
                .containsExactly(
                        "Course RAG",
                        1,
                        List.of("Course RAG"),
                        "Page one context with tab and line break.",
                        1
                );
        assertThat(parsed.sections().get(1))
                .extracting(
                        ParsedSection::title,
                        ParsedSection::headingLevel,
                        ParsedSection::headingPath,
                        ParsedSection::content,
                        ParsedSection::pageNum
                )
                .containsExactly(
                        "Citations",
                        2,
                        List.of("Course RAG", "Citations"),
                        "Second page citation detail.",
                        2
                );
    }

    @Test
    void poiDocxProviderPreservesBodyOrderForTablesAndSkipsTocParagraphs() throws Exception {
        PoiDocxDocumentFormatParser provider = new PoiDocxDocumentFormatParser();
        DocumentParserService service = new DocumentParserService(List.of(provider));

        ParsedDocument parsed = service.parse(
                document("table.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                docxWithTableAndTocBytes()
        );

        assertThat(parsed.parser()).isEqualTo(DocumentParser.DOCX);
        assertThat(parsed.sections()).hasSize(3);
        assertThat(parsed.sections()).extracting(ParsedSection::content)
                .containsExactly(
                        "Intro paragraph.",
                        "Concept | Definition; RAG | Retrieval augmented generation",
                        "After table explanation."
                );
        assertThat(parsed.sections()).extracting(ParsedSection::contentKind)
                .containsExactly("TEXT", "TABLE_TEXT", "TEXT");
        assertThat(parsed.sections()).extracting(ParsedSection::readingOrderIndex)
                .containsExactly(1, 2, 3);
        assertThat(parsed.sections()).allSatisfy(section -> {
            assertThat(section.headingPath()).containsExactly("Course RAG");
            assertThat(section.pageNumSource()).isEqualTo("PARSER_INFERRED");
            assertThat(section.layoutConfidence()).isNull();
            assertThat(section.ocrConfidence()).isNull();
            assertThat(section.content()).doesNotContain("TOC", "<w:tbl", "<w:t");
        });
    }

    @Test
    void poiDocxProviderFailureIsMappedToSafeParserCode() {
        PoiDocxDocumentFormatParser provider = new PoiDocxDocumentFormatParser();
        DocumentParserService service = new DocumentParserService(List.of(provider));

        assertThatThrownBy(() -> service.parse(
                document("broken.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                "not a zip C:\\secret\\file.docx apiKey=sk-test".getBytes(StandardCharsets.UTF_8)
        ))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void providersRejectOversizedInputsWithSafeParserCode() {
        DocumentParserService service = new DocumentParserService(List.of(
                new PdfBoxDocumentFormatParser(new NoopOcrFallbackService()),
                new PoiDocxDocumentFormatParser()
        ));
        byte[] oversized = new byte[ParserResourceLimits.MAX_INPUT_BYTES + 1];

        assertThatThrownBy(() -> service.parse(document("huge.pdf", "application/pdf"), oversized))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");

        assertThatThrownBy(() -> service.parse(
                document("huge.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                oversized
        ))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void pdfBoxProviderRejectsOversizedOcrFallbackTextWithSafeParserCode() {
        OcrFallbackService oversizedOcr = input -> new OcrFallbackResult(
                OcrFallbackResult.Status.SUCCEEDED,
                "TEST_OCR",
                "x".repeat(ParserResourceLimits.MAX_EXTRACTED_CHARS + 1)
        );
        DocumentParserService service = new DocumentParserService(List.of(new PdfBoxDocumentFormatParser(oversizedOcr)));

        assertThatThrownBy(() -> service.parse(document("blank.pdf", "application/pdf"), blankPdfBytes()))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    private KbDocument document(String name, String contentType) {
        KbDocument document = new KbDocument();
        document.setName(name);
        document.setContentType(contentType);
        return document;
    }

    private byte[] pdfBytes(String... pageTexts) throws Exception {
        StringBuilder objects = new StringBuilder();
        StringBuilder kids = new StringBuilder();
        int objectNumber = 3;
        for (String pageText : pageTexts) {
            int pageObject = objectNumber++;
            int contentObject = objectNumber++;
            kids.append(pageObject).append(" 0 R ");
            String stream = "BT /F1 12 Tf 72 720 Td (" + escapePdfText(pageText) + ") Tj ET";
            objects.append(pageObject)
                    .append(" 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] ")
                    .append("/Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> ")
                    .append("/Contents ")
                    .append(contentObject)
                    .append(" 0 R >> endobj\n");
            objects.append(contentObject)
                    .append(" 0 obj << /Length ")
                    .append(stream.getBytes(StandardCharsets.ISO_8859_1).length)
                    .append(" >> stream\n")
                    .append(stream)
                    .append("\nendstream endobj\n");
        }
        String pdf = """
                %%PDF-1.4
                1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
                2 0 obj << /Type /Pages /Kids [%s] /Count %d >> endobj
                %s
                trailer << /Root 1 0 R >>
                %%%%EOF
                """.formatted(kids, pageTexts.length, objects);
        return pdf.getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] blankPdfBytes() {
        return """
                %PDF-1.4
                1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
                2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
                3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >> endobj
                trailer << /Root 1 0 R >>
                %%EOF
                """.getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escapePdfText(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private OcrFallbackProvider ocrProvider(String provider, OcrFallbackService service) {
        return new OcrFallbackProvider() {
            @Override
            public String provider() {
                return provider;
            }

            @Override
            public OcrFallbackResult extractText(ParseInput input) {
                return service.extractText(input);
            }
        };
    }

    private byte[] docxBytes() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph heading1 = document.createParagraph();
            heading1.setStyle("Heading1");
            heading1.createRun().setText("Course RAG");

            XWPFParagraph pageOne = document.createParagraph();
            XWPFRun pageOneRun = pageOne.createRun();
            pageOneRun.setText("Page one context");
            pageOneRun.addTab();
            pageOneRun.setText("with tab");
            pageOneRun.addBreak();
            pageOneRun.setText("and line break.");

            XWPFParagraph heading2 = document.createParagraph();
            heading2.setStyle("Heading2");
            heading2.createRun().setText("Citations");

            XWPFParagraph pageTwo = document.createParagraph();
            XWPFRun pageTwoRun = pageTwo.createRun();
            pageTwoRun.addBreak(BreakType.PAGE);
            pageTwoRun.setText("Second page citation detail.");

            document.write(output);
            return output.toByteArray();
        }
    }

    private byte[] docxWithTableAndTocBytes() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("Course RAG");

            XWPFParagraph toc = document.createParagraph();
            toc.setStyle("TOC1");
            toc.createRun().setText("TOC Course RAG .... 1");

            XWPFParagraph intro = document.createParagraph();
            intro.createRun().setText("Intro paragraph.");

            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Concept");
            table.getRow(0).getCell(1).setText("Definition");
            table.getRow(1).getCell(0).setText("RAG");
            table.getRow(1).getCell(1).setText("Retrieval augmented generation");

            XWPFParagraph after = document.createParagraph();
            after.createRun().setText("After table explanation.");

            document.write(output);
            return output.toByteArray();
        }
    }
}
