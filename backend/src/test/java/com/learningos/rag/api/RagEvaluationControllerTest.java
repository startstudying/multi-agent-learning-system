package com.learningos.rag.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class RagEvaluationControllerTest {

    private final MockMvc mockMvc;

    RagEvaluationControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void exposesRagQualityEvaluationReport() throws Exception {
        mockMvc.perform(post("/api/rag/evaluations")
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedSourceIds": ["doc_sql", "doc_join", "doc_index"],
                                  "actualCitations": [
                                    {
                                      "documentId": "doc_sql",
                                      "documentName": "sql.md",
                                      "pageNum": 1,
                                      "sectionTitle": "JOIN",
                                      "excerpt": "SQL JOIN source",
                                      "score": 1.0
                                    },
                                    {
                                      "documentId": "doc_noise",
                                      "documentName": "noise.md",
                                      "pageNum": 2,
                                      "sectionTitle": "Noise",
                                      "excerpt": "Noise",
                                      "score": 0.4
                                    },
                                    {
                                      "documentId": "doc_join",
                                      "documentName": "join.md",
                                      "pageNum": 3,
                                      "sectionTitle": "JOIN",
                                      "excerpt": "JOIN source",
                                      "score": 0.9
                                    }
                                  ],
                                  "topK": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.recallAtK").value(1.0 / 3.0))
                .andExpect(jsonPath("$.data.citationAccuracy").value(2.0 / 3.0))
                .andExpect(jsonPath("$.data.groundedness").value(4.0 / 9.0))
                .andExpect(jsonPath("$.data.expectedSourceCount").value(3))
                .andExpect(jsonPath("$.data.evaluatedCitationCount").value(3))
                .andExpect(jsonPath("$.data.relevantCitationCount").value(2));
    }

    @Test
    void exposesBenchmarkSummaryNoSourceMetricAndArchivableReport() throws Exception {
        mockMvc.perform(post("/api/rag/evaluations")
                        .header("X-User-Id", "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.recallAtK").value(0.5))
                .andExpect(jsonPath("$.data.citationAccuracy").value(2.0 / 3.0))
                .andExpect(jsonPath("$.data.groundedness").value(closeTo(4.0 / 7.0, 0.000001)))
                .andExpect(jsonPath("$.data.noSourceRefusalRate").value(1.0))
                .andExpect(jsonPath("$.data.expectedSourceCount").value(2))
                .andExpect(jsonPath("$.data.evaluatedCitationCount").value(3))
                .andExpect(jsonPath("$.data.relevantCitationCount").value(2))
                .andExpect(jsonPath("$.data.benchmarkSummary.benchmarkId").value("rag-sql-course"))
                .andExpect(jsonPath("$.data.benchmarkSummary.name").value("SQL Course RAG Benchmark"))
                .andExpect(jsonPath("$.data.benchmarkSummary.sampleCount").value(2))
                .andExpect(jsonPath("$.data.benchmarkSummary.noSourceSampleCount").value(1))
                .andExpect(jsonPath("$.data.benchmarkSummary.sourceRequiredSampleCount").value(1))
                .andExpect(jsonPath("$.data.benchmarkSummary.topK").value(2))
                .andExpect(jsonPath("$.data.sampleResults[0].sampleKey").value("rag-source-001"))
                .andExpect(jsonPath("$.data.sampleResults[0].recallAtK").value(0.5))
                .andExpect(jsonPath("$.data.sampleResults[0].citationAccuracy").value(2.0 / 3.0))
                .andExpect(jsonPath("$.data.sampleResults[1].sampleKey").value("rag-nosource-001"))
                .andExpect(jsonPath("$.data.sampleResults[1].expectedNoSource").value(true))
                .andExpect(jsonPath("$.data.sampleResults[1].noSourceRefusal").value(true))
                .andExpect(jsonPath("$.data.report").value(containsString("SQL Course RAG Benchmark")))
                .andExpect(jsonPath("$.data.report").value(containsString("Recall@K")))
                .andExpect(jsonPath("$.data.report").value(containsString("No-source Refusal Rate")));
    }
}
