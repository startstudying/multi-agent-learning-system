package com.learningos.rag.application;

import com.learningos.rag.api.dto.RagQueryDtos.SourceCitation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class CitationCoverageAnalyzer {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[。！？.!?；;\\r\\n]+");
    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final List<String> NO_SOURCE_MARKERS = List.of(
            "NO_SOURCE",
            "NO SOURCE",
            "NO CITED COURSE MATERIAL",
            "NO CITED MATERIAL"
    );

    public CoverageResult analyze(String answer, List<SourceCitation> citations) {
        List<String> claims = coreClaims(answer);
        if (claims.isEmpty()) {
            return new CoverageResult(0, 0, 1.0, false);
        }
        Set<String> citationTerms = terms(citationCorpus(citations));
        int coveredClaims = 0;
        for (String claim : claims) {
            if (isCovered(claim, citationTerms)) {
                coveredClaims++;
            }
        }
        double coverage = (double) coveredClaims / claims.size();
        boolean possibleLeak = claims.size() > 1 && coveredClaims < claims.size();
        return new CoverageResult(claims.size(), coveredClaims, coverage, possibleLeak);
    }

    private List<String> coreClaims(String answer) {
        if (!hasText(answer) || containsNoSourceMarker(answer)) {
            return List.of();
        }
        List<String> claims = new ArrayList<>();
        for (String sentence : SENTENCE_SPLIT.split(answer)) {
            String normalized = normalize(sentence);
            if (normalized.length() >= 8 && !containsNoSourceMarker(normalized)) {
                claims.add(normalized);
            }
        }
        return List.copyOf(claims);
    }

    private boolean isCovered(String claim, Set<String> citationTerms) {
        Set<String> claimTerms = terms(claim);
        if (claimTerms.isEmpty()) {
            return false;
        }
        int overlap = 0;
        for (String term : claimTerms) {
            if (citationTerms.contains(term)) {
                overlap++;
            }
        }
        int required = Math.max(1, (int) Math.ceil(claimTerms.size() * 0.50));
        return overlap >= required;
    }

    private Set<String> terms(String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String normalized = normalize(value).toLowerCase(Locale.ROOT);
        for (String token : WORD_SPLIT.split(normalized)) {
            if (token.length() >= 4) {
                result.add(token);
            }
        }
        StringBuilder cjkRun = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char item = normalized.charAt(i);
            if (isCjk(item)) {
                cjkRun.append(item);
            } else {
                addCjkBigrams(result, cjkRun);
                cjkRun.setLength(0);
            }
        }
        addCjkBigrams(result, cjkRun);
        return result;
    }

    private void addCjkBigrams(Set<String> terms, StringBuilder cjkRun) {
        if (cjkRun.length() < 2) {
            return;
        }
        for (int i = 0; i < cjkRun.length() - 1; i++) {
            terms.add(cjkRun.substring(i, i + 2));
        }
    }

    private boolean isCjk(char item) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(item);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private String citationCorpus(List<SourceCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (SourceCitation citation : citations) {
            if (citation == null) {
                continue;
            }
            append(builder, citation.documentName());
            append(builder, citation.sectionTitle());
            append(builder, citation.excerpt());
        }
        return builder.toString();
    }

    private boolean containsNoSourceMarker(String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        return NO_SOURCE_MARKERS.stream().anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private void append(StringBuilder builder, String value) {
        if (hasText(value)) {
            builder.append(' ').append(value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record CoverageResult(
            int coreClaimCount,
            int coveredCoreClaimCount,
            double coreClaimCitationCoverage,
            boolean uncitedContextLeak
    ) {
    }
}
