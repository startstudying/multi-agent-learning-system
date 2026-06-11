package com.learningos.agent.application;

import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import com.learningos.config.ModelProviderProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderSecretCodecTest {

    @Test
    void encryptsAndDecryptsApiKeys() {
        ModelProviderSecretCodec codec = new ModelProviderSecretCodec(
                new ModelProviderProperties("test-encryption-key"),
                new AuthProperties("", "learning-os", "", "", true),
                new AppProperties("dev", "X-Trace-Id")
        );

        String encrypted = codec.encrypt("sk-test-secret-value");
        String decrypted = codec.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo("sk-test-secret-value");
        assertThat(decrypted).isEqualTo("sk-test-secret-value");
    }

    @Test
    void masksApiKeysWithoutLeakingFullValue() {
        assertThat(ModelProviderSecretCodec.maskApiKey("sk-0a2c80edde1146b6a92f07821ce8f5eb"))
                .isEqualTo("sk-***eb");
        assertThat(ModelProviderSecretCodec.maskApiKey("short")).isEqualTo("***");
    }
}
