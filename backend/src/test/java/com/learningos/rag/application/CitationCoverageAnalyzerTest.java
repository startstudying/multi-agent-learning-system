package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitationCoverageAnalyzerTest {

    private final CitationCoverageAnalyzer analyzer = new CitationCoverageAnalyzer();

    @Test
    void marksCoreClaimsCoveredWhenCitationExcerptsContainTheConclusionTerms() {
        CitationCoverageAnalyzer.CoverageResult result = analyzer.analyze(
                "JOIN combines matching row pairs. Indexes help lookup matching rows.",
                List.of(
                        citation("chunk_join", "JOIN combines matching row pairs."),
                        citation("chunk_index", "Indexes help lookup matching rows.")
                )
        );

        assertThat(result.coreClaimCount()).isEqualTo(2);
        assertThat(result.coveredCoreClaimCount()).isEqualTo(2);
        assertThat(result.coreClaimCitationCoverage()).isEqualTo(1.0);
        assertThat(result.uncitedContextLeak()).isFalse();
    }

    @Test
    void detectsUncoveredCoreClaimAsPossibleUncitedContextLeak() {
        CitationCoverageAnalyzer.CoverageResult result = analyzer.analyze(
                "事务隔离影响并发可见性。幻读发生在范围查询看见新增行时。",
                List.of(citation("chunk_isolation", "事务隔离影响并发可见性。"))
        );

        assertThat(result.coreClaimCount()).isEqualTo(2);
        assertThat(result.coveredCoreClaimCount()).isEqualTo(1);
        assertThat(result.coreClaimCitationCoverage()).isEqualTo(0.5);
        assertThat(result.uncitedContextLeak()).isTrue();
    }

    @Test
    void treatsNoAnswerClaimsAsNotLeaking() {
        CitationCoverageAnalyzer.CoverageResult result = analyzer.analyze(
                "   ",
                List.of()
        );

        assertThat(result.coreClaimCount()).isZero();
        assertThat(result.coreClaimCitationCoverage()).isEqualTo(1.0);
        assertThat(result.uncitedContextLeak()).isFalse();
    }

    private SourceCitation citation(String documentId, String excerpt) {
        return new SourceCitation(documentId, documentId + ".md", 1, "section", excerpt, 0.95);
    }
}
