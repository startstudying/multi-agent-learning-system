package com.learningos.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaConvergenceMigrationTest {

    @Test
    void v1ToV5MigrationsContainMysqlOnlyDialectConstructsNotExercisedByH2Profile() throws IOException {
        String v1 = migrationText("db/migration/V1__rag_foundation.sql");
        String v3 = migrationText("db/migration/V3__schema_convergence_indexes.sql");
        String v4 = migrationText("db/migration/V4__resource_generation_idempotency.sql");
        String v5 = migrationText("db/migration/V5__assessment_answer_idempotency.sql");
        String testProfile = migrationText("application-test.yml");

        assertThat(v1)
                .contains("engine=InnoDB")
                .contains("collate=utf8mb4_0900_ai_ci")
                .contains("kb_ids_json text")
                .contains("question text")
                .contains("sources_json text");
        assertThat(v3)
                .contains("DELIMITER //")
                .contains("CREATE PROCEDURE add_index_if_missing")
                .contains("information_schema.statistics")
                .contains("DATABASE()")
                .contains("PREPARE stmt FROM @ddl");
        assertThat(v4)
                .contains("CREATE PROCEDURE add_column_if_missing")
                .contains("PREPARE stmt FROM @ddl");
        assertThat(v5)
                .contains("CREATE PROCEDURE add_column_if_missing")
                .contains("CREATE UNIQUE INDEX uk_answer_learner_request");
        assertThat(testProfile)
                .contains("jdbc:h2:mem:learning_os_test;MODE=MySQL")
                .contains("flyway:")
                .contains("enabled: false");
    }

    @Test
    void v3MigrationAddsMissingIndexesForV1ToV2SchemaConvergence() throws IOException {
        String migration = migrationText("db/migration/V3__schema_convergence_indexes.sql");

        assertThat(migration)
                .contains("DROP PROCEDURE IF EXISTS add_index_if_missing")
                .contains("idx_agent_task_owner")
                .contains("idx_agent_task_trace")
                .contains("idx_model_call_task")
                .contains("idx_token_usage_task")
                .contains("idx_answer_learner_created")
                .contains("idx_wrong_question_kp")
                .contains("idx_rgt_agent_task");
    }

    @Test
    void v5MigrationAddsAnswerSubmissionIdempotencyColumnsAndConstraint() throws IOException {
        String migration = migrationText("db/migration/V5__assessment_answer_idempotency.sql");

        assertThat(migration)
                .contains("request_id")
                .contains("request_hash")
                .contains("response_json")
                .contains("uk_answer_learner_request")
                .contains("answer_record(learner_id, request_id)");
    }

    @Test
    void v6MigrationAddsStructuredResourceReviewGovernanceColumns() throws IOException {
        String migration = migrationText("db/migration/V6__resource_review_governance.sql");

        assertThat(migration)
                .contains("reason")
                .contains("citation_check")
                .contains("safety_check")
                .contains("revision_suggestion")
                .contains("resource_review");
    }

    @Test
    void v7MigrationAddsRagQueryReplaySnapshotColumnsAndConstraint() throws IOException {
        String migration = migrationText("db/migration/V7__rag_query_replay_snapshot.sql");

        assertThat(migration)
                .contains("request_id")
                .contains("request_hash")
                .contains("response_json")
                .contains("uk_kb_query_user_request")
                .contains("kb_query_log(user_id, request_id)");
    }

    @Test
    void v8MigrationAddsDocumentUploadIdempotencyColumnsAndConstraint() throws IOException {
        String migration = migrationText("db/migration/V8__rag_document_upload_idempotency.sql");

        assertThat(migration)
                .contains("request_id")
                .contains("request_hash")
                .contains("response_json")
                .contains("uk_kb_document_user_request")
                .contains("kb_document(created_by, request_id)");
    }

    @Test
    void v9MigrationAddsLearningPathNodeRecommendationMetadataColumns() throws IOException {
        String migration = migrationText("db/migration/V9__learning_path_node_recommendation_metadata.sql");

        assertThat(migration)
                .contains("learning_path_node")
                .contains("recommendation_reason")
                .contains("estimated_duration_minutes")
                .contains("resource_type")
                .contains("assessment_binding_relation");
    }

    @Test
    void v10MigrationAddsProfileSnapshotContextColumns() throws IOException {
        String migration = migrationText("db/migration/V10__profile_snapshot_context.sql");

        assertThat(migration)
                .contains("learning_path")
                .contains("resource_generation_task")
                .contains("profile_snapshot");
    }

    @Test
    void v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask() throws IOException {
        String migration = migrationText("db/migration/V11__resource_generation_task_recovery_state.sql");

        assertThat(migration)
                .contains("resource_generation_task")
                .contains("retry_count")
                .contains("next_retry_at")
                .contains("last_error")
                .contains("recoverable");
    }

    @Test
    void v12MigrationAddsModelCallPromptMetadataColumns() throws IOException {
        String migration = migrationText("db/migration/V12__model_call_prompt_metadata.sql");

        assertThat(migration)
                .contains("model_call_log")
                .contains("prompt_code")
                .contains("prompt_version")
                .contains("temperature")
                .contains("structured_output_schema");
    }

    @Test
    void v13MigrationAddsEvaluationSetManagementTables() throws IOException {
        String migration = migrationText("db/migration/V13__evaluation_set_management.sql");

        assertThat(migration)
                .contains("evaluation_set")
                .contains("evaluation_sample")
                .contains("set_type")
                .contains("RAG_QUESTION")
                .contains("GRADING_SAMPLE")
                .contains("RESOURCE_GENERATION_SAMPLE")
                .contains("input_json")
                .contains("expected_json")
                .contains("metadata_json")
                .contains("uk_evaluation_set_owner_code_version")
                .contains("idx_evaluation_sample_set");
    }

    @Test
    void v14MigrationAddsEvaluationRunQualityMetricTables() throws IOException {
        String migration = migrationText("db/migration/V14__evaluation_run_quality_metrics.sql");

        assertThat(migration)
                .contains("evaluation_run")
                .contains("evaluation_run_metric")
                .contains("evaluation_set_id")
                .contains("prompt_code")
                .contains("prompt_version")
                .contains("run_status")
                .contains("metric_name")
                .contains("metric_value")
                .contains("idx_evaluation_run_set_prompt")
                .contains("idx_evaluation_run_metric_run")
                .contains("uk_evaluation_run_metric_run_name")
                .contains("ck_evaluation_run_status")
                .contains("ck_evaluation_run_succeeded_sample_count")
                .contains("ck_evaluation_run_metric_sample_count");
    }

    @Test
    void v15MigrationAddsAgentToolCallTraceGovernanceColumns() throws IOException {
        String migration = migrationText("db/migration/V15__agent_tool_call_trace_governance.sql");

        assertThat(migration)
                .contains("agent_tool_call")
                .contains("trace_id")
                .contains("input_summary")
                .contains("output_summary")
                .contains("retention_class")
                .contains("idx_agent_tool_call_trace")
                .contains("idx_agent_tool_call_status");
    }

    @Test
    void v16MigrationAddsRagIndexTaskWorkerProgressColumnsAndIndexes() throws IOException {
        String migration = migrationText("db/migration/V16__rag_index_task_worker_progress.sql");

        assertThat(migration)
                .contains("kb_index_task")
                .contains("progress_percent")
                .contains("progress_phase")
                .contains("heartbeat_at")
                .contains("lease_owner")
                .contains("lease_until")
                .contains("next_retry_at")
                .contains("recoverable")
                .contains("idx_kb_index_task_due")
                .contains("idx_kb_index_task_lease");
    }

    @Test
    void v17MigrationAddsRagChunkProductionMetadataColumnsAndIndexes() throws IOException {
        String migration = migrationText("db/migration/V17__rag_chunk_production_metadata.sql");

        assertThat(migration)
                .contains("kb_doc_chunk")
                .contains("chunk_hash")
                .contains("uk_kb_doc_chunk_document_version_hash")
                .contains("idx_kb_doc_chunk_document_version_hash")
                .contains("document_id, document_version, chunk_hash");
    }

    @Test
    void v18MigrationAddsProviderColumnToModelCallLog() throws IOException {
        String migration = migrationText("db/migration/V18__model_call_provider_observability.sql");

        assertThat(migration)
                .contains("add_column_if_missing")
                .contains("model_call_log")
                .contains("provider")
                .contains("ADD COLUMN provider varchar(80) NOT NULL DEFAULT ''none''");
    }

    @Test
    void v19MigrationAddsCourseEnrollmentScopeTable() throws IOException {
        String migration = migrationText("db/migration/V19__course_enrollment_scope.sql");

        assertThat(migration)
                .contains("course_enrollment")
                .contains("course_id varchar(80) not null")
                .contains("learner_id varchar(120) not null")
                .contains("status varchar(40) not null default 'ACTIVE'")
                .contains("uk_course_enrollment_course_learner")
                .contains("idx_course_enrollment_learner_status")
                .contains("idx_course_enrollment_course_status");
    }

    @Test
    void v20MigrationAddsKbCourseBindingGovernanceColumns() throws IOException {
        String migration = migrationText("db/migration/V20__kb_course_binding_governance.sql");

        assertThat(migration)
                .contains("kb_knowledge_base")
                .contains("course_id varchar(80)")
                .contains("binding_status varchar(40)")
                .contains("bound_by varchar(120)")
                .contains("bound_at datetime(6)")
                .contains("idx_kb_course_binding")
                .contains("idx_kb_document_kb_course_deleted")
                .contains("ck_kb_binding_status")
                .contains("ck_kb_binding_course_consistency")
                .contains("fk_kb_course_binding_course")
                .contains("BOUND")
                .contains("UNBOUND")
                .contains("CONFLICTED");
    }

    @Test
    void v21MigrationAddsModelProviderRegistryTable() throws IOException {
        String migration = migrationText("db/migration/V21__model_provider_registry.sql");

        assertThat(migration)
                .contains("model_provider")
                .contains("provider_code varchar(80)")
                .contains("display_name varchar(120)")
                .contains("api_key_ciphertext varchar(1024)")
                .contains("uk_model_provider_code")
                .contains("idx_model_provider_default_enabled");
    }

    @Test
    void v22MigrationAddsOpsAlertPersistenceTable() throws IOException {
        String migration = migrationText("db/migration/V22__ops_alert_persistence.sql");

        assertThat(migration)
                .contains("ops_alert_record")
                .contains("alert_type")
                .contains("notification_status")
                .contains("uk_ops_alert_window")
                .contains("idx_ops_alert_status");
    }

    private String migrationText(String resourcePath) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(stream).as(resourcePath + " exists").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
