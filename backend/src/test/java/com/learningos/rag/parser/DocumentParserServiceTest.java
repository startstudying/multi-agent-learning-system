package com.learningos.rag.parser;

import com.learningos.rag.domain.KbDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParserServiceTest {

    private final DocumentParserService parserService = new DocumentParserService();

    @Test
    void parsesMarkdownIntoHeadingAwareSections() {
        KbDocument document = document("lesson.md", "text/markdown");
        byte[] bytes = """
                # SQL Basics

                Root introduction.

                ## Join Strategy

                Join cardinality matters.

                ### Index Use

                Indexes help lookup speed.
                """.getBytes(StandardCharsets.UTF_8);

        ParsedDocument parsed = parserService.parse(document, bytes);

        assertThat(parsed.parser()).isEqualTo(DocumentParser.MARKDOWN);
        assertThat(parsed.sections()).hasSize(3);
        assertThat(parsed.sections().get(0))
                .extracting(
                        ParsedSection::title,
                        ParsedSection::headingLevel,
                        ParsedSection::headingPath,
                        ParsedSection::content,
                        ParsedSection::pageNum
                )
                .containsExactly(
                        "SQL Basics",
                        1,
                        List.of("SQL Basics"),
                        "Root introduction.",
                        null
                );
        assertThat(parsed.sections().get(1).headingPath())
                .containsExactly("SQL Basics", "Join Strategy");
        assertThat(parsed.sections().get(2).headingPath())
                .containsExactly("SQL Basics", "Join Strategy", "Index Use");
    }

    @Test
    void parsesTxtAsSingleSectionAndSkipsBlankContent() {
        KbDocument txt = document("outline.txt", "text/plain");

        ParsedDocument parsed = parserService.parse(txt, "  Retrieval   notes.\n\nCitations.  ".getBytes(StandardCharsets.UTF_8));
        ParsedDocument blank = parserService.parse(txt, " \n\t ".getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.parser()).isEqualTo(DocumentParser.TXT);
        assertThat(parsed.sections()).singleElement()
                .satisfies(section -> {
                    assertThat(section.title()).isNull();
                    assertThat(section.headingLevel()).isNull();
                    assertThat(section.headingPath()).isEmpty();
                    assertThat(section.content()).isEqualTo("Retrieval notes. Citations.");
                    assertThat(section.pageNum()).isNull();
        });
        assertThat(blank.sections()).isEmpty();
    }

    @Test
    void parsedDocumentAddsStableReadingOrderAndSafeDefaultSectionMetadata() {
        ParsedDocument parsed = new ParsedDocument(DocumentParser.MARKDOWN, List.of(
                new ParsedSection("Intro", 1, List.of("Intro"), "First section", null),
                new ParsedSection("Details", 2, List.of("Intro", "Details"), "Second section", 3)
        ));

        assertThat(parsed.sections())
                .extracting(ParsedSection::readingOrderIndex)
                .containsExactly(1, 2);
        assertThat(parsed.sections().get(0))
                .extracting(
                        ParsedSection::pageNumSource,
                        ParsedSection::contentKind,
                        ParsedSection::layoutConfidence,
                        ParsedSection::ocrConfidence
                )
                .containsExactly("NONE", "TEXT", null, null);
        assertThat(parsed.sections().get(1).pageNumSource()).isEqualTo("PARSER_INFERRED");
    }

    @Test
    void selectsPdfAndDocxParsersWithCurrentLightweightExtraction() throws Exception {
        KbDocument pdf = document("retrieval.pdf", "application/pdf");
        KbDocument docx = document("rubric.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        ParsedDocument parsedPdf = parserService.parse(
                pdf,
                "BT (Hybrid retrieval combines keyword search and vectors.) Tj ET".getBytes(StandardCharsets.ISO_8859_1)
        );
        ParsedDocument parsedDocx = parserService.parse(
                docx,
                docxBytes("Rubric grading should compare criteria and wrong causes.")
        );

        assertThat(parsedPdf.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsedPdf.sections()).singleElement()
                .satisfies(section -> {
                    assertThat(section.content()).contains("Hybrid retrieval combines");
                    assertThat(section.pageNum()).isEqualTo(1);
                });
        assertThat(parsedDocx.parser()).isEqualTo(DocumentParser.DOCX);
        assertThat(parsedDocx.sections()).singleElement()
                .satisfies(section -> {
                    assertThat(section.content()).contains("Rubric grading");
                    assertThat(section.pageNum()).isEqualTo(1);
                });
    }

    @Test
    void pdfWithoutTextObjectsDoesNotFallbackToRawBinaryContent() {
        KbDocument pdf = document("scan.pdf", "application/pdf");
        byte[] bytes = """
                %PDF-1.7
                1 0 obj <</Length 42>> stream
                x\u0000\u0001binary object stream should not become searchable text
                endstream endobj
                %%EOF
                """.getBytes(StandardCharsets.ISO_8859_1);

        ParsedDocument parsed = parserService.parse(pdf, bytes);

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).isEmpty();
    }

    @Test
    void pdfExtractsSimpleTextArrayWithoutIndexingOperators() {
        KbDocument pdf = document("array.pdf", "application/pdf");

        ParsedDocument parsed = parserService.parse(
                pdf,
                "BT [(Hybrid retrieval ) 120 (uses RRF.)] TJ (Escaped \\(value\\) and \\\\ path.) Tj ET"
                        .getBytes(StandardCharsets.ISO_8859_1)
        );

        assertThat(parsed.sections()).singleElement()
                .satisfies(section -> {
                    assertThat(section.content()).contains("Hybrid retrieval uses RRF.");
                    assertThat(section.content()).contains("Escaped (value) and \\ path.");
                    assertThat(section.content()).doesNotContain("BT");
                    assertThat(section.pageNum()).isEqualTo(1);
                });
    }

    @Test
    void pdfExtractsSimplePageBoundariesBestEffort() {
        KbDocument pdf = document("multi-page.pdf", "application/pdf");

        ParsedDocument parsed = parserService.parse(
                pdf,
                """
                        1 0 obj <</Type /Page>> endobj
                        BT (First page retrieval note.) Tj ET
                        2 0 obj <</Type /Page>> endobj
                        BT [(Second page ) (citation detail.)] TJ ET
                        """.getBytes(StandardCharsets.ISO_8859_1)
        );

        assertThat(parsed.sections()).hasSize(2);
        assertThat(parsed.sections().get(0))
                .extracting(ParsedSection::content, ParsedSection::pageNum)
                .containsExactly("First page retrieval note.", 1);
        assertThat(parsed.sections().get(1))
                .extracting(ParsedSection::content, ParsedSection::pageNum)
                .containsExactly("Second page citation detail.", 2);
    }

    @Test
    void docxParsesHeadingParagraphsAndPageBreaksBestEffort() throws Exception {
        KbDocument docx = document(
                "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(docx, docxXmlBytes("""
                <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Course RAG</w:t></w:r></w:p>
                <w:p><w:r><w:t>Course overview.</w:t></w:r></w:p>
                <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Citations</w:t></w:r></w:p>
                <w:p><w:r><w:lastRenderedPageBreak/><w:t>Second page citation detail.</w:t></w:r></w:p>
                """));

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
                        "Course overview.",
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
    void docxCountsMultiplePageBreaksInSingleParagraph() throws Exception {
        KbDocument docx = document(
                "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(docx, docxXmlBytes("""
                <w:p><w:r><w:br w:type="page"/><w:lastRenderedPageBreak/><w:t>Third page detail.</w:t></w:r></w:p>
                """));

        assertThat(parsed.sections()).singleElement()
                .satisfies(section -> assertThat(section.pageNum()).isEqualTo(3));
    }

    @Test
    void docxSplitsTextAroundPageBreakInsideParagraph() throws Exception {
        KbDocument docx = document(
                "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(docx, docxXmlBytes("""
                <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Course RAG</w:t></w:r></w:p>
                <w:p><w:r><w:t>Page one context.</w:t><w:br w:type="page"/><w:t>Page two context.</w:t></w:r></w:p>
                """));

        assertThat(parsed.sections()).hasSize(2);
        assertThat(parsed.sections().get(0))
                .extracting(ParsedSection::content, ParsedSection::pageNum, ParsedSection::headingPath)
                .containsExactly("Page one context.", 1, List.of("Course RAG"));
        assertThat(parsed.sections().get(1))
                .extracting(ParsedSection::content, ParsedSection::pageNum, ParsedSection::headingPath)
                .containsExactly("Page two context.", 2, List.of("Course RAG"));
    }

    @Test
    void docxParsesTablesInBodyOrderAndSkipsTocParagraphsBestEffort() throws Exception {
        KbDocument docx = document(
                "table.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(docx, docxXmlBytes("""
                <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Course RAG</w:t></w:r></w:p>
                <w:p><w:pPr><w:pStyle w:val="TOC1"/></w:pPr><w:r><w:t>Course RAG .... 1</w:t></w:r></w:p>
                <w:p><w:r><w:t>Intro paragraph.</w:t></w:r></w:p>
                <w:tbl>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Concept</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Definition</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>RAG</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Retrieval augmented generation</w:t></w:r></w:p></w:tc>
                  </w:tr>
                </w:tbl>
                <w:p><w:r><w:t>After table explanation.</w:t></w:r></w:p>
                """));

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
        assertThat(parsed.sections()).allSatisfy(section -> assertThat(section.headingPath())
                .containsExactly("Course RAG"));
    }

    @Test
    void docxPreservesTabAndLineBreakSeparators() throws Exception {
        KbDocument docx = document(
                "layout.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(docx, docxXmlBytes("""
                <w:p><w:r><w:t>Alpha</w:t><w:tab/><w:t>Beta</w:t><w:br/><w:t>Gamma</w:t></w:r></w:p>
                """));

        assertThat(parsed.sections()).singleElement()
                .satisfies(section -> assertThat(section.content()).isEqualTo("Alpha Beta Gamma"));
    }

    @Test
    void docxDoesNotInflateNonDocumentEntryBodiesAfterDocumentXml() throws Exception {
        KbDocument docx = document(
                "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(docx, docxWithCorruptNonDocumentEntryAfterDocumentXml());

        assertThat(parsed.sections()).singleElement()
                .satisfies(section -> assertThat(section.content()).isEqualTo("Document text survives."));
    }

    @Test
    void docxOversizedDocumentXmlFailsWithSafeParserCode() throws Exception {
        KbDocument docx = document(
                "huge.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        assertThatThrownBy(() -> parserService.parse(docx, oversizedDocxBytes()))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void docxMalformedZipAndTooManyEntriesFailWithSafeParserCode() throws Exception {
        KbDocument docx = document(
                "broken.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        assertThatThrownBy(() -> parserService.parse(docx, "not a zip archive".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");

        assertThatThrownBy(() -> parserService.parse(docx, "PK".getBytes(StandardCharsets.ISO_8859_1)))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");

        assertThatThrownBy(() -> parserService.parse(docx, docxWithTooManyEntries()))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");

        assertThatThrownBy(() -> parserService.parse(docx, docxWithTooManyEntriesAfterDocumentXml()))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");

        assertThatThrownBy(() -> parserService.parse(docx, docxWithCentralDirectoryNameMismatch()))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void docxTextOutsideParagraphsDoesNotFallbackToSyntheticSection() throws Exception {
        KbDocument docx = document(
                "no-paragraph.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        ParsedDocument parsed = parserService.parse(
                docx,
                docxXmlBytes("<w:r><w:t>Text outside a paragraph should not be indexed.</w:t></w:r>")
        );

        assertThat(parsed.sections()).isEmpty();
    }

    @Test
    void markdownBomStillRecognizesFirstHeadingAndBinaryTextIsRejected() {
        KbDocument markdown = document("lesson.md", "text/markdown");
        ParsedDocument parsed = parserService.parse(
                markdown,
                "\uFEFF# First\n\nContent.".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(parsed.sections()).singleElement()
                .satisfies(section -> {
                    assertThat(section.title()).isEqualTo("First");
                    assertThat(section.headingPath()).containsExactly("First");
                });

        KbDocument txt = document("binary.txt", "text/plain");
        assertThatThrownBy(() -> parserService.parse(txt, new byte[]{0, 1, 2, 3, 4, 5, 6, 7}))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");

        assertThatThrownBy(() -> parserService.parse(markdown, new byte[]{0, 1, 2, 3, 4, 5, 6, 7}))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void serviceDelegatesPdfToRegisteredProvider() {
        CapturingParser pdfProvider = new CapturingParser(DocumentParser.PDF);
        DocumentParserService service = new DocumentParserService(List.of(
                new CapturingParser(DocumentParser.MARKDOWN),
                new CapturingParser(DocumentParser.TXT),
                pdfProvider,
                new CapturingParser(DocumentParser.DOCX)
        ));
        KbDocument pdf = document("delegated.pdf", "application/pdf");
        pdf.setId("doc_42");

        ParsedDocument parsed = service.parse(pdf, "provider text".getBytes(StandardCharsets.ISO_8859_1));

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(pdfProvider.called).isTrue();
        assertThat(pdfProvider.lastInput.documentId()).isEqualTo("doc_42");
        assertThat(pdfProvider.lastInput.name()).isEqualTo("delegated.pdf");
        assertThat(pdfProvider.lastInput.contentType()).isEqualTo("application/pdf");
        assertThat(pdfProvider.lastInput.sizeBytes()).isEqualTo("provider text".getBytes(StandardCharsets.ISO_8859_1).length);
    }

    @Test
    void providerFailureIsMappedToSafeParserCode() {
        DocumentParserService service = new DocumentParserService(List.of(new DocumentFormatParser() {
            @Override
            public DocumentParser format() {
                return DocumentParser.PDF;
            }

            @Override
            public ParsedDocument parse(ParseInput input) {
                throw new IllegalArgumentException("C:\\secret\\scan.pdf apiKey=sk-test raw learner text");
            }
        }));

        assertThatThrownBy(() -> service.parse(document("scan.pdf", "application/pdf"), new byte[]{1, 2, 3}))
                .isInstanceOf(DocumentParseException.class)
                .extracting("safeCode")
                .isEqualTo("DOCUMENT_PARSE_FAILED");
    }

    @Test
    void pdfReturnsEmptySectionsForImageOnlyPdfUntilOcrAdapterExists() {
        KbDocument pdf = document("image-only.pdf", "application/pdf");
        byte[] bytes = """
                %PDF-1.7
                1 0 obj <</Type /Page /Resources <</XObject <</Im1 2 0 R>>>>>> endobj
                2 0 obj <</Subtype /Image /Width 100 /Height 100>> stream
                raw image bytes should not become searchable text
                endstream endobj
                %%EOF
                """.getBytes(StandardCharsets.ISO_8859_1);

        ParsedDocument parsed = parserService.parse(pdf, bytes);

        assertThat(parsed.parser()).isEqualTo(DocumentParser.PDF);
        assertThat(parsed.sections()).isEmpty();
    }

    private KbDocument document(String name, String contentType) {
        KbDocument document = new KbDocument();
        document.setName(name);
        document.setContentType(contentType);
        return document;
    }

    private byte[] docxBytes(String text) throws Exception {
        return docxXmlBytes("<w:p><w:r><w:t>" + text + "</w:t></w:r></w:p>");
    }

    private byte[] docxXmlBytes(String body) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body>%s</w:body>
                    </w:document>
                    """.formatted(body)).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private byte[] oversizedDocxBytes() throws Exception {
        String oversizedText = "x".repeat(2 * 1024 * 1024 + 128);
        return docxXmlBytes("<w:p><w:r><w:t>" + oversizedText + "</w:t></w:r></w:p>");
    }

    private byte[] docxWithTooManyEntries() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int index = 0; index < 65; index++) {
                zip.putNextEntry(new ZipEntry("word/extra-" + index + ".xml"));
                zip.write("x".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document><w:body/></w:document>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private byte[] docxWithTooManyEntriesAfterDocumentXml() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document><w:body/></w:document>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (int index = 0; index < 65; index++) {
                zip.putNextEntry(new ZipEntry("word/extra-after-" + index + ".xml"));
                zip.write("x".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private byte[] docxWithCorruptNonDocumentEntryAfterDocumentXml() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("""
                    <w:document>
                      <w:body><w:p><w:r><w:t>Document text survives.</w:t></w:r></w:p></w:body>
                    </w:document>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/corrupt.bin"));
            zip.write("This entry body must not be inflated by the parser.".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        byte[] bytes = output.toByteArray();
        corruptLocalEntryData(bytes, "word/corrupt.bin");
        return bytes;
    }

    private byte[] docxWithCentralDirectoryNameMismatch() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/notdocum.xml"));
            zip.write("""
                    <w:document>
                      <w:body><w:p><w:r><w:t>Forged central directory text.</w:t></w:r></w:p></w:body>
                    </w:document>
                    """.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        byte[] bytes = output.toByteArray();
        renameCentralDirectoryEntry(bytes, "word/notdocum.xml", "word/document.xml");
        return bytes;
    }

    private void corruptLocalEntryData(byte[] bytes, String entryName) {
        int centralDirectoryEntry = findCentralDirectoryEntry(bytes, entryName);
        int compressedSize = readLittleEndianInt(bytes, centralDirectoryEntry + 20);
        int localHeaderOffset = readLittleEndianInt(bytes, centralDirectoryEntry + 42);
        int nameLength = readLittleEndianUnsignedShort(bytes, localHeaderOffset + 26);
        int extraLength = readLittleEndianUnsignedShort(bytes, localHeaderOffset + 28);
        int dataStart = localHeaderOffset + 30 + nameLength + extraLength;
        for (int index = 0; index < Math.min(4, compressedSize); index++) {
            bytes[dataStart + index] = (byte) 0xFF;
        }
    }

    private int findCentralDirectoryEntry(byte[] bytes, String entryName) {
        int eocd = findEndOfCentralDirectory(bytes);
        int entryCount = readLittleEndianUnsignedShort(bytes, eocd + 10);
        int offset = readLittleEndianInt(bytes, eocd + 16);
        for (int index = 0; index < entryCount; index++) {
            int nameLength = readLittleEndianUnsignedShort(bytes, offset + 28);
            int extraLength = readLittleEndianUnsignedShort(bytes, offset + 30);
            int commentLength = readLittleEndianUnsignedShort(bytes, offset + 32);
            String currentName = new String(bytes, offset + 46, nameLength, StandardCharsets.UTF_8);
            if (entryName.equals(currentName)) {
                return offset;
            }
            offset += 46 + nameLength + extraLength + commentLength;
        }
        throw new IllegalStateException("central directory entry not found: " + entryName);
    }

    private void renameCentralDirectoryEntry(byte[] bytes, String originalName, String replacementName) {
        byte[] original = originalName.getBytes(StandardCharsets.UTF_8);
        byte[] replacement = replacementName.getBytes(StandardCharsets.UTF_8);
        if (original.length != replacement.length) {
            throw new IllegalArgumentException("replacement must keep central directory length stable");
        }
        int centralDirectoryEntry = findCentralDirectoryEntry(bytes, originalName);
        System.arraycopy(replacement, 0, bytes, centralDirectoryEntry + 46, replacement.length);
    }

    private int findEndOfCentralDirectory(byte[] bytes) {
        for (int index = bytes.length - 22; index >= 0; index--) {
            if (readLittleEndianInt(bytes, index) == 0x06054b50) {
                return index;
            }
        }
        throw new IllegalStateException("EOCD not found");
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private int readLittleEndianUnsignedShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static class CapturingParser implements DocumentFormatParser {

        private final DocumentParser format;
        private boolean called;
        private ParseInput lastInput;

        private CapturingParser(DocumentParser format) {
            this.format = format;
        }

        @Override
        public DocumentParser format() {
            return format;
        }

        @Override
        public ParsedDocument parse(ParseInput input) {
            this.called = true;
            this.lastInput = input;
            return new ParsedDocument(format, List.of(new ParsedSection(null, null, List.of(), "provider text", 7)));
        }
    }
}
