package com.learningos.agent.api;

import com.learningos.agent.repository.ModelProviderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "learning-os.auth.jwt-secret=unit-test-secret-32-bytes-long!!",
        "learning-os.auth.issuer=learning-os",
        "learning-os.model-provider.encryption-key=test-model-provider-key"
})
class ModelProviderControllerTest {

    private static final String AUTH_SECRET = "unit-test-secret-32-bytes-long!!";
    private static final String AUTH_ISSUER = "learning-os";

    private final MockMvc mockMvc;
    private final ModelProviderRepository modelProviderRepository;

    ModelProviderControllerTest(MockMvc mockMvc, ModelProviderRepository modelProviderRepository) {
        this.mockMvc = mockMvc;
        this.modelProviderRepository = modelProviderRepository;
    }

    @Test
    void adminCanCreateListAndUpdateModelProvidersWithoutReturningFullApiKey() throws Exception {
        mockMvc.perform(post("/api/admin/model-providers")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerCode": "deepseek",
                                  "displayName": "DeepSeek",
                                  "remark": "Company account",
                                  "websiteUrl": "https://platform.deepseek.com",
                                  "baseUrl": "https://api.deepseek.com",
                                  "chatModel": "deepseek-chat",
                                  "apiKey": "sk-test-provider-key",
                                  "enabled": true,
                                  "defaultProvider": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerCode").value("deepseek"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-***ey"))
                .andExpect(jsonPath("$.data.defaultProvider").value(true))
                .andExpect(jsonPath("$.data.apiKey").doesNotExist());

        String providerId = modelProviderRepository.findByProviderCode("deepseek").orElseThrow().getId();

        mockMvc.perform(get("/api/admin/model-providers")
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].displayName").value("DeepSeek"));

        mockMvc.perform(put("/api/admin/model-providers/{id}", providerId)
                        .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerCode": "deepseek",
                                  "displayName": "DeepSeek Updated",
                                  "remark": "Updated note",
                                  "websiteUrl": "https://platform.deepseek.com",
                                  "baseUrl": "https://api.deepseek.com",
                                  "chatModel": "deepseek-chat",
                                  "apiKey": "sk-***ey",
                                  "enabled": true,
                                  "defaultProvider": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("DeepSeek Updated"));

        assertThat(modelProviderRepository.findById(providerId).orElseThrow().getDisplayName())
                .isEqualTo("DeepSeek Updated");
    }

    @Test
    void studentCannotManageModelProviders() throws Exception {
        mockMvc.perform(get("/api/admin/model-providers")
                        .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private String jwt(String subject, String name, List<String> roles) throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("""
                {
                  "sub":"%s",
                  "name":"%s",
                  "roles":%s,
                  "iss":"%s",
                  "iat":%d,
                  "exp":%d
                }
                """.formatted(
                subject,
                name,
                roles.stream().map(role -> "\"" + role + "\"").collect(Collectors.joining(",", "[", "]")),
                AUTH_ISSUER,
                now,
                now + 3600
        ).replaceAll("\\s+", ""));
        String unsigned = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(AUTH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        return unsigned + "." + signature;
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
