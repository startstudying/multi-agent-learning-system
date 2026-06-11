# Review Gate 审核权限加固计划

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 本任务是后端权限加固，必须先写文档再实现。 |
| `spring-boot-architecture` | 变更 Controller/Service 调用边界。 |
| `api-contract-design` | 需要定义 403 行为和现有 API envelope。 |
| `security-review` | 涉及权限绕过和详情泄露风险。 |
| `test-generator` | 需要补充 controller/service 权限测试。 |
| `critic-review-agent` | 涉及资源审核队列和审核决策治理。 |
| `test-driven-development` | 行为变更先补失败测试。 |
| `verification-before-completion` | 完成前必须运行聚焦测试并记录证据。 |

Missing skills: 无。

GitHub Research Needed: No。当前项目已有 `CurrentUserService` 和临时 header 用户契约，本切片不需要外部参考。

New Project-Specific Skill To Create: No。

## Subagent Decision

Use Subagents: No。

Reason: 本切片只影响 Review Gate 后端权限，用户已指定 Worker C 文件边界；不在 Worker 内再派生并行实现。

Parallelism Level: Single Codex。

## Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementation | PASS | 现有 Review Gate 有状态治理，但 list/decision 未传入 current user。 |
| Architecture compliance | PASS | 复用 `CurrentUserService`、`ApiException(ErrorCode.FORBIDDEN)`、Controller -> Service 分层。 |
| Official docs verified | N/A | 不新增外部 API 或依赖。 |
| OSS references | N/A | 本切片是项目内部临时 RBAC 契约。 |
| Root cause identified | PASS | 审核接口缺少教师/管理员权限检查。 |

Confidence: 0.93。

## 实施步骤

1. 补 RED 测试：student list/decision 403，admin 正常。
2. 修改 Controller 注入 `CurrentUserService` 并传入当前用户。
3. 修改 Service 在 list/decision 入口执行 `assertReviewerAccess`。
4. 运行聚焦测试。
5. 写 Evidence 和 Acceptance。

## 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| 硬编码 `teacher/admin` 不是正式权限模型 | 中 | SPEC/Open Issues 明确临时策略，后续替换 RBAC |
| 修改 service 方法签名影响现有测试 | 低 | 同步更新 `ReviewGovernanceServiceTest` |
| 未授权错误消息泄露详情 | 中 | guard 放在 repository lookup 前，统一 403 文案 |
