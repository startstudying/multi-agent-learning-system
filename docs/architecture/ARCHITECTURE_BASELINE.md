# Architecture Baseline

## Backend Layering

```
Controller
→ Application Service
→ Domain Service / Agent Orchestrator
→ Tool
→ Service
→ Mapper / Repository
→ Database
```

## Frontend Rules

- Frontend cannot call LLM APIs directly.
- Frontend cannot store API keys.
- API calls must use shared request wrapper.
- AI streaming must use SSE wrapper.

## Agent Rules

- Agent Tool cannot call Mapper directly.
- Tool must call Service.
- Agent execution must write trace.
- Max tool round trips must be limited.
- Model output must be validated.

## RAG Rules

- RAG answers must include sources.
- Retrieval must enforce permission filtering.
- Context must be trimmed.
- Query and embedding cache should be considered.
- Retrieval quality should be measurable.

## Security Rules

- Permission cannot rely on Prompt.
- Sensitive data cannot enter Prompt.
- API keys cannot be committed.

## Module Boundaries

| Module | Responsibility | Depends On |
|---|---|---|
| common | Auth, trace, exception | — |
| user | User management | common |
| learning | Learning workflow, path | user, common |
| assessment | Grading, feedback | learning, common |
| rag | RAG pipeline | common, knowledge |
| orchestrator | Agent coordination | rag, learning, common |
| tutor | AI tutoring | orchestrator, rag |
| analytics | Metrics, dashboards | learning, common |
| knowledge | Course knowledge base | common |
