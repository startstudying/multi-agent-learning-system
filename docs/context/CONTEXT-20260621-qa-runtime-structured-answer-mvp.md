# CONTEXT-20260621 QaRuntime structured answer MVP

## 1. 相关文档

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/product/PRD-20260621-memory-answer-quality-roadmap.md`
- `docs/requirements/REQ-20260621-memory-answer-quality-roadmap.md`
- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/specs/SPEC-20260621-memory-context-service-mvp.md`
- `docs/evidence/EVIDENCE-20260621-memory-context-service-mvp.md`
- `docs/acceptance/ACCEPT-20260621-memory-context-service-mvp.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 2. 当前任务边界

只实现 P0-3 `QaRuntime structured answer MVP`。

不实现 P0-4 verifier/eval gate，不实现 P1 streaming / workbench，不实现 P2 memory lifecycle。

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/runtime/*.java`
- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/test/java/com/learningos/aiqa/application/runtime/*.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `docs/product/PRD-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/requirements/REQ-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/specs/SPEC-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/plans/PLAN-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/tasks/TASK-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/context/CONTEXT-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/evidence/EVIDENCE-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/acceptance/ACCEPT-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

## 4. 禁止修改文件

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/**`
- provider credentials / env files
- P0-4/P1/P2 相关生产模块

## 5. 测试命令

```powershell
cd backend
mvn --% -Dtest=QaRuntimeTest test
mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,MemoryContextServiceTest,RagQueryServiceTest test
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

## 6. 隐私与安全约束

- 不暴露 raw chain-of-thought。
- 不暴露 prompt、provider key、teacher note 或未脱敏 profileSnapshot。
- RAG 权限过滤只通过 `RagQueryService`。
- tool call summary 必须为低敏摘要。
