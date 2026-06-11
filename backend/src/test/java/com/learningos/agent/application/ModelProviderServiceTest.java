package com.learningos.agent.application;

import com.learningos.agent.domain.ModelProvider;
import com.learningos.agent.dto.ModelProviderUpsertRequest;
import com.learningos.agent.repository.ModelProviderRepository;
import com.learningos.common.exception.ApiException;
import com.learningos.config.AppProperties;
import com.learningos.config.AuthProperties;
import com.learningos.config.ModelProviderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelProviderServiceTest {

    @Mock
    private ModelProviderRepository modelProviderRepository;

    private ModelProviderService service;

    @BeforeEach
    void setUp() {
        ModelProviderSecretCodec codec = new ModelProviderSecretCodec(
                new ModelProviderProperties("test-encryption-key"),
                new AuthProperties("", "learning-os", "", "", true),
                new AppProperties("dev", "X-Trace-Id")
        );
        service = new ModelProviderService(modelProviderRepository, codec, new OpenAiCompatibleChatModelFactory());
    }

    @Test
    void createsDefaultProviderAndMasksApiKeyInResponse() {
        when(modelProviderRepository.findByProviderCode("deepseek")).thenReturn(Optional.empty());
        when(modelProviderRepository.save(any(ModelProvider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(deepseekRequest(true), true, "ops_admin");

        assertThat(response.providerCode()).isEqualTo("deepseek");
        assertThat(response.apiKeyConfigured()).isTrue();
        assertThat(response.apiKeyMasked()).isEqualTo("sk-***ey");
        assertThat(response.defaultProvider()).isTrue();
        verify(modelProviderRepository).clearDefaultProviders();
    }

    @Test
    void resolvesDefaultChatProviderForGateway() {
        ModelProvider provider = savedProvider();
        when(modelProviderRepository.findFirstByEnabledTrueAndDefaultProviderTrueOrderByUpdatedAtDesc())
                .thenReturn(Optional.of(provider));

        Optional<ModelProviderService.ResolvedChatProvider> resolved = service.resolveDefaultChatProvider();

        assertThat(resolved).isPresent();
        assertThat(resolved.get().providerCode()).isEqualTo("deepseek");
        assertThat(resolved.get().chatModel()).isEqualTo("deepseek-chat");
        assertThat(resolved.get().apiKey()).isEqualTo("sk-registry-key");
    }

    @Test
    void deniesNonAdminAccess() {
        assertThatThrownBy(() -> service.list(false))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void testConnectionUsesConfiguredChatModel() {
        ModelProvider provider = savedProvider();
        when(modelProviderRepository.findById("mdl_test")).thenReturn(Optional.of(provider));
        ModelProviderService testService = new ModelProviderService(
                modelProviderRepository,
                new ModelProviderSecretCodec(
                        new ModelProviderProperties("test-encryption-key"),
                        new AuthProperties("", "learning-os", "", "", true),
                        new AppProperties("dev", "X-Trace-Id")
                ),
                (baseUrl, apiKey, model) -> prompt -> new ChatResponse(
                        List.of(new Generation(new AssistantMessage("pong")))
                )
        );

        var result = testService.testConnection("mdl_test", true);

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.providerCode()).isEqualTo("deepseek");
    }

    @Test
    void updateKeepsExistingApiKeyWhenMaskedPlaceholderProvided() {
        ModelProvider provider = savedProvider();
        when(modelProviderRepository.findById("mdl_test")).thenReturn(Optional.of(provider));
        when(modelProviderRepository.save(any(ModelProvider.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ModelProvider> captor = ArgumentCaptor.forClass(ModelProvider.class);

        service.update(
                "mdl_test",
                new ModelProviderUpsertRequest(
                        "deepseek",
                        "DeepSeek Updated",
                        "note",
                        "https://platform.deepseek.com",
                        "https://api.deepseek.com",
                        "deepseek-chat",
                        null,
                        "sk-***ey",
                        true,
                        true
                ),
                true
        );

        verify(modelProviderRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayName()).isEqualTo("DeepSeek Updated");
        assertThat(captor.getValue().getApiKeyCiphertext()).isEqualTo(provider.getApiKeyCiphertext());
    }

    private ModelProvider savedProvider() {
        ModelProviderSecretCodec codec = new ModelProviderSecretCodec(
                new ModelProviderProperties("test-encryption-key"),
                new AuthProperties("", "learning-os", "", "", true),
                new AppProperties("dev", "X-Trace-Id")
        );
        ModelProvider provider = new ModelProvider();
        provider.setId("mdl_test");
        provider.setProviderCode("deepseek");
        provider.setDisplayName("DeepSeek");
        provider.setBaseUrl("https://api.deepseek.com");
        provider.setChatModel("deepseek-chat");
        provider.setApiKeyCiphertext(codec.encrypt("sk-registry-key"));
        provider.setEnabled(true);
        provider.setDefaultProvider(true);
        return provider;
    }

    private ModelProviderUpsertRequest deepseekRequest(boolean defaultProvider) {
        return new ModelProviderUpsertRequest(
                "deepseek",
                "DeepSeek",
                "Company account",
                "https://platform.deepseek.com",
                "https://api.deepseek.com",
                "deepseek-chat",
                null,
                "sk-registry-key",
                true,
                defaultProvider
        );
    }
}
