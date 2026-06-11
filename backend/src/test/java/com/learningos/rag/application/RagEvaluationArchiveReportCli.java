package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class RagEvaluationArchiveReportCli {

    static final String REPORT_FILE_NAME = "rag-quality-evaluation-report.md";

    private RagEvaluationArchiveReportCli() {
    }

    public static void main(String[] args) throws IOException {
        Path archiveDir = args.length == 0 || args[0].isBlank()
                ? Path.of("target", "rag-evaluation-archive", "latest")
                : Path.of(args[0]);
        Path reportPath = writeReport(archiveDir);
        System.out.println(reportPath.toAbsolutePath());
    }

    static Path writeReport(Path archiveDir) throws IOException {
        RagEvaluationResult result = new RagEvaluationService().evaluate(archiveBenchmarkRequest());
        Path reportPath = archiveDir.resolve(REPORT_FILE_NAME);

        Files.createDirectories(archiveDir);
        Files.writeString(
                reportPath,
                archiveReport(result),
                StandardCharsets.UTF_8
        );
        return reportPath;
    }

    static String archiveReport(RagEvaluationResult result) {
        RagEvaluationResult.BenchmarkSummary summary = result.benchmarkSummary();
        StringBuilder report = new StringBuilder();
        report.append("# RAG Quality Evaluation Archive\n\n")
                .append("Benchmark: ").append(summary.name()).append('\n')
                .append("Benchmark ID: ").append(summary.benchmarkId()).append('\n')
                .append("Course ID: ").append(summary.courseId()).append('\n')
                .append("Version: ").append(summary.version()).append('\n')
                .append("Sample Count: ").append(summary.sampleCount()).append('\n')
                .append("Source Required Samples: ").append(summary.sourceRequiredSampleCount()).append('\n')
                .append("No-source Samples: ").append(summary.noSourceSampleCount()).append("\n\n")
                .append("## Metrics\n\n")
                .append("Recall@K: ").append(format(result.recallAtK())).append('\n')
                .append("Citation Accuracy: ").append(format(result.citationAccuracy())).append('\n')
                .append("Groundedness: ").append(format(result.groundedness())).append('\n')
                .append("No-source Refusal Rate: ").append(format(result.noSourceRefusalRate())).append("\n\n")
                .append("## Samples\n\n");

        for (RagEvaluationResult.SampleResult sample : result.sampleResults()) {
            report.append("- ").append(sample.sampleKey())
                    .append(": recallAtK=").append(format(sample.recallAtK()))
                    .append(", citationAccuracy=").append(format(sample.citationAccuracy()))
                    .append(", groundedness=").append(format(sample.groundedness()))
                    .append(", noSourceRefusal=").append(sample.noSourceRefusal())
                    .append('\n');
        }

        report.append("\n## Service Report\n\n")
                .append(result.report());
        return report.toString();
    }

    private static RagEvaluationRequest archiveBenchmarkRequest() {
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

    private static SourceCitation citation(String documentId, String documentName, String sectionTitle, double score) {
        return new SourceCitation(
                documentId,
                documentName,
                1,
                sectionTitle,
                "archive evaluation excerpt",
                score
        );
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
