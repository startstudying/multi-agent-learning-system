# RUN - P3-4-U 架构专家报告

## 结论

架构专家建议下一切片优先处理 `ResourceGeneration / Agent Trace` 详情尾部 roles-first RBAC。

## 关键证据

- `AgentTraceController` / `AgentTraceGovernanceController` 仍存在只传 `currentUserId` 的路径。
- `AgentTraceGovernanceService` 和 `ResourceGenerationService` 详情路径仍有 `"admin".equals(...)` 管理员判断。
- P3-4-S/T 已完成 create/retry，但明确未触碰 detail/trace/cancel/review。

## 建议

- 候选后续切片：ResourceGeneration detail 与 Agent Trace governance roles-first RBAC。
- 不建议在同一切片引入 formal OAuth2/JWK/Spring Security、DB migration、frontend 或 ResourceGeneration create/retry 行为变更。

## 集成备注

本报告指出的 detail/trace 风险有效，但本轮安全专家将 Review Gate 审核发布面评为 HIGH；主线本次优先处理 Review Gate，trace/detail 记录为下一候选。
