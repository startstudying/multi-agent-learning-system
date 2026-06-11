package com.learningos.rag.parser;

import com.learningos.config.RagParserOcrProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConfigurableOcrFallbackService implements OcrFallbackService {

    private static final Pattern SAFE_REASON_CODE = Pattern.compile("[A-Z0-9_]{1,64}");

    private final RagParserOcrProperties properties;
    private final Map<String, OcrFallbackProvider> providers;

    public ConfigurableOcrFallbackService(RagParserOcrProperties properties, List<OcrFallbackProvider> providers) {
        this.properties = properties == null ? new RagParserOcrProperties(false, "none") : properties;
        this.providers = providers == null ? Map.of() : providers.stream()
                .filter(provider -> provider != null && normalize(provider.provider()) != null)
                .collect(Collectors.toUnmodifiableMap(
                        provider -> normalize(provider.provider()),
                        provider -> provider,
                        (first, ignored) -> first
                ));
    }

    @Override
    public OcrFallbackResult extractText(ParseInput input) {
        if (!properties.enabled()) {
            return disabled();
        }
        String providerName = normalize(properties.provider());
        if (providerName == null || "none".equals(providerName)) {
            return unavailable();
        }
        OcrFallbackProvider provider = providers.get(providerName);
        if (provider == null) {
            return unavailable();
        }
        try {
            return sanitize(provider.extractText(input));
        } catch (RuntimeException exception) {
            return failed();
        }
    }

    private OcrFallbackResult sanitize(OcrFallbackResult result) {
        if (result == null) {
            return failed();
        }
        OcrFallbackResult.Status status = result.status();
        String reasonCode = safeReasonCode(result.reasonCode(), status);
        String text = status == OcrFallbackResult.Status.SUCCEEDED ? result.text() : "";
        return new OcrFallbackResult(status, reasonCode, text, result.confidence());
    }

    private String safeReasonCode(String reasonCode, OcrFallbackResult.Status status) {
        if (reasonCode != null && SAFE_REASON_CODE.matcher(reasonCode).matches()) {
            return reasonCode;
        }
        return switch (status == null ? OcrFallbackResult.Status.UNAVAILABLE : status) {
            case DISABLED -> "OCR_DISABLED";
            case UNAVAILABLE -> "OCR_PROVIDER_UNAVAILABLE";
            case SUCCEEDED -> "OCR_PROVIDER_SUCCEEDED";
            case FAILED -> "OCR_PROVIDER_FAILED";
        };
    }

    private OcrFallbackResult disabled() {
        return new OcrFallbackResult(OcrFallbackResult.Status.DISABLED, "OCR_DISABLED", "");
    }

    private OcrFallbackResult unavailable() {
        return new OcrFallbackResult(OcrFallbackResult.Status.UNAVAILABLE, "OCR_PROVIDER_UNAVAILABLE", "");
    }

    private OcrFallbackResult failed() {
        return new OcrFallbackResult(OcrFallbackResult.Status.FAILED, "OCR_PROVIDER_FAILED", "");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
