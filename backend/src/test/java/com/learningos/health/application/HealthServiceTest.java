package com.learningos.health.application;

import com.learningos.config.AiModelProperties;
import com.learningos.config.AppProperties;
import com.learningos.config.RagVectorProperties;
import com.learningos.config.StorageProperties;
import com.learningos.health.api.HealthDtos.HealthResponse;
import com.learningos.rag.vector.QdrantVectorHealthProbe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthServiceTest {

    @Test
    void databaseConnectionFailureReturnsDownWithoutRawException() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("jdbc:mysql://secret-host/db password=secret"));

        HealthResponse response = service(dataSource, null, redisProperties("")).currentHealth();

        assertThat(response.database().status()).isEqualTo("DOWN");
        assertThat(response.database().detail()).isEqualTo("database connection check failed");
        assertThat(response.database().metadata()).containsEntry("errorCode", "CONNECTION_FAILED");
        assertThat(response.toString()).doesNotContain("secret-host", "password=secret");
    }

    @Test
    void redisPingSuccessReturnsUp() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        HealthResponse response = service(null, connectionFactory, redisProperties("redis.internal")).currentHealth();

        assertThat(response.redis().status()).isEqualTo("UP");
        assertThat(response.redis().metadata()).containsEntry("configured", true)
                .containsEntry("checked", true);
    }

    @Test
    void redisPingFailureReturnsDownWithoutRawException() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(connectionFactory.getConnection()).thenThrow(new IllegalStateException("redis.internal password=secret"));

        HealthResponse response = service(null, connectionFactory, redisProperties("redis.internal")).currentHealth();

        assertThat(response.redis().status()).isEqualTo("DOWN");
        assertThat(response.redis().detail()).isEqualTo("redis connection check failed");
        assertThat(response.redis().metadata()).containsEntry("errorCode", "CONNECTION_FAILED");
        assertThat(response.toString()).doesNotContain("redis.internal", "password=secret");
    }

    @Test
    void redisWithoutHostReturnsUnconfiguredAndSkipsConnectionAttempt() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        HealthResponse response = service(null, connectionFactory, redisProperties("")).currentHealth();

        assertThat(response.redis().status()).isEqualTo("UNCONFIGURED");
        assertThat(response.redis().metadata()).containsEntry("configured", false)
                .containsEntry("checked", false)
                .containsEntry("errorCode", "CONFIG_INCOMPLETE");
        verify(connectionFactory, never()).getConnection();
    }

    @Test
    void minioIncompleteConfigurationReturnsUnconfigured() {
        HealthResponse response = service(
                null,
                null,
                redisProperties(""),
                new StorageProperties("http://localhost:9000", "", "", "bucket"),
                new AiModelProperties("none", "", "")
        ).currentHealth();

        assertThat(response.minio().status()).isEqualTo("UNCONFIGURED");
        assertThat(response.minio().metadata()).containsEntry("configured", false)
                .containsEntry("checked", false)
                .containsEntry("errorCode", "CONFIG_INCOMPLETE");
        assertThat(response.toString()).doesNotContain("localhost:9000", "bucket");
    }

    @Test
    void minioClientBuildFailureReturnsDownWithoutRawConfiguration() {
        HealthResponse response = service(
                null,
                null,
                redisProperties(""),
                new StorageProperties("not-a-valid-endpoint", "access-key", "secret-key", "bucket"),
                new AiModelProperties("none", "", "")
        ).currentHealth();

        assertThat(response.minio().status()).isEqualTo("DOWN");
        assertThat(response.minio().detail()).isEqualTo("minio client configuration failed");
        assertThat(response.minio().metadata()).containsEntry("errorCode", "CLIENT_BUILD_FAILED");
        assertThat(response.toString()).doesNotContain("not-a-valid-endpoint", "access-key", "secret-key", "bucket");
    }

    @Test
    void nonNoneModelProviderWithModelNameReturnsConfigured() {
        HealthResponse response = service(
                null,
                null,
                redisProperties(""),
                defaultStorage(),
                new AiModelProperties("openai", "gpt-test", "")
        ).currentHealth();

        assertThat(response.model().status()).isEqualTo("CONFIGURED");
        assertThat(response.model().metadata()).containsEntry("configured", true)
                .containsEntry("providerConfigured", true)
                .containsEntry("chatModelConfigured", true)
                .containsEntry("embeddingModelConfigured", false);
        assertThat(response.toString()).doesNotContain("openai", "gpt-test");
    }

    @Test
    void nonNoneModelProviderWithoutModelNameReturnsUnconfigured() {
        HealthResponse response = service(
                null,
                null,
                redisProperties(""),
                defaultStorage(),
                new AiModelProperties("openai", "", "")
        ).currentHealth();

        assertThat(response.model().status()).isEqualTo("UNCONFIGURED");
        assertThat(response.model().metadata()).containsEntry("configured", true)
                .containsEntry("providerConfigured", true)
                .containsEntry("chatModelConfigured", false)
                .containsEntry("embeddingModelConfigured", false);
        assertThat(response.toString()).doesNotContain("openai");
    }

    @Test
    void configuredDatabaseWithValidConnectionReturnsUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);

        HealthResponse response = service(dataSource, null, redisProperties("")).currentHealth();

        assertThat(response.database().status()).isEqualTo("UP");
        assertThat(response.database().metadata()).containsEntry("configured", true)
                .containsEntry("checked", true);
    }

    @Test
    void missingDatabaseBeanReturnsDown() {
        HealthResponse response = service(null, null, redisProperties("")).currentHealth();

        assertThat(response.database().status()).isEqualTo("DOWN");
        assertThat(response.database().detail()).isEqualTo("database connection check failed");
        assertThat(response.database().metadata()).containsEntry("configured", false)
                .containsEntry("checked", true)
                .containsEntry("errorCode", "CONNECTION_FACTORY_UNAVAILABLE");
    }

    private HealthService service(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            RedisProperties redisProperties
    ) {
        return service(
                dataSource,
                redisConnectionFactory,
                redisProperties,
                defaultStorage(),
                new AiModelProperties("none", "", "")
        );
    }

    private HealthService service(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            RedisProperties redisProperties,
            StorageProperties storageProperties,
            AiModelProperties aiModelProperties
    ) {
        return new HealthService(
                new AppProperties("test", "X-Trace-Id"),
                storageProperties,
                aiModelProperties,
                new RagVectorProperties(false, "none", null),
                redisProperties,
                provider(dataSource),
                provider(redisConnectionFactory),
                provider(null)
        );
    }

    private RedisProperties redisProperties(String host) {
        RedisProperties properties = new RedisProperties();
        properties.setHost(host);
        return properties;
    }

    private StorageProperties defaultStorage() {
        return new StorageProperties("", "", "", "");
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
