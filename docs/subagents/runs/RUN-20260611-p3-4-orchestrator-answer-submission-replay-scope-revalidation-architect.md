# RUN-20260611 P3-4 Orchestrator ANSWER_SUBMISSION Replay Scope Revalidation Architect

## 结论

建议按 S 级安全修补切片执行。

虽然该问题跨越 Orchestrator 调用链与 Assessment replay 服务入口，但生产修复可以收敛在 `AssessmentService.replayAnswerIfPresent(...)`。这能保护所有调用该 replay 服务入口的上层路径，同时不改变 Orchestrator API/DTO/schema。

## 推荐边界

### 允许修改

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/tasks/TASK-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-*.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 禁止修改

- `backend/src/main/java/com/learningos/orchestrator/api/**`
- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/resources/db/migration/**`
- `backend/pom.xml`
- `frontend/**`
- PRD / REQ / SPEC / PLAN，除非任务升级为 M。

## 架构判断

低漂移路径：

1. Orchestrator 继续只负责 workflow 编排。
2. Assessment Service 负责答题 replay 的业务授权一致性。
3. 复用既有 `requireSubmitQuestionScope(...)`。
4. 不恢复 subject-name role inference。
5. 不让 Controller 做对象权限判断。

## Size 分类

Size：S。

理由：

- 一个明确缺陷。
- 最小生产改动为一行服务层校验。
- 不改 contract/schema/dependency/frontend。
- focused/adjacent/full backend tests 足够验收。

升级触发：

- 需要改 workflow envelope、AgentTask persistence、认证/角色模型或 schema。
- 需要引入新的 retry/replay contract。
- 新增测试暴露多个业务路径存在同类系统性缺陷。

## Architecture Drift Check 预判

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 权限保持在 Service 层。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime。 |
| Security | PASS | replay 前重新校验对象范围。 |
| API / Database | PASS | 不改 API 或 schema。 |
