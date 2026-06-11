# Evaluation Set 管理 Context Pack

## 当前任务

实现 `evaluation_set` 和 `evaluation_sample` 的最小后端管理闭环，完成 TODO 第 142 行。

## 已选技能

- `feature-development-workflow`
- `ai-learning-agent-development`
- `spring-ai-agent-backend`
- `educational-rag-pipeline`
- `assessment-feedback-agent`
- `test-driven-development`
- `verification-before-completion`

## 子任务计划

- L1 只读分析：
  - 架构边界分析
  - 测试策略分析
  - 安全边界分析
- 编码由主 Codex 单线完成，避免并行写同一模块。

## 允许修改文件

- `backend/src/main/java/com/learningos/evaluation/**`
- `backend/src/test/java/com/learningos/evaluation/**`
- `backend/src/main/resources/db/migration/V13__evaluation_set_management.sql`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/product/PRD-20260606-evaluation-set-management.md`
- `docs/requirements/REQ-20260606-evaluation-set-management.md`
- `docs/specs/SPEC-20260606-evaluation-set-management.md`
- `docs/plans/PLAN-20260606-evaluation-set-management.md`
- `docs/tasks/TASK-20260606-evaluation-set-management.md`
- `docs/context/CONTEXT-20260606-evaluation-set-management.md`
- `docs/subagents/runs/RUN-20260606-evaluation-set-management.md`
- `docs/evidence/EVIDENCE-20260606-evaluation-set-management.md`
- `docs/acceptance/ACCEPT-20260606-evaluation-set-management.md`
- `docs/retrospectives/RETRO-20260606-evaluation-set-management.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

## 禁止事项

- 不新增依赖。
- 不实现 evaluation run。
- 不实现 prompt version comparison。
- 不保存 raw prompt 或 raw model output。
- 不让 Controller 直接访问 Repository。

## 验证命令

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,SchemaConvergenceMigrationTest" test
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,PromptVersionServiceTest,PromptVersionControllerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,GradingEvaluationServiceTest,SchemaConvergenceMigrationTest" test
```
