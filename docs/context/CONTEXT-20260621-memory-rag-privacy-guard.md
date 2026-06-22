# CONTEXT-20260621 Memory/RAG Privacy Guard

## 1. Related Memory And Docs

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/security/AI_TOOL_SECURITY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-hybrid-retrieval.md`

## 2. Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `agent-trace-governance`
- `spring-ai-agent-backend`
- `security-review`
- `test-driven-development`
- `verification-before-completion`

## 3. Current Findings

- `RagQueryService.persistQueryLog(...)` 当前写入 raw `question`。
- `RagQueryService.persistQueryLog(...)` 在 requestId 场景写入完整 `responseJson`，其中可能包含 answer、source excerpt 和 no-source answer 中的 raw question。
- `RagQueryService.toSourceCitationRecord(...)` 当前写入完整 source excerpt。
- `LearningWorkflowService.profileSnapshot(...)` 当前写入 `learnerId` 和 `teacher_note`。
- `ResourceGenerationService.profileSnapshot(...)` 当前写入 `learnerId` 和 `teacher_note`，并把快照传入 `ResourceAgent` 模型请求参数。

## 4. Subagent Plan

不使用 subagent。本切片由主线程串行完成，避免多个执行者触碰同一批后端服务文件。

## 5. Files Allowed To Modify

- `backend/src/main/java/com/learningos/common/privacy/MemoryPrivacyPolicy.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `docs/specs/SPEC-20260621-memory-rag-privacy-guard.md`
- `docs/tasks/TASK-20260621-memory-rag-privacy-guard.md`
- `docs/context/CONTEXT-20260621-memory-rag-privacy-guard.md`
- `docs/evidence/EVIDENCE-20260621-memory-rag-privacy-guard.md`
- `docs/acceptance/ACCEPT-20260621-memory-rag-privacy-guard.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`

## 6. Files Not Allowed To Modify

- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `pom.xml`
- `backend/src/main/java/com/learningos/**/api/**` DTO/Controller contracts unless later重新分级。
- 任何密钥、环境变量或依赖锁文件。

## 7. Test Commands

```powershell
cd backend; mvn --% -Dtest=RagQueryServiceTest test
cd backend; mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
```

## 8. Task Boundary

只治理新写入数据：

- 不清理历史 DB 内容。
- 不新增 retention sweep。
- 不扩展长期记忆表。
- 不改变前端页面。
- 不新增模型 provider 或 RAG reranker 行为。

## 9. Architecture Notes

- 隐私策略放在 `common`，避免 `learning` 依赖 `rag`。
- RAG 权限过滤顺序保持不变。
- Durable replay snapshot 会降级；现场 API 响应仍返回当前用户可访问的 citations。
- `teacher_note` 仍可保留在 learner profile 结构化字段中供后续受控教师场景治理，但不进入本切片涉及的模型上下文或长期 profile snapshot。
