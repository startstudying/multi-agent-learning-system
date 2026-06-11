package com.learningos.health.application;

import com.learningos.config.AiModelProperties;
import com.learningos.config.AppProperties;
import com.learningos.config.RagVectorProperties;
import com.learningos.config.StorageProperties;
import com.learningos.health.api.HealthDtos.ComponentStatus;
import com.learningos.health.api.HealthDtos.HealthResponse;
import com.learningos.rag.vector.QdrantVectorHealthProbe;
import io.minio.MinioClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HealthService {

    private final AppProperties appProperties;
    private final StorageProperties storageProperties;
    private final AiModelProperties aiModelProperties;
    private final RagVectorProperties ragVectorProperties;
    private final RedisProperties redisProperties;
    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final ObjectProvider<QdrantVectorHealthProbe> qdrantVectorHealthProbeProvider;

    public HealthService(
            AppProperties appProperties,
            StorageProperties storageProperties,
            AiModelProperties aiModelProperties,
            RagVectorProperties ragVectorProperties,
            RedisProperties redisProperties,
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
            ObjectProvider<QdrantVectorHealthProbe> qdrantVectorHealthProbeProvider
    ) {
        this.appProperties = appProperties;
        this.storageProperties = storageProperties;
        this.aiModelProperties = aiModelProperties;
        this.ragVectorProperties = ragVectorProperties;
        this.redisProperties = redisProperties;
        this.dataSourceProvider = dataSourceProvider;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
        this.qdrantVectorHealthProbeProvider = qdrantVectorHealthProbeProvider;
    }

    public HealthResponse currentHealth() {
        return new HealthResponse(
                ComponentStatus.up("application is running", Map.of(
                        "environment", safeEnvironment(appProperties.environment())
                )),
                databaseStatus(),
                redisStatus(),
                storageStatus(),
                modelStatus(),
                vectorStatus()
        );
    }

    private ComponentStatus vectorStatus() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", ragVectorProperties.enabled());
        metadata.put("provider", ragVectorProperties.provider());
        QdrantVectorHealthProbe probe = qdrantVectorHealthProbeProvider.getIfAvailable();
        if (probe == null) {
            return ComponentStatus.disabled("vector health probe unavailable", metadata);
        }
        QdrantVectorHealthProbe.QdrantHealthSnapshot snapshot = probe.probe();
        metadata.putAll(snapshot.metadata());
        return switch (snapshot.status()) {
            case "UP" -> ComponentStatus.up(snapshot.detail(), metadata);
            case "DISABLED" -> ComponentStatus.disabled(snapshot.detail(), metadata);
            default -> ComponentStatus.down(snapshot.detail(), metadata);
        };
    }

    private ComponentStatus databaseStatus() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("configured", dataSourceProvider.getIfAvailable() != null);
        metadata.put("checked", true);

        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            metadata.put("errorCode", "CONNECTION_FACTORY_UNAVAILABLE");
            return ComponentStatus.down("database connection check failed", metadata);
        }
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return ComponentStatus.up("database connection is valid", metadata);
            }
            metadata.put("errorCode", "CONNECTION_FAILED");
            return ComponentStatus.down("database connection check failed", metadata);
        } catch (SQLException ex) {
            metadata.put("errorCode", "CONNECTION_FAILED");
            return ComponentStatus.down("database connection check failed", metadata);
        }
    }

    private ComponentStatus redisStatus() {
        boolean configured = hasText(redisProperties.getHost());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("configured", configured);

        if (!configured) {
            metadata.put("checked", false);
            metadata.put("errorCode", "CONFIG_INCOMPLETE");
            return ComponentStatus.unconfigured("redis configuration incomplete", metadata);
        }

        metadata.put("checked", true);
        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        if (connectionFactory == null) {
            metadata.put("errorCode", "CONNECTION_FACTORY_UNAVAILABLE");
            return ComponentStatus.down("redis connection check failed", metadata);
        }
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                return ComponentStatus.up("redis ping succeeded", metadata);
            }
            metadata.put("errorCode", "PING_FAILED");
            return ComponentStatus.down("redis connection check failed", metadata);
        } catch (RuntimeException ex) {
            metadata.put("errorCode", "CONNECTION_FAILED");
            return ComponentStatus.down("redis connection check failed", metadata);
        }
    }

    private ComponentStatus storageStatus() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        boolean configured = storageProperties.configured();
        metadata.put("configured", configured);

        if (!configured) {
            metadata.put("checked", false);
            metadata.put("errorCode", "CONFIG_INCOMPLETE");
            return ComponentStatus.unconfigured("minio configuration incomplete", metadata);
        }

        metadata.put("checked", true);
        try {
            URI endpoint = new URI(storageProperties.endpoint());
            if (endpoint.getScheme() == null || endpoint.getHost() == null) {
                throw new URISyntaxException(storageProperties.endpoint(), "endpoint host or scheme missing");
            }
            MinioClient.builder()
                    .endpoint(storageProperties.endpoint())
                    .credentials(storageProperties.accessKey(), storageProperties.secretKey())
                    .build();
            return ComponentStatus.configured("minio client configuration valid", metadata);
        } catch (RuntimeException | URISyntaxException ex) {
            metadata.put("errorCode", "CLIENT_BUILD_FAILED");
            return ComponentStatus.down("minio client configuration failed", metadata);
        }
    }

    private ComponentStatus modelStatus() {
        boolean providerConfigured = aiModelProperties.configured();
        boolean chatModelConfigured = hasText(aiModelProperties.chatModel());
        boolean embeddingModelConfigured = hasText(aiModelProperties.embeddingModel());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("configured", providerConfigured);
        metadata.put("providerConfigured", providerConfigured);
        metadata.put("chatModelConfigured", chatModelConfigured);
        metadata.put("embeddingModelConfigured", embeddingModelConfigured);

        if (!providerConfigured) {
            return ComponentStatus.disabled("model provider disabled", metadata);
        }
        if (!chatModelConfigured) {
            metadata.put("errorCode", "CONFIG_INCOMPLETE");
            return ComponentStatus.unconfigured("model configuration incomplete", metadata);
        }
        return ComponentStatus.configured("model provider configured", metadata);
    }

    private String safeEnvironment(String environment) {
        return hasText(environment) ? environment : "unknown";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
