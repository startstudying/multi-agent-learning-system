# RAG 查询重放与响应快照 Context Pack

## 1. 当前任务

`docs/tasks/TASK-20260606-rag-query-replay-snapshot.md`

## 2. 相关记忆和文档

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260606-orchestrator-rag-qa-context.md`
- `docs/specs/SPEC-20260606-orchestrator-runtime-failure-evidence.md`
- `docs/specs/SPEC-20260606-assessment-answer-idempotency.md`

## 3. Selected Skills

- `feature-development-workflow`
- `ai-learning-agent-development`
- `educational-rag-pipeline`
- `spring-ai-agent-backend`
- `agent-trace-governance`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

L1 并行分析：

- 架构审查：只读，输出数据流、实现边界、风险。
- 测试审查：只读，输出 RED 测试和回归命令。
- 安全审查：只读，输出 replay 权限和隐私边界。

实现由主 Codex 单线完成，避免多 worker 修改同一批后端文件。

## 5. 允许修改

- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
- `backend/src/main/java/com/learningos/rag/repository/KbQueryLogRepository.java`
- `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java`
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/resources/db/migration/V7__rag_query_replay_snapshot.sql`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/product/PRD-20260606-rag-query-replay-snapshot.md`
- `docs/requirements/REQ-20260606-rag-query-replay-snapshot.md`
- `docs/specs/SPEC-20260606-rag-query-replay-snapshot.md`
- `docs/plans/PLAN-20260606-rag-query-replay-snapshot.md`
- `docs/tasks/TASK-20260606-rag-query-replay-snapshot.md`
- `docs/context/CONTEXT-20260606-rag-query-replay-snapshot.md`
- `docs/evidence/EVIDENCE-20260606-rag-query-replay-snapshot.md`
- `docs/acceptance/ACCEPT-20260606-rag-query-replay-snapshot.md`
- `docs/retrospectives/RETRO-20260606-rag-query-replay-snapshot.md`
- `docs/subagents/runs/RUN-20260606-rag-query-replay-snapshot.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. 禁止修改

- 前端文件。
- 文档上传幂等生产逻辑。
- 索引任务恢复生产逻辑。
- Review Gate 生产逻辑。
- 认证/角色权限模型。
- 外部依赖配置。

## 7. 测试命令

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest" test
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test
mvn test
```

## 8. 当前边界

- 有 `requestId` 的 RAG 查询走 replay；无 `requestId` 的旧查询保持原行为。
- replay 返回首次 traceId。
- 冲突不创建 Orchestrator workflow task。
- 未授权查询不写 query log、citation 或 response snapshot。
