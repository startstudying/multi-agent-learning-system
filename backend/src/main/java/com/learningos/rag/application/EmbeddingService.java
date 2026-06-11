package com.learningos.rag.application;

import com.learningos.agent.application.EmbeddingModelFactory;
import com.learningos.agent.application.ModelProviderService;
import com.learningos.config.AiModelProperties;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class EmbeddingService {

    private static final String NOOP_MODEL_VERSION = "noop-embedding-v1";
    private static final String PROVIDER_NOT_CONFIGURED = "EMBEDDING_PROVIDER_NOT_CONFIGURED";
    private static final String PROVIDER_ERROR = "EMBEDDING_PROVIDER_ERROR";

    private final AiModelProperties properties;
    private final EmbeddingModel embeddingModel;
    private final ModelProviderService modelProviderService;
    private final EmbeddingModelFactory embeddingModelFactory;

    public EmbeddingService(AiModelProperties properties) {
        this(properties, (EmbeddingModel) null, null, null);
    }

    @Autowired
    public EmbeddingService(
            AiModelProperties properties,
            ObjectProvider<EmbeddingModel> embeddingModelProvider,
            ObjectProvider<ModelProviderService> modelProviderServiceProvider,
            ObjectProvider<EmbeddingModelFactory> embeddingModelFactoryProvider
    ) {
        this(
                properties,
                embeddingModelProvider.getIfAvailable(),
                modelProviderServiceProvider.getIfAvailable(),
                embeddingModelFactoryProvider.getIfAvailable()
        );
    }

    EmbeddingService(AiModelProperties properties, EmbeddingModel embeddingModel) {
        this(properties, embeddingModel, null, null);
    }

    EmbeddingService(
            AiModelProperties properties,
            EmbeddingModel embeddingModel,
            ModelProviderService modelProviderService,
            EmbeddingModelFactory embeddingModelFactory
    ) {
        this.properties = properties;
        this.embeddingModel = embeddingModel;
        this.modelProviderService = modelProviderService;
        this.embeddingModelFactory = embeddingModelFactory;
    }

    public String currentModelVersion() {
        var registryProvider = resolveRegistryProvider();
        if (registryProvider.isPresent()) {
            return registryProvider.get().embeddingModel();
        }
        if (isEnabled() && hasText(properties.embeddingModel())) {
            return properties.embeddingModel().trim();
        }
        return NOOP_MODEL_VERSION;
    }

    public boolean isEnabled() {
        if (resolveRegistryProvider().isPresent()) {
            return true;
        }
        return properties.configured() && hasText(properties.embeddingModel());
    }

    public EmbeddingBatchResult embedDocumentChunks(String documentId, String kbId, List<ChunkEmbeddingInput> chunks) {
        long startedAt = System.nanoTime();
        int chunkCount = chunks == null ? 0 : chunks.size();
        if (!isEnabled()) {
            return EmbeddingBatchResult.disabled(currentModelVersion(), chunkCount);
        }
        EmbeddingModel activeModel = resolveEmbeddingModel();
        if (activeModel == null) {
            return providerError(startedAt, chunkCount, PROVIDER_NOT_CONFIGURED);
        }
        try {
            List<String> inputs = chunks == null
                    ? List.of()
                    : chunks.stream().map(ChunkEmbeddingInput::content).map(this::safeContent).toList();
            if (inputs.isEmpty()) {
                return new EmbeddingBatchResult(
                        EmbeddingStatus.SUCCEEDED,
                        currentModelVersion(),
                        0,
                        latencyMs(startedAt),
                        null,
                        List.of()
                );
            }
            EmbeddingResponse response = activeModel.call(new EmbeddingRequest(
                    inputs,
                    EmbeddingOptionsBuilder.builder().withModel(currentModelVersion()).build()
            ));
            validateResponse(response, chunkCount);
            return new EmbeddingBatchResult(
                    EmbeddingStatus.SUCCEEDED,
                    responseModelVersion(response),
                    chunkCount,
                    latencyMs(startedAt),
                    null,
                    vectors(chunks, response)
            );
        } catch (RuntimeException ex) {
            return providerError(startedAt, chunkCount, PROVIDER_ERROR);
        }
    }

    public QueryEmbeddingResult embedQuery(String question) {
        long startedAt = System.nanoTime();
        if (!isEnabled()) {
            return QueryEmbeddingResult.disabled(currentModelVersion());
        }
        EmbeddingModel activeModel = resolveEmbeddingModel();
        if (activeModel == null) {
            return queryProviderError(startedAt, PROVIDER_NOT_CONFIGURED);
        }
        try {
            EmbeddingResponse response = activeModel.call(new EmbeddingRequest(
                    List.of(safeContent(question)),
                    EmbeddingOptionsBuilder.builder().withModel(currentModelVersion()).build()
            ));
            validateResponse(response, 1);
            Embedding first = response.getResults().getFirst();
            return new QueryEmbeddingResult(
                    EmbeddingStatus.SUCCEEDED,
                    responseModelVersion(response),
                    new EmbeddingVector("query", first.getOutput()),
                    latencyMs(startedAt),
                    null
            );
        } catch (RuntimeException ex) {
            return queryProviderError(startedAt, PROVIDER_ERROR);
        }
    }

    private EmbeddingModel resolveEmbeddingModel() {
        var registryProvider = resolveRegistryProvider();
        if (registryProvider.isPresent() && embeddingModelFactory != null) {
            ModelProviderService.ResolvedEmbeddingProvider resolved = registryProvider.get();
            return embeddingModelFactory.createEmbeddingModel(
                    resolved.baseUrl(),
                    resolved.apiKey(),
                    resolved.embeddingModel()
            );
        }
        return embeddingModel;
    }

    private java.util.Optional<ModelProviderService.ResolvedEmbeddingProvider> resolveRegistryProvider() {
        if (modelProviderService == null) {
            return java.util.Optional.empty();
        }
        return modelProviderService.resolveDefaultEmbeddingProvider();
    }

    private void validateResponse(EmbeddingResponse response, int chunkCount) {
        if (response == null || response.getResults() == null || response.getResults().size() != chunkCount) {
            throw new IllegalStateException(PROVIDER_ERROR);
        }
        for (Embedding result : response.getResults()) {
            if (result == null || result.getOutput() == null || result.getOutput().length == 0) {
                throw new IllegalStateException(PROVIDER_ERROR);
            }
        }
    }

    private String responseModelVersion(EmbeddingResponse response) {
        if (response != null && response.getMetadata() != null && hasText(response.getMetadata().getModel())) {
            return safeModel(response.getMetadata().getModel(), currentModelVersion());
        }
        return currentModelVersion();
    }

    private List<EmbeddingVector> vectors(List<ChunkEmbeddingInput> chunks, EmbeddingResponse response) {
        List<Embedding> results = response.getResults();
        java.util.ArrayList<EmbeddingVector> vectors = new java.util.ArrayList<>(results.size());
        for (int index = 0; index < results.size(); index++) {
            vectors.add(new EmbeddingVector(chunks.get(index).chunkId(), results.get(index).getOutput()));
        }
        return vectors;
    }

    private EmbeddingBatchResult providerError(long startedAt, int chunkCount, String errorCode) {
        return new EmbeddingBatchResult(
                EmbeddingStatus.PROVIDER_ERROR,
                currentModelVersion(),
                chunkCount,
                latencyMs(startedAt),
                errorCode
        );
    }

    private QueryEmbeddingResult queryProviderError(long startedAt, String errorCode) {
        return new QueryEmbeddingResult(
                EmbeddingStatus.PROVIDER_ERROR,
                currentModelVersion(),
                null,
                latencyMs(startedAt),
                errorCode
        );
    }

    private String safeContent(String content) {
        return content == null ? "" : content;
    }

    private String safeModel(String candidate, String fallback) {
        if (!hasText(candidate)) {
            return fallback;
        }
        String trimmed = candidate.trim();
        String lower = trimmed.toLowerCase();
        if (lower.contains("sk-") || lower.contains("apikey") || lower.contains("api_key")
                || lower.contains("secret") || lower.contains("https://") || lower.contains("http://")) {
            return fallback;
        }
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private long latencyMs(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
