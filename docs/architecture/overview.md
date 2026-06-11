# Architecture Overview

AI Learning OS is a modular monolith for personalized learning. The first production boundary is the backend service layer, not microservices. Agents and tools never access repositories directly; application services own persistence, authorization, caching, and business rules.

## Runtime Stack

- Frontend: Vue 3, TypeScript, Vite, Vue Router, lucide icons.
- Backend: Java 21, Spring Boot 3.x, Spring Web, Validation, Data JPA, Flyway, Spring AI dependency management.
- Primary database: MySQL 8.
- Optional infrastructure: Redis 7 for cache/progress, MinIO for source documents, and an external VectorDB when semantic retrieval is enabled.
- Local services: `backend/docker-compose.yml` starts MySQL 8, Redis, and MinIO.

PostgreSQL is not the primary database path. If pgvector is evaluated later, treat it as an optional vector-store adapter, not as the main application database.

## Layers

```text
Vue workbench
-> REST / SSE controllers
-> application services
-> agent orchestration services
-> domain services
-> repositories / storage / model clients
-> MySQL 8, Redis, MinIO, optional VectorDB
```

The current implementation uses deterministic service behavior where model calls will later be inserted. This keeps the API, persistence, trace, and review contracts testable before vendor-specific LLM wiring.

## Core Domains

- RAG: knowledge bases, permissions, documents, index tasks, chunks, query logs, chat stream events, citations.
- Learning: learner profile, learning events, generated paths, path nodes, mastery records.
- Resource generation: generation tasks, generated drafts, critic reviews, agent tasks, traces, model-call logs, token usage logs.
- Assessment: submitted answers, grading results, wrong-question records, mastery updates, replan records.
- Governance: trace ids, health checks, model cost/token records, safety status, review gates.

## Multi-Agent Design

The target agent loop is:

```text
Learner input / course material / answer
-> Orchestrator
-> Profile Agent
-> Diagnosis Agent
-> Course RAG Agent
-> Path Planner Agent
-> Resource Agent
-> Question / Assessment Agent
-> Critic Agent
-> Tutor Agent
-> learning event + mastery update + replan
```

Each agent call must persist an `agent_task` row and ordered `agent_trace` rows. Each model call must persist token, latency, status, error, and estimated cost through `model_call_log` and `token_usage_log`.

## RAG Rules

- Retrieval must hard-filter knowledge-base ids by permission before chunk lookup.
- Source chunks must carry document, page, section, version, and KB metadata.
- Answers must return citations to the frontend.
- Query evidence is stored in `kb_query_log`.
- Vector retrieval, hybrid retrieval, RRF, and reranking are planned adapters. The current MySQL schema keeps chunk text and metadata while optional vector storage can live outside MySQL.

## Frontend Pages

- `/`: student learning loop, backed by RAG, resource-generation, agent-trace, and assessment APIs.
- `/teacher/reviews`: critic/teacher review queue for release governance.
- `/admin/operations`: dependency health, model-call/token counts, learning activity counts, and resource review status counts from the currently implemented analytics APIs.

The frontend must not call model providers or hold API keys. All model and retrieval work stays behind backend APIs.

## Phase Plan

1. Foundation: Spring Boot, MySQL/Flyway, health, trace filter, Docker Compose.
2. Course RAG: knowledge bases, permissions, documents, chunks, query endpoint, SSE stream.
3. Learning loop: profile extraction, path generation, learning events, mastery records.
4. Resource generation: agent task persistence, trace timeline, critic-gated drafts.
5. Assessment: answer submission, grading, wrong-question diagnosis, mastery update, replan record.
6. Frontend workbench: route-based pages, typed API clients, loading/error states.
7. Model integration: Spring AI/Spring AI Alibaba chat, embedding, structured output, tool calls.
8. Retrieval hardening: parser/chunker, embedding cache, optional VectorDB, hybrid retrieval, RRF, reranker, evaluation set.
