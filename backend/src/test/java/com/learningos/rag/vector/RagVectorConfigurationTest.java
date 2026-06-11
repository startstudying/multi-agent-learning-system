package com.learningos.rag.vector;

import com.learningos.rag.application.NoopVectorIndexAdapter;
import com.learningos.rag.application.VectorIndexAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RagVectorConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QdrantVectorConfiguration.class));

    @Test
    void defaultsToNoopWhenVectorDbIsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(VectorIndexAdapter.class);
            assertThat(context).hasSingleBean(NoopVectorIndexAdapter.class);
            assertThat(context).doesNotHaveBean(QdrantVectorIndexAdapter.class);
            assertThat(context.getBean(VectorIndexAdapter.class).isEnabled()).isFalse();
        });
    }

    @Test
    void createsQdrantAdapterOnlyWhenExplicitlyEnabledAndConfigured() {
        contextRunner
                .withPropertyValues(
                        "learning-os.rag.vector.enabled=true",
                        "learning-os.rag.vector.provider=qdrant",
                        "learning-os.rag.vector.qdrant.host=localhost",
                        "learning-os.rag.vector.qdrant.port=6334",
                        "learning-os.rag.vector.qdrant.collection-name=learning_os_chunks"
                )
                .withBean(QdrantVectorOperations.class, FakeOperations::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(VectorIndexAdapter.class);
                    assertThat(context).hasSingleBean(QdrantVectorIndexAdapter.class);
                    assertThat(context).doesNotHaveBean(NoopVectorIndexAdapter.class);
                    assertThat(context.getBean(VectorIndexAdapter.class).isEnabled()).isTrue();
                });
    }

    @Test
    void failsFastWhenEnabledQdrantConfigIsIncomplete() {
        contextRunner
                .withPropertyValues(
                        "learning-os.rag.vector.enabled=true",
                        "learning-os.rag.vector.provider=qdrant",
                        "learning-os.rag.vector.qdrant.host=",
                        "learning-os.rag.vector.qdrant.collection-name=learning_os_chunks"
                )
                .withBean(QdrantVectorOperations.class, FakeOperations::new)
                .run(context -> assertThat(context).hasFailed());
    }

    static class FakeOperations implements QdrantVectorOperations {
        @Override
        public void deleteDocument(QdrantVectorDeleteCommand command) {
        }

        @Override
        public void upsert(QdrantVectorUpsertCommand command) {
        }

        @Override
        public java.util.List<QdrantVectorSearchHit> search(QdrantVectorSearchCommand command) {
            return java.util.List.of();
        }
    }
}
