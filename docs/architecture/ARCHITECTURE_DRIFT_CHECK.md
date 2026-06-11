# Architecture Drift Check

Run this checklist before and after every implementation task.

## Backend Layering

- [ ] Controller only handles HTTP, delegates to Service
- [ ] Service contains business logic
- [ ] Agent Tool calls Service, not Mapper/Repository
- [ ] No business logic in Mapper/Repository
- [ ] Global exception handler used for errors

## Frontend

- [ ] No direct LLM API calls from frontend
- [ ] No API keys in frontend code or env exposed to client
- [ ] API calls use shared request wrapper
- [ ] SSE streaming uses shared composable

## Agent / RAG

- [ ] Agent execution writes trace record
- [ ] Max tool round trips enforced
- [ ] Model output validated before persistence
- [ ] RAG answers include citation sources
- [ ] Retrieval enforces permission filtering
- [ ] AI-generated resources have review status

## Security

- [ ] Permission checks in backend code, not Prompt
- [ ] No secrets in code or memory files
- [ ] New dependencies reviewed in `docs/security/`

## API / Database

- [ ] API contract matches SPEC
- [ ] Database schema matches SPEC
- [ ] No undocumented endpoints

## Result

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS / FAIL | |
| Frontend rules | PASS / FAIL | |
| Agent / RAG rules | PASS / FAIL | |
| Security | PASS / FAIL | |
| API / Database | PASS / FAIL | |

If any FAIL: document drift in PLAN or create ADR before proceeding.
