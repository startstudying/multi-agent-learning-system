package com.learningos.agent.application;

import com.learningos.agent.domain.ModelProvider;
import com.learningos.agent.dto.ModelProviderResponse;
import com.learningos.agent.dto.ModelProviderTestConnectionResponse;
import com.learningos.agent.dto.ModelProviderUpsertRequest;
import com.learningos.agent.repository.ModelProviderRepository;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ModelProviderService {

    private static final Set<String> ALLOWED_PROVIDER_CODES = Set.of(
            "deepseek", "mimo", "dashscope", "openai", "custom"
    );
    private static final String TEST_SUCCEEDED = "SUCCEEDED";
    private static final String TEST_FAILED = "FAILED";
    private static final String TEST_ERROR = "MODEL_PROVIDER_TEST_FAILED";

    private final ModelProviderRepository modelProviderRepository;
    private final ModelProviderSecretCodec secretCodec;
    private final ChatModelFactory chatModelFactory;

    public ModelProviderService(
            ModelProviderRepository modelProviderRepository,
            ModelProviderSecretCodec secretCodec,
            ChatModelFactory chatModelFactory
    ) {
        this.modelProviderRepository = modelProviderRepository;
        this.secretCodec = secretCodec;
        this.chatModelFactory = chatModelFactory;
    }

    @Transactional
    public ModelProviderResponse create(ModelProviderUpsertRequest request, boolean currentUserAdmin, String createdBy) {
        requireAdmin(currentUserAdmin);
        String providerCode = normalizeProviderCode(request.providerCode());
        if (modelProviderRepository.findByProviderCode(providerCode).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "Model provider code already exists");
        }
        ModelProvider provider = new ModelProvider();
        applyRequest(provider, request, true);
        provider.setProviderCode(providerCode);
        provider.setCreatedBy(safeUserId(createdBy));
        if (Boolean.TRUE.equals(request.defaultProvider())) {
            modelProviderRepository.clearDefaultProviders();
            provider.setDefaultProvider(true);
        }
        return toResponse(modelProviderRepository.save(provider));
    }

    @Transactional
    public ModelProviderResponse update(String id, ModelProviderUpsertRequest request, boolean currentUserAdmin) {
        requireAdmin(currentUserAdmin);
        ModelProvider provider = findById(id);
        String providerCode = normalizeProviderCode(request.providerCode());
        if (!provider.getProviderCode().equals(providerCode)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Provider code cannot be changed");
        }
        applyRequest(provider, request, false);
        if (Boolean.TRUE.equals(request.defaultProvider())) {
            modelProviderRepository.clearDefaultProviders();
            provider.setDefaultProvider(true);
        } else if (Boolean.FALSE.equals(request.defaultProvider())) {
            provider.setDefaultProvider(false);
        }
        return toResponse(modelProviderRepository.save(provider));
    }

    @Transactional(readOnly = true)
    public List<ModelProviderResponse> list(boolean currentUserAdmin) {
        requireAdmin(currentUserAdmin);
        return modelProviderRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModelProviderResponse get(String id, boolean currentUserAdmin) {
        requireAdmin(currentUserAdmin);
        return toResponse(findById(id));
    }

    @Transactional
    public ModelProviderResponse setDefault(String id, boolean currentUserAdmin) {
        requireAdmin(currentUserAdmin);
        ModelProvider provider = findById(id);
        if (!provider.isEnabled()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Disabled provider cannot be default");
        }
        modelProviderRepository.clearDefaultProviders();
        provider.setDefaultProvider(true);
        return toResponse(modelProviderRepository.save(provider));
    }

    @Transactional(readOnly = true)
    public Optional<ResolvedChatProvider> resolveDefaultChatProvider() {
        return modelProviderRepository.findFirstByEnabledTrueAndDefaultProviderTrueOrderByUpdatedAtDesc()
                .filter(provider -> hasText(provider.getChatModel()))
                .map(provider -> new ResolvedChatProvider(
                        provider.getProviderCode(),
                        provider.getChatModel(),
                        provider.getBaseUrl(),
                        secretCodec.decrypt(provider.getApiKeyCiphertext())
                ));
    }

    @Transactional(readOnly = true)
    public Optional<ResolvedEmbeddingProvider> resolveDefaultEmbeddingProvider() {
        return modelProviderRepository.findFirstByEnabledTrueAndDefaultProviderTrueOrderByUpdatedAtDesc()
                .filter(provider -> hasText(provider.getEmbeddingModel()))
                .map(provider -> new ResolvedEmbeddingProvider(
                        provider.getProviderCode(),
                        provider.getEmbeddingModel(),
                        provider.getBaseUrl(),
                        secretCodec.decrypt(provider.getApiKeyCiphertext())
                ));
    }

    @Transactional(readOnly = true)
    public ModelProviderTestConnectionResponse testConnection(String id, boolean currentUserAdmin) {
        requireAdmin(currentUserAdmin);
        ModelProvider provider = findById(id);
        if (!hasText(provider.getChatModel())) {
            return new ModelProviderTestConnectionResponse(TEST_FAILED, provider.getProviderCode(), 0L, "CHAT_MODEL_NOT_CONFIGURED");
        }
        long startedAt = System.nanoTime();
        try {
            ChatModel chatModel = chatModelFactory.createChatModel(
                    provider.getBaseUrl(),
                    secretCodec.decrypt(provider.getApiKeyCiphertext()),
                    provider.getChatModel()
            );
            ChatResponse response = chatModel.call(new Prompt(new UserMessage("ping")));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return failed(provider.getProviderCode(), startedAt);
            }
            return new ModelProviderTestConnectionResponse(
                    TEST_SUCCEEDED,
                    provider.getProviderCode(),
                    latencyMs(startedAt),
                    null
            );
        } catch (RuntimeException ex) {
            return failed(provider.getProviderCode(), startedAt);
        }
    }

    private ModelProviderTestConnectionResponse failed(String providerCode, long startedAt) {
        return new ModelProviderTestConnectionResponse(
                TEST_FAILED,
                providerCode,
                latencyMs(startedAt),
                TEST_ERROR
        );
    }

    private void applyRequest(ModelProvider provider, ModelProviderUpsertRequest request, boolean creating) {
        provider.setDisplayName(requiredText(request.displayName(), "Display name is required"));
        provider.setRemark(blankToNull(request.remark()));
        provider.setWebsiteUrl(normalizeWebsiteUrl(request.websiteUrl()));
        provider.setBaseUrl(OpenAiCompatibleChatModelFactory.normalizeBaseUrl(request.baseUrl()));
        provider.setChatModel(blankToNull(request.chatModel()));
        provider.setEmbeddingModel(blankToNull(request.embeddingModel()));
        if (creating || shouldReplaceApiKey(request.apiKey())) {
            provider.setApiKeyCiphertext(secretCodec.encrypt(requiredText(request.apiKey(), "API key is required")));
        }
        if (request.enabled() != null) {
            provider.setEnabled(request.enabled());
        } else if (creating) {
            provider.setEnabled(true);
        }
    }

    private boolean shouldReplaceApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        String trimmed = apiKey.trim();
        return !trimmed.contains("***");
    }

    private ModelProvider findById(String id) {
        return modelProviderRepository.findById(requiredText(id, "Provider id is required"))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Model provider not found"));
    }

    private ModelProviderResponse toResponse(ModelProvider provider) {
        return ModelProviderResponse.from(provider, secretCodec.decrypt(provider.getApiKeyCiphertext()));
    }

    private String normalizeProviderCode(String providerCode) {
        String normalized = requiredText(providerCode, "Provider code is required").toLowerCase(Locale.ROOT);
        if (!ALLOWED_PROVIDER_CODES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported provider code");
        }
        return normalized;
    }

    private String normalizeWebsiteUrl(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return null;
        }
        URI uri = URI.create(websiteUrl.trim());
        String scheme = uri.getScheme();
        if (scheme == null || uri.getHost() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Website URL must be a valid URL");
        }
        return uri.toString();
    }

    private void requireAdmin(boolean currentUserAdmin) {
        if (!currentUserAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Model provider admin access required");
        }
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeUserId(String createdBy) {
        return createdBy == null || createdBy.isBlank() ? null : createdBy.trim();
    }

    private long latencyMs(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ResolvedChatProvider(
            String providerCode,
            String chatModel,
            String baseUrl,
            String apiKey
    ) {
    }

    public record ResolvedEmbeddingProvider(
            String providerCode,
            String embeddingModel,
            String baseUrl,
            String apiKey
    ) {
    }
}
