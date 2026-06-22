package com.learningos.common.privacy;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class MemoryPrivacyPolicy {

    public static final String RAG_REPLAY_REDACTED = "RAG_REPLAY_REDACTED";

    public String questionLogValue(String rawQuestion) {
        String normalized = normalize(rawQuestion);
        return "questionHash=" + sha256(normalized)
                + ";questionLength=" + normalized.length()
                + ";policy=hash_only_v1";
    }

    public String citationExcerptValue(String rawExcerpt, String citationRef) {
        String normalized = normalize(rawExcerpt);
        return "citationRef=" + safeReference(citationRef)
                + ";excerptHash=" + sha256(normalized)
                + ";excerptLength=" + normalized.length()
                + ";policy=citation_ref_hash_v1";
    }

    public String replayRedactedAnswer() {
        return RAG_REPLAY_REDACTED
                + ": durable replay stores trace, retrieval metadata, and citation references only.";
    }

    public String profileRef(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return "profile:none";
        }
        return "profile:" + safeReference(profileId);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String safeReference(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder(Math.min(normalized.length(), 160));
        for (int i = 0; i < normalized.length() && builder.length() < 160; i++) {
            char item = normalized.charAt(i);
            if (Character.isLetterOrDigit(item)
                    || item == '_'
                    || item == '-'
                    || item == ':'
                    || item == '.'
                    || item == '#') {
                builder.append(item);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
