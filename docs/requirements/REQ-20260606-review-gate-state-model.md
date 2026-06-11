# Review Gate 状态模型加固需求

## 1. 追溯

- PRD：`docs/product/PRD-20260606-review-gate-state-model.md`
- 需求编号：REQ-20260606-review-gate-state-model

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | 系统必须定义 `DRAFT`、`PENDING_CRITIC`、`APPROVED`、`REVISION_REQUESTED`、`REJECTED`、`PUBLISHED` 资源审核状态。 | 必须 | 状态集合可在常量和测试中验证 |
| FR-02 | 教师审核决策必须支持 `APPROVED`、`REVISION_REQUESTED`、`REJECTED`。 | 必须 | `REJECTED` 请求不再返回 `VALIDATION_ERROR` |
| FR-03 | 审核请求必须支持 `reason`、`citationCheck`、`safetyCheck`、`revisionSuggestion`。 | 必须 | API response 和数据库实体能读到这些字段 |
| FR-04 | 当单条 review 为 `REJECTED` 时，对应资源进入 `REJECTED`，任务保持不可发布状态。 | 必须 | `canReleaseToLearner(taskId)` 返回 false |
| FR-05 | 当全部 review 为 `APPROVED` 时，任务和资源进入 `PUBLISHED`。 | 必须 | 学生端资源读取通过，任务/资源状态为 `PUBLISHED` |
| FR-06 | `canReleaseToLearner` 只能对 `PUBLISHED` 且全部 review 为 `APPROVED` 的任务返回 true。 | 必须 | 仅伪造 `APPROVED` 不可绕过发布 |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增外部依赖。 | 必须 |
| NFR-02 | Controller 只做请求映射和校验，业务规则在 Service 层。 | 必须 |
| NFR-03 | 审核字段长度受控，避免大文本进入审计表。 | 必须 |
| NFR-04 | 新增数据库字段必须有 Flyway migration 和迁移文本测试。 | 必须 |

## 4. 用户流程

### 流程 1：教师拒绝资源

```text
教师 → 提交 REJECTED 决策和原因 → 系统保存结构化审核字段 → 资源状态 REJECTED → 学生端仍不可读取
```

### 流程 2：教师全部批准资源

```text
教师 → 逐条提交 APPROVED 决策 → 系统保存结构化审核字段 → 全部通过后任务/资源 PUBLISHED → 学生端可读取
```

## 5. 输入 / 输出

### 输入

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| decision | string | 是 | `APPROVED` / `REVISION_REQUESTED` / `REJECTED` |
| summary | string | 是 | 非空 |
| reason | string | 否 | 最长 2000 |
| citationCheck | string | 否 | 最长 2000 |
| safetyCheck | string | 否 | 最长 2000 |
| revisionSuggestion | string | 否 | 最长 2000 |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| status | string | review 决策状态 |
| resourceReviewStatus | string | 资源当前审核/发布状态 |
| reason | string | 审核原因 |
| citationCheck | string | 引用检查摘要 |
| safetyCheck | string | 安全检查摘要 |
| revisionSuggestion | string | 修订建议 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| unsupported decision | 返回 `VALIDATION_ERROR` |
| 任一资源 `REJECTED` | 任务不可发布 |
| 任一 review 未批准 | 任务不可发布 |
| 资源状态伪造为 `PUBLISHED` 但 review 未批准 | 任务不可发布 |

## 7. 依赖关系

- 上游依赖：资源生成任务已创建 review 记录。
- 下游影响：学生资源读取和 Review Queue。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Backend | 2026-06-06 | 草案 |
