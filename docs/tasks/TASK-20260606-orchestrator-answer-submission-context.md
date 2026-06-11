# Orchestrator ANSWER_SUBMISSION 上下文收敛任务

## Task Boundary

只做 P0-1 的 `ANSWER_SUBMISSION` 最小上下文收敛。目标是让答题提交进入统一 Orchestrator workflow，并保证 assessment 业务记录复用 workflow traceId。

## Checklist

- [x] 读取项目记忆、架构基线和相关规则。
- [x] 完成 Skill Selection Gate。
- [x] 启用多专家子代理只读评审。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / Context Pack。
- [x] 编写失败测试覆盖 `ANSWER_SUBMISSION` create/get/invalid/replay/conflict。
- [x] 实现 `AssessmentService` 显式 traceId 入口和 replay preflight。
- [x] 实现 Orchestrator `ANSWER_SUBMISSION` 分支。
- [x] 补齐 `requestId` 服务层校验：非空且长度不超过 120。
- [x] 补齐 Orchestrator replay 精确匹配，避免只按 `requestId` 命中错误 workflow。
- [x] 补齐 trace drift 处理：返回已有 winning workflow，并清理 transient loser task/trace。
- [x] 修复全量测试暴露的 RAG timeout recovery 时间 fixture 漂移问题。
- [x] 运行聚焦测试。
- [x] 运行全量测试。
- [x] 更新 Evidence / Acceptance / TODO / Memory / Changelog / Retro。

## Done Criteria

- `workflowType=ANSWER_SUBMISSION` 成功返回 `DONE`。
- response 和 GET workflow 都能看到同一 `workflowId / agentTaskId / traceId`。
- assessment 业务表 traceId 与 Orchestrator traceId 一致。
- replay 不新增 workflow task 和业务记录。
- replay 必须精确匹配 workflow envelope，不允许同 `requestId` 的错误候选抢先返回。
- trace drift 必须返回已有 winning workflow，不保留本次 transient loser task/trace。
- conflict 和 invalid payload 不新增 workflow task。
- 空或过长 `requestId` 在 service 层拒绝。
- 直接 `/api/assessment/answers` 测试仍通过。
