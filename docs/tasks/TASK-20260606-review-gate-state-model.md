# Review Gate 状态模型加固任务

## 1. 追溯

- PLAN：`docs/plans/PLAN-20260606-review-gate-state-model.md`
- SPEC：`docs/specs/SPEC-20260606-review-gate-state-model.md`
- 任务编号：TASK-20260606-review-gate-state-model

## 2. 目标

补齐 Review Gate 的显式状态模型和结构化审核日志，让教师可以拒绝资源，全部审核通过后任务/资源进入 `PUBLISHED`，学生端只读取已发布资源。

## 3. 范围

### 纳入范围

- Review 状态常量。
- `REJECTED` 决策。
- `reason/citationCheck/safetyCheck/revisionSuggestion` 字段。
- `PUBLISHED` 发布状态。
- V6 migration 和测试。

### 排除范围

- 真实 Critic Agent LLM 审核。
- 教师角色权限模型。
- 前端页面改造。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java`
- `backend/src/main/java/com/learningos/agent/domain/ResourceReview.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/main/resources/db/migration/V6__resource_review_governance.sql`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- 本任务对应的 `docs/**` 工作流文档、Memory、Changelog、TODO。

## 5. 禁止修改的文件

- 前端文件。
- RAG 生产逻辑。
- Orchestrator 生产逻辑。
- 现有 API 路径。

## 6. 实施步骤

1. 写 RED 测试。
2. 实现状态常量和结构化字段。
3. 更新 Review 决策逻辑和 release 判断。
4. 新增 V6 migration。
5. 运行聚焦测试。
6. 运行全量测试。
7. 更新证据、验收、记忆和 changelog。

## 7. 测试命令

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,SchemaConvergenceMigrationTest" test
mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
mvn test
```

## 8. 完成标准

- [x] `REJECTED` 决策测试通过。
- [x] 结构化审核字段测试通过。
- [x] 全部审核通过后任务和资源为 `PUBLISHED`。
- [x] 学生端只读取 `PUBLISHED` 资源。
- [x] V6 migration 文本测试通过。
- [x] Evidence / Acceptance / Memory / Changelog 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 2026-06-06 |
