# CONTEXT - RAG POC Context Builder

## 1. Related Memory And Docs

- `docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/specs/SPEC-20260608-rag-hybrid-retrieval.md`
- `docs/specs/SPEC-20260606-rag-chunk-production-metadata.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 2. Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `test-driven-development`
- `verification-before-completion`

## 3. Subagent Plan

不使用 subagent。原因：当前工具策略要求只有用户明确要求 subagent 才能派生；本切片保持单 Codex 实现。

## 4. Allowed Files

- `backend/src/main/java/com/learningos/rag/application/PocContextBuilder.java`
- `backend/src/main/java/com/learningos/rag/application/PocContextResult.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/test/java/com/learningos/rag/application/PocContextBuilderTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `docs/specs/SPEC-20260622-rag-poc-context-builder.md`
- `docs/tasks/TASK-20260622-rag-poc-context-builder.md`
- `docs/context/CONTEXT-20260622-rag-poc-context-builder.md`
- `docs/evidence/EVIDENCE-20260622-rag-poc-context-builder.md`
- `docs/acceptance/ACCEPT-20260622-rag-poc-context-builder.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 5. Disallowed Files

- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- Controller / DTO public API files，除非发现必须升级任务范围
- RAG parser / vector adapter / model provider runtime
- `docs/superpowers/**`

## 6. Test Commands

```bash
cd backend && mvn test -Dtest=PocContextBuilderTest,RagQueryServiceTest
cd backend && mvn compile -q
```

如果 focused tests 暴露相邻失败，再补跑相关 RAG / AI QA tests。

## 7. Current Boundary

只实现检索后上下文构造：

```text
Permission filter
-> Chunk retrieval / RRF
-> Reranker fallback
-> POC Context Builder
-> Deterministic grounded answer composition
-> Sources/citations remain original selected chunks
```

不实现 query rewrite、HyDE、MCP、API wrapper 或 VectorDB provider 替换。
