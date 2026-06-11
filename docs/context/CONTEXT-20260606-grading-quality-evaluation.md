# 自动批改质量评估 Context Pack

## 当前任务边界

实现 TODO P2-3「自动批改质量评估」最小可验收后端切片，在现有 `/api/assessment/grading-evaluations` 基础上支持人工评分样本和离线指标报告。

## 已读取上下文

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationSummary.java`
- `backend/src/main/java/com/learningos/assessment/dto/GradingEvaluationRequest.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`

## Selected Skills

- `feature-development-workflow`
- `assessment-feedback-agent`
- `spring-ai-agent-backend`
- `test-driven-development`
- `Confidence Check`
- `verification-before-completion`

## Subagent Plan

不再派发子代理。当前 worker 作为并行后端 worker，只产出 `docs/subagents/runs/RUN-20260606-grading-quality-evaluation-worker.md` 供主协调者集成。

## 允许修改文件

- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluation*`
- `backend/src/main/java/com/learningos/assessment/dto/GradingEvaluation*`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`，仅限 grading evaluations endpoint
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`，仅限 grading evaluation 相关测试
- `docs/product/PRD-20260606-grading-quality-evaluation.md`
- `docs/requirements/REQ-20260606-grading-quality-evaluation.md`
- `docs/specs/SPEC-20260606-grading-quality-evaluation.md`
- `docs/plans/PLAN-20260606-grading-quality-evaluation.md`
- `docs/tasks/TASK-20260606-grading-quality-evaluation.md`
- `docs/context/CONTEXT-20260606-grading-quality-evaluation.md`
- `docs/evidence/EVIDENCE-20260606-grading-quality-evaluation.md`
- `docs/acceptance/ACCEPT-20260606-grading-quality-evaluation.md`
- `docs/subagents/runs/RUN-20260606-grading-quality-evaluation-worker.md`

## 禁止修改文件

- `docs/changelog/CHANGELOG.md`
- `docs/memory/*`
- `docs/planning/backend-architecture-todolist.md`
- `backend/src/main/java/com/learningos/evaluation/**`
- `backend/src/test/java/com/learningos/evaluation/**`
- `backend/src/main/resources/db/migration/V14__evaluation_run_quality_metrics.sql`
- 其他 worker 可能正在修改的 rag/agent 模块

## Test Commands

RED / GREEN 定向命令：

```powershell
cd backend; mvn "-Dtest=GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport" test
```

可选 service 快速命令：

```powershell
cd backend; mvn "-Dtest=GradingEvaluationServiceTest" test
```

## 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只转交 Service |
| Frontend rules | N/A | 不改前端 |
| Agent / RAG rules | N/A | 不涉及 Agent/RAG |
| Security | PASS | 不新增依赖，不写敏感数据 |
| API / Database | PASS | SPEC 已定义 API，不改数据库 |
