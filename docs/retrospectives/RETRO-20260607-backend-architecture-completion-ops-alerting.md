# Retrospective - P3-5-A 运维告警 API

## 1. Feature Summary

完成 `GET /api/analytics/ops/alerts` admin-only 查询型告警 API，覆盖：

- `SLOW_RAG_QUERY`
- `SLOW_MODEL_CALL`
- `RAG_NO_SOURCE`
- `REVIEW_BACKLOG`

实现未新增依赖、未修改数据库 schema、未修改前端；响应使用白名单 DTO，避免泄露 prompt、question、raw response、errorMessage、markdownContent 和 review 私有字段。

## 2. What Went Well

- 先补测试再实现，能清晰证明新路由从 RED 到 GREEN。
- 告警口径复用既有 `KbQueryLog`、`ModelCallLog`、`ResourceReview`，避免引入表结构或后台调度。
- 安全口径明确：不复用含 `errorMessage` 的异常模型调用 DTO。
- 相邻回归覆盖了日志、健康、RAG 查询和 Review Gate，能证明 P3-5 观测链路未互相破坏。

## 3. What Didn't Go Well

- 当前 admin-only 仍是过渡 `X-User-Id` 字符串身份，不能代表真实 RBAC/JWT。
- `RAG_NO_SOURCE` 只能使用 `retrievalCount <= 0`，缺少更结构化的 no-source 原因字段。
- 告警仍是 query-time 聚合，不具备外部推送、持久化阈值或通知能力。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 后端运维告警聚合 API：白名单 DTO、阈值校验、敏感字段脱敏、query-time 优先 | Yes | `docs/skills/project-specific/ops-alerting.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 告警切片复用 backend-architecture-completion 总文档 | 后续大型总文档继续在 TASK 顶部增加当前切片 override，避免误改旧任务范围 |
| Testing | 聚焦、相邻、完整后端测试均已执行 | 后续告警增强可增加 service-level unit test，减少 controller 集成测试 seed 体量 |
| Documentation | Evidence / Acceptance 记录完整测试命令 | 对 query-time vs push-time 告警能力继续在 SPEC 中显式区分 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 用真实 RBAC/JWT 替代 `X-User-Id: admin` | Backend | 后续 P3-4 |
| 如需生产告警，增加持久化阈值、通知渠道和 dashboard 规格 | Backend / Ops | 后续 P3-5 |
| 为 RAG no-source 增加结构化原因字段 | Backend / RAG | 后续 P3-2/P3-5 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md（无基线变更）
