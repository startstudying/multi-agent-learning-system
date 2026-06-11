# Data Model

The main database is MySQL 8. Flyway migrations live in `backend/src/main/resources/db/migration`.

## RAG Tables

Implemented in `V1__rag_foundation.sql`:

- `kb_knowledge_base`: KB metadata, visibility, owner, soft delete.
- `kb_permission`: user/role/class/department access to KBs.
- `kb_document`: source document metadata, MinIO location, parse/index status, version.
- `kb_doc_chunk`: chunk text and retrieval metadata. `kb_id` is redundant by design for permission-filtered retrieval.
- `kb_index_task`: async indexing status and retry/error metadata.
- `kb_chat_session`: chat session scope and activity metadata.
- `kb_chat_message`: user/assistant messages, source summaries, token/latency metadata.
- `kb_query_log`: trace id, user id, KB ids, question, retrieval count, sources, latency.

Also implemented in `V2__learning_agent_loop.sql`:

- `source_citation`: normalized citation records linked to query/chat/resource outputs.

Planned:

- Optional vector-store tables only if MySQL is selected for vector storage. The preferred design is a separate VectorDB adapter.

## Learning Tables

Implemented in `V2__learning_agent_loop.sql`:

- `app_user`, `role`, `user_role`
- `learner_profile`
- `learning_goal`
- `learning_event`
- `course`, `chapter`, `knowledge_point`, `knowledge_dependency`
- `learning_path`, `learning_path_node`
- `mastery_record`

`LearningWorkflowService` currently persists learner profiles, learning events, learning paths, path nodes, and mastery records where applicable.

## Resource And Agent Tables

Implemented:

- `resource_generation_task`
- `learning_resource`
- `resource_review`
- `agent_task`
- `agent_trace`
- `agent_tool_call`
- `prompt_version`
- `model_call_log`
- `token_usage_log`
- `learning_effect_report`

`ResourceGenerationService` currently persists generation tasks, resource drafts, critic review rows, agent tasks, ordered trace rows, model-call logs, and token-usage logs.

## Assessment Tables

Implemented:

- `question`
- `rubric`
- `answer_record`
- `grading_result`
- `wrong_question`

`AssessmentService` currently persists submitted answers, grading results, mastery updates, wrong-question records, and assessment learning events.

## Cross-Cutting Rules

- Agent calls must create `agent_task` and `agent_trace`.
- Model calls must create `model_call_log` and `token_usage_log`.
- RAG retrieval must hard-filter by allowed KB ids before chunk lookup.
- Generated resources stay `PENDING_CRITIC` until critic/teacher review approves release.
- Frontend clients consume typed API DTOs and never access the database or model providers directly.
