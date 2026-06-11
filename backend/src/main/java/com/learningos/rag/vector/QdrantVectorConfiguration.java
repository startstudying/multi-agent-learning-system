package com.learningos.rag.vector;

import com.learningos.config.RagVectorProperties;
import com.learningos.rag.application.NoopVectorIndexAdapter;
import com.learningos.rag.application.VectorIndexAdapter;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagVectorProperties.class)
public class QdrantVectorConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "learning-os.rag.vector", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${learning-os.rag.vector.provider:none}' == 'qdrant'")
    @ConditionalOnMissingBean(QdrantVectorOperations.class)
    QdrantClient qdrantClient(RagVectorProperties properties) {
        properties.validateQdrant();
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                properties.qdrant().host(),
                properties.qdrant().port(),
                properties.qdrant().useTls()
        ).withTimeout(properties.qdrant().timeout());
        if (!properties.qdrant().apiKey().isBlank()) {
            builder.withApiKey(properties.qdrant().apiKey());
        }
        return new QdrantClient(builder.build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "learning-os.rag.vector", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${learning-os.rag.vector.provider:none}' == 'qdrant'")
    @ConditionalOnMissingBean(QdrantVectorOperations.class)
    QdrantVectorOperations qdrantVectorOperations(QdrantClient qdrantClient) {
        return new NativeQdrantVectorOperations(qdrantClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "learning-os.rag.vector", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${learning-os.rag.vector.provider:none}' == 'qdrant'")
    VectorIndexAdapter qdrantVectorIndexAdapter(
            RagVectorProperties properties,
            QdrantVectorOperations operations
    ) {
        properties.validateQdrant();
        return new QdrantVectorIndexAdapter(properties, operations);
    }

    @Bean
    @ConditionalOnMissingBean(VectorIndexAdapter.class)
    VectorIndexAdapter noopVectorIndexAdapter() {
        return new NoopVectorIndexAdapter();
    }
}
