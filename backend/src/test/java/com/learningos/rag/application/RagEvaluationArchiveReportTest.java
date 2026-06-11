package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagEvaluationArchiveReportTest {

    private static final String REPORT_FILE_NAME = "rag-quality-evaluation-report.md";

    private final RagEvaluationService service = new RagEvaluationService();

    @Test
    void writesArchivableReportFileForRepeatableRagBenchmark() throws Exception {
        RagEvaluationResult result = service.evaluate(archiveBenchmarkRequest());
        Path archiveDir = archiveDirectory();
        Path reportPath = RagEvaluationArchiveReportCli.writeReport(archiveDir);

        String report = Files.readString(reportPath, StandardCharsets.UTF_8);
        assertThat(reportPath).exists().isRegularFile();
        assertThat(report)
                .contains("# RAG Quality Evaluation Archive")
                .contains("Benchmark ID: rag-sql-course")
                .contains("Sample Count: 2")
                .contains("Recall@K: 0.500000")
                .contains("Citation Accuracy: 0.666667")
                .contains("Groundedness: 0.571429")
                .contains("No-source Refusal Rate: 1.000000")
                .contains("rag-source-001")
                .contains("rag-nosource-001")
                .contains("RAG Quality Evaluation Report");

        assertThat(RagEvaluationArchiveReportCli.archiveReport(result))
                .isEqualTo(report);
    }

    @Test
    void scriptInvokesArchiveReportTestWithConfigurableArchiveDirectory() throws IOException {
        Path scriptPath = Path.of("..", "scripts", "run-rag-evaluation-archive.ps1");

        assertThat(scriptPath).exists().isRegularFile();
        String script = Files.readString(scriptPath, StandardCharsets.UTF_8);
        assertThat(script)
                .contains("RagEvaluationArchiveReportTest")
                .contains("RagEvaluationArchiveReportCli")
                .contains("rag.evaluation.archiveDir")
                .contains("mvn")
                .contains("javac")
                .contains(REPORT_FILE_NAME);
    }

    private Path archiveDirectory() {
        String configured = System.getProperty("rag.evaluation.archiveDir");
        if (configured == null || configured.isBlank()) {
            return Path.of("target", "rag-evaluation-archive", "latest");
        }
        return Path.of(configured);
    }

    private RagEvaluationRequest archiveBenchmarkRequest() {
        return new RagEvaluationRequest(
                List.of(),
                List.of(),
                null,
                new RagEvaluationRequest.Benchmark(
                        "rag-sql-course",
                        "course_sql",
                        "SQL Course RAG Benchmark",
                        "v1",
                        2,
                        List.of(
                                new RagEvaluationRequest.BenchmarkSample(
                                        "rag-source-001",
                                        "Why can JOIN duplicate rows?",
                                        List.of("chunk_join", "chunk_index"),
                                        "JOIN combines every matching row pair.",
                                        "Do not cite material outside the SQL course.",
                                        false,
                                        "JOIN combines every matching row pair.",
                                        List.of(
                                                citation("chunk_join", "sql.md", "JOIN", 0.96),
                                                citation("chunk_noise", "noise.md", "Noise", 0.31),
                                                citation("chunk_index", "index.md", "Index", 0.88)
                                        ),
                                        2
                                ),
                                new RagEvaluationRequest.BenchmarkSample(
                                        "rag-nosource-001",
                                        "What does the course say about imaginary indexes?",
                                        List.of(),
                                        "",
                                        "Must refuse when no cited course material exists.",
                                        true,
                                        "NO_SOURCE: No cited course material was found.",
                                        List.of(),
                                        2
                                )
                        )
                )
        );
    }

    private SourceCitation citation(String documentId, String documentName, String sectionTitle, double score) {
        return new SourceCitation(
                documentId,
                documentName,
                1,
                sectionTitle,
                "archive evaluation excerpt",
                score
        );
    }

}
