# Review Gate 状态模型加固规格

## 1. 概述

本切片加固 P0-4 Review Gate：显式区分审核决策和学生可发布状态，补齐结构化审核证据，并确保学生端只读取 `PUBLISHED` 资源。

## 2. 追溯

- PRD：`docs/product/PRD-20260606-review-gate-state-model.md`
- REQ：`docs/requirements/REQ-20260606-review-gate-state-model.md`

## 3. 领域模型

### Review 决策状态

```text
PENDING_CRITIC
APPROVED
REVISION_REQUESTED
REJECTED
```

### 资源/任务发布状态

```text
DRAFT
PENDING_CRITIC
APPROVED
REVISION_REQUESTED
REJECTED
PUBLISHED
```

本轮约定：

- `ResourceReview.status` 保存单条审核决策，不保存 `PUBLISHED`。
- `LearningResource.reviewStatus` 和 `ResourceGenerationTask.reviewStatus` 保存资源/任务治理状态。
- 全部 review `APPROVED` 后，资源和任务状态进入 `PUBLISHED`。

## 4. API 契约

### 端点

```http
POST /api/reviews/resources/{reviewId}/decision
```

### 请求

```json
{
  "decision": "REJECTED",
  "summary": "引用不足，不能发布。",
  "reason": "资源中包含未经课程资料支持的结论。",
  "citationCheck": "缺少至少一条课程资料引用。",
  "safetyCheck": "未发现安全违规。",
  "revisionSuggestion": "补充课程资料引用后重新提交。"
}
```

### 响应

```json
{
  "reviewId": "rrv_xxx",
  "resourceId": "res_xxx",
  "generationTaskId": "rgt_xxx",
  "status": "REJECTED",
  "summary": "引用不足，不能发布。",
  "resourceReviewStatus": "REJECTED",
  "reason": "资源中包含未经课程资料支持的结论。",
  "citationCheck": "缺少至少一条课程资料引用。",
  "safetyCheck": "未发现安全违规。",
  "revisionSuggestion": "补充课程资料引用后重新提交。"
}
```

## 5. 前端交互

本轮不改前端。后端 response 字段可供教师端 Review Queue 后续展示。

## 6. 后端流程

```text
validate decision
-> load review/resource/task
-> persist review.status + structured fields
-> set resource.reviewStatus
-> if rejected: task.reviewStatus = REJECTED
-> else if revision requested: task.reviewStatus = REVISION_REQUESTED
-> else if all reviews approved: task/resource reviewStatus = PUBLISHED, task status = DONE
-> return ResourceReviewSummary
```

## 7. Agent 工作流

不新增真实 Critic Agent 调用。现有 `CriticAgent` review 记录继续作为审核入口，后续可由模型自动填充结构化字段。

## 8. RAG 工作流

本轮不改 RAG 检索。`citationCheck` 作为 Review Gate 结构化证据字段保存。

## 9. 数据库变更

新增 migration：`V6__resource_review_governance.sql`

```sql
alter table resource_review
  add column reason varchar(2000),
  add column citation_check varchar(2000),
  add column safety_check varchar(2000),
  add column revision_suggestion varchar(2000);
```

## 10. 状态流转

```text
resource/task: PENDING_CRITIC -> REVISION_REQUESTED
resource/task: PENDING_CRITIC -> REJECTED
resource/task: PENDING_CRITIC -> PUBLISHED
```

## 11. 错误处理

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| VALIDATION_ERROR | 决策非法 | decision 非三种允许值 |
| NOT_FOUND | 审核记录不存在 | reviewId 不存在 |

## 12. 权限规则

本轮保持现有接口权限模型，不新增教师角色校验。学生端资源读取仍通过 `ReviewGovernanceService.canReleaseToLearner(...)`。

## 13. Trace / 日志

结构化审核字段保存到 `resource_review`，并继续保留 `traceId`。

## 14. 测试策略

- `ResourceReviewControllerTest`：覆盖 rejected 决策、结构化字段 response、全部 approved 后 `PUBLISHED`。
- `ReviewGovernanceServiceTest`：覆盖 `PUBLISHED` 才可 release，伪造状态不可绕过。
- `SchemaConvergenceMigrationTest`：覆盖 V6 migration 文本。

## 15. 验收清单

- [x] `REJECTED` 决策可提交。
- [x] 结构化审核字段可持久化和返回。
- [x] 全部通过后任务/资源为 `PUBLISHED`。
- [x] `canReleaseToLearner` 只对 `PUBLISHED` 且全部 review approved 返回 true。
- [x] 测试通过。
