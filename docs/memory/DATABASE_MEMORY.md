# DATABASE_MEMORY.md

## Database

- MySQL 8.x
- Migrations via Flyway or Liquibase

## Schema Conventions

- Table names: snake_case, plural (e.g. `learning_paths`)
- Primary key: `id` (BIGINT AUTO_INCREMENT or UUID)
- Timestamps: `created_at`, `updated_at`
- Soft delete: `deleted_at` where applicable
- Status fields: ENUM or VARCHAR with documented state machine

## Core Tables (baseline)

| Table | Purpose | Status |
|---|---|---|
| users | User accounts | existing |
| learner_profiles | Learning profile dimensions | planned |
| learning_paths | Personalized path nodes | existing |
| knowledge_documents | RAG document metadata | existing |
| agent_traces | Agent execution audit | planned |
| generated_resources | AI-generated content with review status | planned |

## Rules

- Schema changes require SPEC update first.
- Index strategy documented in SPEC.
- No raw SQL in application code without review.
- P3-4 minimum permission/security hardening added no schema changes; full RBAC may require future permission tables or migrations with separate SPEC.
- Real MySQL migration smoke is opt-in via `MysqlMigrationSmokeTest` / `scripts/mysql-migration-smoke.ps1`; normal H2 tests keep Flyway disabled and do not prove MySQL-only SQL compatibility.
- Current real MySQL smoke has been verified through Flyway V1-V17; V18 has convergence/smoke test assertions but fresh real MySQL execution is blocked locally by `root` credential mismatch.
- RAG index lease recovery uses indexed `kb_index_task(status, lease_until)` lookup and treats `lease_until < now` as expired.
- RAG parser adapter minimal (`2026-06-07`) added no schema changes and no migration; it only moved parsing logic into `rag/parser`.

## Prompt Version Table

Related SPEC: `docs/specs/SPEC-20260605-prompt-version-management.md`

The existing `V2__learning_agent_loop.sql` `prompt_version` table is now mapped by JPA through `PromptVersion`.
No new migration was added. The current table columns are `id`, `code`, `version`, `prompt_text`, `status`, and `created_at`.

## Migration History

| Date | Migration | Related SPEC |
|---|---|---|
| 2026-06-08 | `V19__course_enrollment_scope.sql` adds `course_enrollment` with `(course_id, learner_id)` uniqueness, `status`, active lookup indexes by learner/status and course/status. This is the authorization source for P3-4-D course enrollment scope. Static migration and full backend Maven verification passed; real MySQL smoke remains environment-dependent. | `docs/specs/SPEC-20260608-course-enrollment-scope.md` |
| 2026-06-06 | `V1__rag_foundation.sql` stores `kb_query_log.kb_ids_json`, `question`, and `sources_json` as `text` to avoid MySQL 8 `ERROR 1118 Row size too large` under `utf8mb4`; entity mapping uses `columnDefinition = "text"`. | `docs/specs/SPEC-20260606-mysql-migration-smoke.md` |
| 2026-06-06 | `V6__resource_review_governance.sql` adds `reason`, `citation_check`, `safety_check`, and `revision_suggestion` to `resource_review`. | `docs/specs/SPEC-20260606-review-gate-state-model.md` |
| 2026-06-06 | `V7__rag_query_replay_snapshot.sql` adds `request_id`, `request_hash`, `response_json`, and `uk_kb_query_user_request(user_id, request_id)` to `kb_query_log`. | `docs/specs/SPEC-20260606-rag-query-replay-snapshot.md` |
| 2026-06-06 | `V8__rag_document_upload_idempotency.sql` adds `request_id`, `request_hash`, `response_json`, and `uk_kb_document_user_request(created_by, request_id)` to `kb_document`. | `docs/specs/SPEC-20260606-rag-document-upload-idempotency.md` |
| 2026-06-06 | `V9__learning_path_node_recommendation_metadata.sql` adds recommendation metadata columns to `learning_path_node`. | `docs/specs/SPEC-20260606-learning-path-node-recommendation-metadata.md` |
| 2026-06-06 | `V10__profile_snapshot_context.sql` adds `profile_snapshot` to `learning_path` and `resource_generation_task`. | `docs/specs/SPEC-20260606-learner-profile-snapshot-context.md` |
| 2026-06-06 | `V11__resource_generation_task_recovery_state.sql` adds `retry_count`, `next_retry_at`, `last_error`, and `recoverable` to `resource_generation_task`. | `docs/specs/SPEC-20260606-resource-generation-recovery-state.md` |
| 2026-06-06 | `V12__model_call_prompt_metadata.sql` adds `prompt_code`, `prompt_version`, `temperature`, and `structured_output_schema` to `model_call_log`. | `docs/specs/SPEC-20260606-model-call-prompt-metadata.md` |
| 2026-06-06 | `V13__evaluation_set_management.sql` adds `evaluation_set` and `evaluation_sample` for persistent RAG, grading, and resource-generation evaluation samples. | `docs/specs/SPEC-20260606-evaluation-set-management.md` |
| 2026-06-06 | `V14__evaluation_run_quality_metrics.sql` adds `evaluation_run` and `evaluation_run_metric`, including status checks, positive succeeded-run sample count, positive metric sample count, and `uk_evaluation_run_metric_run_name(run_id, metric_name)`. | `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md` |
| 2026-06-06 | `V15__agent_tool_call_trace_governance.sql` adds `trace_id`, `input_summary`, `output_summary`, `retention_class`, and trace/status indexes to `agent_tool_call`. | `docs/specs/SPEC-20260606-agent-trace-governance-dashboard.md` |
| 2026-06-06 | `V16__rag_index_task_worker_progress.sql` adds progress, phase, heartbeat, lease owner, lease expiry, next retry, recoverable state, and due/lease indexes to `kb_index_task`. | `docs/specs/SPEC-20260606-rag-index-worker-progress.md` |
| 2026-06-06 | `V17__rag_chunk_production_metadata.sql` adds `chunk_hash` and `(document_id, document_version, chunk_hash)` unique/query indexes to `kb_doc_chunk`. | `docs/specs/SPEC-20260606-rag-chunk-production-metadata.md` |
| 2026-06-08 | `V18__model_call_provider_observability.sql` adds `provider varchar(80) not null default 'none'` to `model_call_log`; values are low-cardinality normalized by gateway/recorder and no index is added until real provider aggregation queries justify it. | `docs/specs/SPEC-20260608-model-call-provider-observability.md` |
