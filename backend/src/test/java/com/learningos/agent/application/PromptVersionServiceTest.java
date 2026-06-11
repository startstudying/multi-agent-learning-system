package com.learningos.agent.application;

import com.learningos.agent.dto.PromptVersionUpsertRequest;
import com.learningos.agent.repository.PromptVersionRepository;
import com.learningos.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(PromptVersionService.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PromptVersionServiceTest {

    private final PromptVersionService promptVersionService;
    private final PromptVersionRepository promptVersionRepository;

    PromptVersionServiceTest(
            PromptVersionService promptVersionService,
            PromptVersionRepository promptVersionRepository
    ) {
        this.promptVersionService = promptVersionService;
        this.promptVersionRepository = promptVersionRepository;
    }

    @Test
    void upsertsPromptVersionAndFindsItByCodeAndVersion() {
        var saved = promptVersionService.upsert(adminRequest(
                "resource-generation",
                "v1",
                "Generate resources for {{learnerId}}.",
                "active"
        ), true);

        assertThat(saved.id()).startsWith("prv_");
        assertThat(saved.code()).isEqualTo("resource-generation");
        assertThat(saved.version()).isEqualTo("v1");
        assertThat(saved.promptText()).isEqualTo("Generate resources for {{learnerId}}.");
        assertThat(saved.status()).isEqualTo("ACTIVE");
        assertThat(saved.createdAt()).isNotNull();

        var found = promptVersionService.get("resource-generation", "v1", true, false);

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.promptText()).isEqualTo(saved.promptText());
    }

    @Test
    void duplicateUpsertUpdatesExistingPromptVersionWithoutCreatingDuplicateRows() {
        var first = promptVersionService.upsert(adminRequest(
                "critic-review",
                "v1",
                "Review resource draft.",
                "DRAFT"
        ), true);

        var updated = promptVersionService.upsert(adminRequest(
                "critic-review",
                "v1",
                "Review resource draft with citation checks.",
                "ACTIVE"
        ), true);

        assertThat(updated.id()).isEqualTo(first.id());
        assertThat(updated.promptText()).isEqualTo("Review resource draft with citation checks.");
        assertThat(updated.status()).isEqualTo("ACTIVE");
        assertThat(promptVersionRepository.count()).isEqualTo(1);
    }

    @Test
    void roleAwareReadsHidePromptTextForTeacherMetadataAccess() {
        promptVersionService.upsert(adminRequest(
                "rag-answer",
                "v1",
                "Internal prompt text",
                "ACTIVE"
        ), true);

        var adminView = promptVersionService.get("rag-answer", "v1", true, false);
        var teacherView = promptVersionService.get("rag-answer", "v1", false, true);

        assertThat(adminView.promptText()).isEqualTo("Internal prompt text");
        assertThat(teacherView.promptText()).isNull();
    }

    @Test
    void listsByCodeAndFindsActiveVersionForModelCallLinkage() {
        promptVersionService.upsert(adminRequest(
                "rag-answer",
                "v1",
                "Answer with citations.",
                "ARCHIVED"
        ), true);
        var active = promptVersionService.upsert(adminRequest(
                "rag-answer",
                "v2",
                "Answer with citations and no-source refusal.",
                "ACTIVE"
        ), true);

        assertThat(promptVersionService.list("rag-answer", true, false))
                .extracting("version")
                .containsExactly("v1", "v2");
        assertThat(promptVersionService.findActiveByCode("rag-answer"))
                .hasValueSatisfying(found -> assertThat(found.id()).isEqualTo(active.id()));
    }

    @Test
    void getRejectsMissingPromptVersion() {
        assertThatThrownBy(() -> promptVersionService.get("missing", "v1", true, false))
                .isInstanceOf(ApiException.class)
                .hasMessage("Prompt version not found");
    }

    private PromptVersionUpsertRequest adminRequest(
            String code,
            String version,
            String promptText,
            String status
    ) {
        return new PromptVersionUpsertRequest(code, version, promptText, status);
    }
}
