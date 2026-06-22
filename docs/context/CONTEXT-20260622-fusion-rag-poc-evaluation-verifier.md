# CONTEXT - Fusion RAG POC Evaluation Verifier

## 1. Related Memory / Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/specs/SPEC-20260606-rag-quality-evaluation.md`
- `docs/specs/SPEC-20260621-qa-verifier-eval-gate-mvp.md`
- `docs/specs/SPEC-20260622-rag-poc-context-builder.md`
- `docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`

## 2. Selected Skills

- feature-development-workflow
- educational-rag-pipeline
- test-driven-development
- verification-before-completion
- Confidence Check

## 3. Subagent Plan

- No spawned subagent due current tool policy.
- L1 analysis recorded in `docs/subagents/runs/RUN-20260622-fusion-rag-poc-evaluation-verifier.md`。

## 4. Allowed Files

Production:

- `backend/src/main/java/com/learningos/rag/application/CitationCoverageAnalyzer.java`
- `backend/src/main/java/com/learningos/rag/application/RagEvaluationRequest.java`
- `backend/src/main/java/com/learningos/rag/application/RagEvaluationResult.java`
- `backend/src/main/java/com/learningos/rag/application/RagEvaluationService.java`
- `backend/src/main/java/com/learningos/aiqa/application/quality/AnswerVerifier.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`

Tests:

- `backend/src/test/java/com/learningos/rag/application/CitationCoverageAnalyzerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationServiceTest.java`
- `backend/src/test/java/com/learningos/rag/api/RagEvaluationControllerTest.java`
- `backend/src/test/java/com/learningos/aiqa/application/quality/AnswerVerifierTest.java`
- `backend/src/test/java/com/learningos/aiqa/application/quality/QaEvalGateTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java`

Docs:

- `docs/specs/SPEC-20260622-fusion-rag-poc-evaluation-verifier.md`
- `docs/tasks/TASK-20260622-fusion-rag-poc-evaluation-verifier.md`
- `docs/context/CONTEXT-20260622-fusion-rag-poc-evaluation-verifier.md`
- `docs/subagents/runs/RUN-20260622-fusion-rag-poc-evaluation-verifier.md`
- `docs/evidence/EVIDENCE-20260622-fusion-rag-poc-evaluation-verifier.md`
- `docs/acceptance/ACCEPT-20260622-fusion-rag-poc-evaluation-verifier.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 5. Disallowed Files

- DB migration files.
- `pom.xml` / dependency files.
- Frontend files.
- Production RAG query endpoints beyond evaluation DTO/service.
- `RagQueryService` production retrieval/generation path.

## 6. Test Commands

RED / focused:

```bash
cd backend && mvn test "-Dtest=CitationCoverageAnalyzerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,AnswerVerifierTest,QaEvalGateTest,EvaluationRunServiceTest"
```

Adjacent:

```bash
cd backend && mvn test "-Dtest=RagQueryServiceTest,QaRuntimeTest,AiQaControllerTest"
```

Compile:

```bash
cd backend && mvn compile -q
```

## 7. Current Boundary

只做离线 evaluation 与 deterministic verifier quality signal；不引入 GraphRAG、Agentic RAG、live dual-run 或新 runtime provider。
