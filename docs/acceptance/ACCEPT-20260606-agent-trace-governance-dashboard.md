# Agent Trace 治理看板验收

## 验收项

- [x] 新增 `/api/agent/traces` trace 查询过滤接口。
- [x] 支持用户、Agent 类型、状态、时间和失败原因过滤。
- [x] 新增服务层 `recordToolCall(...)` 写入能力。
- [x] trace detail 展示 tool call 摘要。
- [x] tool call 摘要不暴露 secret、token、password、private document/full text。
- [x] trace detail 返回 retention policy。
- [x] V15 migration 补齐 tool call 治理字段和索引。

## 非本轮范围

- 前端治理看板。
- 大文本清理定时任务。
- 真实 RBAC 细粒度审计权限。
