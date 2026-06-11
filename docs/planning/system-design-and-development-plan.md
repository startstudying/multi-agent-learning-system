# System Design And Development Plan

## Requirement Alignment

| Requirement | Current Status | Evidence |
| --- | --- | --- |
| Vue 3 TypeScript frontend | Implemented foundation | `frontend/src`, Vue Router routes, typed API clients |
| Java 21 Spring Boot backend | Implemented foundation | `backend/pom.xml`, Spring Boot 3.5, Java 21 |
| MySQL 8 primary DB | Implemented config/migrations | `application.yml`, Compose MySQL, Flyway MySQL migrations |
| Course RAG | Partial | KB/document/query/SSE contracts exist; real parser/embedding/reranker pending |
| Personalized learner model | Partial | profile extraction and persistence exist; real LLM extraction pending |
| Learning path | Partial | path generation and persistence exist; graph-driven planning pending |
| Multi-agent resource generation | Partial | task/resource/review/trace persistence exists; real model orchestration pending |
| Assessment feedback loop | Partial | answer/grading/mastery/wrong-question persistence exists; rubric/model grading pending |
| Teacher/critic governance | UI + persistence foundation | resource reviews and review controller exist; richer review workflow pending |
| Observability and cost | Partial | trace/model/token logs exist for resource generation; dashboards pending |

## Research Mapping

| Theme | How It Maps To The Product |
| --- | --- |
| LLM personalized learning | Learner profile extraction, adaptive path generation, mastery updates |
| Education RAG | Course-grounded answers, citation viewer, source-aware resource generation |
| Multi-agent education systems | Profile, diagnosis, planner, RAG, resource, assessment, critic, tutor agents |
| Automated feedback | Assessment grading, wrong-cause diagnosis, resource push strategy |
| Trustworthy AI | Permission filtering, citations, teacher review, trace/cost logs |

## OSS Benchmark Mapping

| OSS Class | Useful Benchmark |
| --- | --- |
| LMS systems such as Moodle/Open edX/Frappe LMS | Course, learner, assessment, and permission data concepts |
| RAG examples such as Adaptive-RAG | Retrieval evaluation, routing, reranking ideas |
| Tutor/Study assistant repos | Chat UX, learning assistant flows, lightweight agent decomposition |

Use OSS projects for comparison and test ideas, not direct copying.

## Backend Modules

- `common`: response envelope, errors, trace filter, dev auth.
- `health`: dependency/model configuration health.
- `rag`: KBs, permissions, documents, indexing tasks, chunks, query, SSE.
- `learning`: profile extraction, learning events, learning paths, mastery records.
- `agent`: resource generation, critic review state, agent tasks/traces/model/token logs.
- `assessment`: answers, grading, mastery updates, wrong-question diagnosis.
- `safety`: input and draft safety checks.

## Frontend Pages

- Student loop: knowledge base/document state, RAG answer, citations, learning path, generated resources, trace, assessment.
- Teacher review: critic/teacher release queue and rubric checklist.
- Admin operations: dependency health, model-call/token counts, learning activity counts, and resource review status counts. Trace coverage, model cost, citation rate, and index-health APIs remain future work.

## API And Streaming

- REST for KB/document/RAG/profile/path/resource/trace/assessment commands.
- SSE for chat stream status/token/done events.
- Frontend API clients use typed DTOs under `frontend/src/api` and `frontend/src/types/api.ts`.
- Backend model access is server-side only.

## MVP Definition

The MVP is a working closed loop:

1. Create or select a knowledge base.
2. Upload course material and create index tasks.
3. Ask a RAG question and receive citations.
4. Extract a learner profile from dialogue.
5. Generate a learning path.
6. Generate critic-gated resource drafts.
7. Inspect agent trace.
8. Submit an answer.
9. Persist grading, mastery updates, wrong-question diagnosis, and replan evidence.
10. Show the loop in the Vue workbench.

## Acceptance Standards

- `mvn test` passes.
- `docker compose config` passes.
- `npm test -- --run` passes.
- `npm run build` passes.
- No PostgreSQL runtime config is reintroduced as the primary path.
- RAG permission filtering happens before retrieval.
- Agent/resource generation writes `agent_task` and `agent_trace`.
- Model call paths write token/cost/latency/status evidence when real model calls are introduced.
- Generated resources require critic/teacher review before learner release.

## Phase Prompts

1. "Implement real document parser/chunker/index workers for PDF, DOCX, Markdown, and TXT while preserving the existing RAG API contracts."
2. "Add Spring AI/Spring AI Alibaba chat and embedding adapters behind service interfaces, with model_call_log and token_usage_log persistence."
3. "Introduce optional VectorDB retrieval behind a repository-neutral adapter and keep MySQL as the primary store."
4. "Implement hybrid retrieval, RRF, reranker timeout fallback, and RAG evaluation tests for citation quality and permission leaks."
5. "Build teacher review backend APIs for resource approval/rejection and wire the teacher Vue page to them."
6. "Replace deterministic assessment grading with rubric-backed LLM grading and persisted feedback records."
7. "Add admin telemetry APIs for trace coverage, model cost, citation rate, index health, and learning outcomes."
8. "Harden auth with Sa-Token/JWT or session auth, replacing dev headers and adding SSE-safe authentication."
