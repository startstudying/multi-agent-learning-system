# Review Gate 状态模型加固证据

## 范围

本轮完成 P0-4 Review Gate 状态模型加固：补齐资源审核状态常量，支持 `REJECTED` 决策，保存结构化审核证据，并将“审核通过”和“学生可发布”显式拆开。单条 `ResourceReview.status` 保留审核决策，任务和资源在全部 review 通过后进入 `PUBLISHED`。

## 代码证据

- `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java`
  - 增加 `DRAFT`、`PENDING_CRITIC`、`APPROVED`、`REVISION_REQUESTED`、`REJECTED`、`PUBLISHED` Review 状态集合。
- `backend/src/main/java/com/learningos/agent/domain/ResourceReview.java`
  - 增加 `reason`、`citationCheck`、`safetyCheck`、`revisionSuggestion` 结构化审核字段。
- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
  - Review 决策请求支持结构化审核字段。
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
  - 决策支持 `APPROVED`、`REVISION_REQUESTED`、`REJECTED`。
  - 任一 review 拒绝时，资源和任务保持 `REJECTED`，不可发布。
  - 任一 review 要求修改时，任务保持 `REVISION_REQUESTED`，不可发布。
  - 全部 review 通过后，任务和资源进入 `PUBLISHED`，Agent task 进入 `DONE`。
  - `canReleaseToLearner(...)` 只允许 `PUBLISHED` 任务、`PUBLISHED` 资源和全部 `APPROVED` review 通过。
- `backend/src/main/resources/db/migration/V6__resource_review_governance.sql`
  - 为 `resource_review` 增加四个结构化审核字段。

## TDD 过程

### RED

先补测试后运行聚焦测试，测试编译失败，暴露生产代码缺失字段和 DTO：

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,SchemaConvergenceMigrationTest" test
```

失败点包括 `ResourceReview.getReason/getCitationCheck/getSafetyCheck/getRevisionSuggestion`、新 `ReviewDecisionRequest` 构造参数和 summary 字段不存在。

### GREEN：聚焦测试

实现状态、字段、服务逻辑和 V6 migration 后重新运行：

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：回归测试

覆盖资源生成和 Review Gate 交叉路径：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```

结果：

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：全量后端测试

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 01:12 min
Finished at: 2026-06-06T10:54:08+08:00
```

## Subagent 审查证据

本轮使用已有 subagent 做 L1 并行审查。汇总见：

- `docs/subagents/runs/RUN-20260606-review-gate-state-model.md`

结论：

- Review Gate 状态模型本轮已收口。
- 后续优先关注 workflow failure strategy、RAG query replay、document upload idempotency 和教师/管理员权限。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 只接收请求并调用 `ReviewGovernanceService`，业务规则仍在 Service 层。 |
| Agent governance | PASS | 全部审核通过后仍通过 `AgentRunRecorder.transitionTask(...)` 收敛 Agent task 状态。 |
| Review Gate | PASS | `APPROVED` 决策和 `PUBLISHED` 发布状态已分离。 |
| Database | PASS | 已新增 V6 migration，并有 migration 文本测试。 |
| Frontend | PASS | 未修改前端，不改变页面交互。 |
| Security | PARTIAL | 本轮未新增教师/管理员角色权限，仍需 P3-4 单独加固。 |
