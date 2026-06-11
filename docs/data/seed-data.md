# Demo Seed Data

Current demo data is intentionally lightweight and deterministic. It exists to make the learning workflow and frontend workbench predictable during local development.

## Current Demo Values

- Knowledge base: Java backend course
- Document: `database-course.md`
- RAG citation: SQL JOIN duplication, page 12
- Learning path:
  - HTTP and controller basics
  - SQL JOIN duplication diagnosis
  - Build a cited RAG service
- Resource generation:
  - LECTURE draft
  - EXERCISE draft
  - Critic review pending
- Assessment:
  - `q_sql_join`
  - score `0.85`
  - mastery update from `42%` to `58%`

The backend currently creates demo workflow responses in service code for Phase 3-5 while Phase 2 persists RAG entities through JPA.

## What Is Persisted Today

Phase 2 RAG entities are backed by the Flyway migration in `backend/src/main/resources/db/migration/V1__rag_foundation.sql`. The persisted model includes:

- `kb_knowledge_base`
- `kb_permission`
- `kb_document`
- `kb_doc_chunk`
- `kb_index_task`
- `kb_chat_session`
- `kb_chat_message`
- `kb_query_log`

Workflow responses for learner profile extraction, learning paths, resource generation, Agent trace, and assessment remain deterministic service-level fixtures in the current release.

## Local Seed Strategy

Use deterministic values only in development and tests:

- Prefer test setup code for controller and service tests.
- Prefer a profile-scoped initializer for interactive demos.
- Keep local demo users, KB ids, document ids, and question ids stable so screenshots and smoke tests are repeatable.
- Keep demo content small enough to inspect manually.

Do not add seed rows to the main RAG migration unless they are required reference data. Production schemas should be empty after migrations unless a separate seed profile is explicitly enabled.

## Production Seed Strategy

When production seed data is needed, move these values into one of these mechanisms:

- A Flyway repeatable migration for required lookup/reference data.
- A profile-scoped Spring data initializer such as `dev-seed` or `demo-seed`.
- A documented import script that writes through application services and enforces permissions.

Required production rules:

- Never seed private learner data into a shared environment.
- Never seed API keys, model credentials, MinIO secrets, or database passwords.
- Every seeded knowledge base must have explicit owner and permission rows.
- Every seeded RAG citation must point to an existing document and chunk.
- Demo Agent traces must be labeled as demo traces and excluded from analytics baselines.
