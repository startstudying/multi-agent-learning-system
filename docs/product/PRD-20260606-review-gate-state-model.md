# Review Gate 状态模型加固 PRD

## 1. 问题陈述

当前 Review Gate 已能阻止未审核资源直接暴露给学生，但审核状态仍偏粗：任务、资源、review 记录主要使用 `PENDING_CRITIC`、`APPROVED`、`REVISION_REQUESTED`，缺少 `REJECTED` 和 `PUBLISHED` 的显式区分，也缺少审核原因、引用检查、安全检查、修订建议等结构化字段。

这会导致教师或管理员只能看到一段 summary，难以解释“为什么通过、为什么退回、为什么拒绝”，也不利于后续 Critic Agent 自动审核和审计追踪。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 教师 | 审核者 | 能明确给出通过、退回、拒绝，并留下结构化原因 |
| 管理员 | 治理者 | 能追踪每条审核决策的引用、安全和修订证据 |
| 学生 | 学习者 | 只看到已发布且通过治理的学习资源 |

## 3. 用户故事

- 作为教师，我希望在审核资源时选择 `APPROVED`、`REVISION_REQUESTED` 或 `REJECTED`，以便区分通过、需要修改和不可发布。
- 作为管理员，我希望审核记录包含 structured evidence，以便解释每次 AI 资源审核结果。
- 作为学生，我希望只读取 `PUBLISHED` 的学习资源，以避免看到未审、退回或拒绝的内容。

## 4. MVP 范围

### 纳入范围

- 增加显式 Review 状态常量：`DRAFT`、`PENDING_CRITIC`、`APPROVED`、`REVISION_REQUESTED`、`REJECTED`、`PUBLISHED`。
- Review 决策支持 `REJECTED`。
- 审核决策请求支持结构化字段：`reason`、`citationCheck`、`safetyCheck`、`revisionSuggestion`。
- `resource_review` 保存上述结构化字段。
- 全部 review 通过后，任务和资源进入 `PUBLISHED`，学生端 release 只接受 `PUBLISHED`。

### 非目标

- 不实现真实 Critic Agent LLM 自动审核。
- 不实现教师权限/班级权限模型。
- 不实现完整资源修订工作流。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| 审核状态可区分 | 支持 6 个状态 | 常量、服务逻辑、测试 |
| 审核日志可解释 | 保存 4 类结构化字段 | API 和 repository 断言 |
| 学生端发布约束 | 仅 `PUBLISHED` 可见 | controller/service 测试 |

## 6. 用户流程

```text
资源草稿生成
→ Review 记录为 PENDING_CRITIC
→ 教师选择 APPROVED / REVISION_REQUESTED / REJECTED
→ 保存 reason / citationCheck / safetyCheck / revisionSuggestion
→ 全部通过后任务和资源变为 PUBLISHED
→ 学生端可读取资源正文
```

## 7. 依赖关系

- 依赖：现有 `ReviewGovernanceService`、`ResourceReviewController`、`resource_review` 表。
- 阻塞：无。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| `PUBLISHED` 是否应单独持久化到 review 记录 | Backend | 本轮不做，review 记录保留审核决策状态 |

## 9. 审批

| 角色 | 姓名 | 日期 | 状态 |
|---|---|---|---|
| Backend | Codex | 2026-06-06 | 草案 |
