package com.learningos.health.api;

import com.learningos.config.AiModelProperties;
import com.learningos.config.AppProperties;
import com.learningos.config.RagVectorProperties;
import com.learningos.config.StorageProperties;
import com.learningos.health.application.HealthService;
import com.learningos.rag.vector.QdrantVectorHealthProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@WebMvcTest(
        controllers = HealthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import({
        HealthService.class,
        HealthControllerTest.PropertiesConfig.class
})
@TestPropertySource(properties = {
        "spring.data.redis.host=redis.internal.example",
        "learning-os.app.environment=test",
        "learning-os.app.trace-header=X-Trace-Id",
        "learning-os.ai-model.provider=none"
})
class HealthControllerTest {

    private final MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private StorageProperties storageProperties;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private QdrantVectorHealthProbe qdrantVectorHealthProbe;

    HealthControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void stubVectorHealth() {
        when(qdrantVectorHealthProbe.probe()).thenReturn(
                QdrantVectorHealthProbe.QdrantHealthSnapshot.disabled("qdrant vector index disabled")
        );
    }

    @Test
    void healthReturnsDeepDependencyShapeWithoutLeakingSensitiveConfiguration() throws Exception {
        stubHealthyDatabase();
        stubHealthyStorage();
        stubRedisDown();

        String responseBody = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.application.status").value("UP"))
                .andExpect(jsonPath("$.data.database.status").value("UP"))
                .andExpect(jsonPath("$.data.database.metadata.checked").value(true))
                .andExpect(jsonPath("$.data.redis.status").value("DOWN"))
                .andExpect(jsonPath("$.data.redis.metadata.checked").value(true))
                .andExpect(jsonPath("$.data.minio.status").value("CONFIGURED"))
                .andExpect(jsonPath("$.data.minio.metadata.checked").value(true))
                .andExpect(jsonPath("$.data.model.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.vector.status").value("DISABLED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody)
                .doesNotContain(
                        "minioadmin",
                        "redis.internal.example",
                        "redis-password",
                        "access-key",
                        "secret-key",
                        "password",
                        "token",
                        "localhost",
                        "localhost:9000",
                        "learning-os-documents"
                );
    }

    @Test
    void healthReturnsDatabaseDownWithoutBreakingEnvelope() throws Exception {
        stubDatabaseDown();
        stubHealthyStorage();
        stubRedisDown();

        String responseBody = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.database.status").value("DOWN"))
                .andExpect(jsonPath("$.data.database.metadata.errorCode").value("CONNECTION_FAILED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("jdbc:mysql://secret-host", "password=secret");
    }

    @Test
    void healthReturnsMinioDownWithoutBreakingEnvelope() throws Exception {
        stubHealthyDatabase();
        stubRedisDown();
        stubMinioDown();

        String responseBody = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.minio.status").value("DOWN"))
                .andExpect(jsonPath("$.data.minio.metadata.errorCode").value("CLIENT_BUILD_FAILED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).doesNotContain("not-a-valid-endpoint", "access-key", "secret-key", "bucket");
    }

    @EnableConfigurationProperties({
            AppProperties.class,
            AiModelProperties.class,
            RedisProperties.class,
            RagVectorProperties.class
    })
    static class PropertiesConfig {
    }

    private void stubHealthyDatabase() throws SQLException {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
    }

    private void stubDatabaseDown() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("jdbc:mysql://secret-host/db password=secret"));
    }

    private void stubHealthyStorage() {
        when(storageProperties.configured()).thenReturn(true);
        when(storageProperties.endpoint()).thenReturn("http://localhost:9000");
        when(storageProperties.accessKey()).thenReturn("minioadmin");
        when(storageProperties.secretKey()).thenReturn("minioadmin");
        when(storageProperties.bucket()).thenReturn("learning-os-documents");
    }

    private void stubMinioDown() {
        when(storageProperties.configured()).thenReturn(true);
        when(storageProperties.endpoint()).thenReturn("not-a-valid-endpoint");
        when(storageProperties.accessKey()).thenReturn("access-key");
        when(storageProperties.secretKey()).thenReturn("secret-key");
        when(storageProperties.bucket()).thenReturn("bucket");
    }

    private void stubRedisDown() {
        when(redisConnectionFactory.getConnection())
                .thenThrow(new IllegalStateException("redis.internal.example redis-password"));
    }
}
