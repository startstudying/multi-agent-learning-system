package com.learningos.rag.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RagEvaluationServiceTest {

    private final RagEvaluationService service = new RagEvaluationService();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void calculatesRecallCitationAccuracyAndGroundednessFromExpectedSourcesAndActualCitations() {
        var result = service.evaluate(new RagEvaluationRequest(
                List.of("doc_sql", "doc_join", "doc_index"),
                List.of(
                        citation("doc_sql"),
                        citation("doc_noise"),
                        citation("doc_join")
                ),
                2
        ));

        assertThat(result.recallAtK()).isEqualTo(1.0 / 3.0);
        assertThat(result.citationAccuracy()).isEqualTo(2.0 / 3.0);
        assertThat(result.groundedness()).isEqualTo(4.0 / 9.0);
        assertThat(result.expectedSourceCount()).isEqualTo(3);
        assertThat(result.evaluatedCitationCount()).isEqualTo(3);
        assertThat(result.relevantCitationCount()).isEqualTo(2);
    }

    @Test
    void handlesEmptyExpectedSourcesAndCitationsDeterministically() {
        var result = service.evaluate(new RagEvaluationRequest(
                List.of(),
                List.of(),
                5
        ));

        assertThat(result.recallAtK()).isEqualTo(1.0);
        assertThat(result.citationAccuracy()).isEqualTo(1.0);
        assertThat(result.groundedness()).isEqualTo(1.0);
        assertThat(result.expectedSourceCount()).isZero();
        assertThat(result.evaluatedCitationCount()).isZero();
        assertThat(result.relevantCitationCount()).isZero();
    }

    @Test
    void evaluatesCourseBenchmarkMetricsAndNoSourceRefusalRate() throws Exception {
        RagEvaluationRequest request = objectMapper.readValue("""
                {
                  "benchmark": {
                    "benchmarkId": "rag-sql-course",
                    "courseId": "course_sql",
                    "name": "SQL Course RAG Benchmark",
                    "version": "v1",
                    "topK": 2,
                    "samples": [
                      {
                        "sampleKey": "rag-source-001",
                        "question": "Why can JOIN duplicate rows?",
                        "expectedChunkIds": ["chunk_join", "chunk_index"],
                        "expectedAnswer": "JOIN combines every matching row pair.",
                        "forbiddenAnswerScope": "Do not cite material outside the SQL course.",
                        "expectedNoSource": false,
                        "actualAnswer": "JOIN combines every matching row pair.",
                        "actualCitations": [
                          {
                            "documentId": "chunk_join",
                            "documentName": "sql.md",
                            "pageNum": 1,
                            "sectionTitle": "JOIN",
                            "excerpt": "JOIN combines matching row pairs.",
                            "score": 0.96
                          },
                          {
                            "documentId": "chunk_noise",
                            "documentName": "noise.md",
                            "pageNum": 2,
                            "sectionTitle": "Noise",
                            "excerpt": "Not part of the expected answer.",
                            "score": 0.31
                          },
                          {
                            "documentId": "chunk_index",
                            "documentName": "index.md",
                            "pageNum": 3,
                            "sectionTitle": "Index",
                            "excerpt": "Indexes can help lookup matching rows.",
                            "score": 0.88
                          }
                        ],
                        "topK": 2
                      },
                      {
                        "sampleKey": "rag-nosource-001",
                        "question": "What does the course say about imaginary indexes?",
                        "expectedChunkIds": [],
                        "expectedAnswer": "",
                        "forbiddenAnswerScope": "Must refuse when no cited course material exists.",
                        "expectedNoSource": true,
                        "actualAnswer": "NO_SOURCE: No cited course material was found.",
                        "actualCitations": [],
                        "topK": 2
                      }
                    ]
                  }
                }
                """, RagEvaluationRequest.class);

        Map<String, Object> result = objectMapper.convertValue(
                service.evaluate(request),
                new TypeReference<>() {
                }
        );

        assertThat((Double) result.get("recallAtK")).isCloseTo(0.5, within(0.000001));
        assertThat((Double) result.get("citationAccuracy")).isCloseTo(2.0 / 3.0, within(0.000001));
        assertThat((Double) result.get("groundedness")).isCloseTo(4.0 / 7.0, within(0.000001));
        assertThat((Double) result.get("noSourceRefusalRate")).isEqualTo(1.0);
        assertThat(result).containsEntry("expectedSourceCount", 2);
        assertThat(result).containsEntry("evaluatedCitationCount", 3);
        assertThat(result).containsEntry("relevantCitationCount", 2);

        assertThat((Map<String, Object>) result.get("benchmarkSummary"))
                .containsEntry("benchmarkId", "rag-sql-course")
                .containsEntry("sampleCount", 2)
                .containsEntry("noSourceSampleCount", 1)
                .containsEntry("sourceRequiredSampleCount", 1)
                .containsEntry("topK", 2);
        assertThat((List<Map<String, Object>>) result.get("sampleResults"))
                .hasSize(2)
                .anySatisfy(sample -> assertThat(sample)
                        .containsEntry("sampleKey", "rag-source-001")
                        .containsEntry("expectedChunkCount", 2)
                        .containsEntry("evaluatedCitationCount", 3)
                        .containsEntry("relevantCitationCount", 2))
                .anySatisfy(sample -> assertThat(sample)
                        .containsEntry("sampleKey", "rag-nosource-001")
                        .containsEntry("expectedNoSource", true)
                        .containsEntry("noSourceRefusal", true));
        assertThat((String) result.get("report"))
                .contains("SQL Course RAG Benchmark", "Recall@K", "No-source Refusal Rate");
    }

    private SourceCitation citation(String documentId) {
        return new SourceCitation(documentId, documentId + ".md", 1, "section", "excerpt", 1.0);
    }
}
