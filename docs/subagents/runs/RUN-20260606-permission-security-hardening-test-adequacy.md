# Subagent Report - P3-4 权限与安全加固测试充分性

## 角色

Test Engineering Expert

## 结论

P3-4 最小切片缺少以下验收测试，已纳入本轮实现范围：

- `LearningWorkflowControllerTest`：`POST /api/profile/dialogue/extract` 中 `X-User-Id=alice` 与 `learnerId=bob` 不一致时返回 `403 FORBIDDEN`，且不写入 `LearnerProfile` / `LearningEvent`。
- `LearningWorkflowControllerTest`：`POST /api/learning-paths` 中 `X-User-Id=alice` 与 `learnerId=bob` 不一致时返回 `403 FORBIDDEN`，且不写入路径、节点和事件。
- `LearningWorkflowControllerTest`：创建 `alice` 路径后，`bob` 读取同一 `pathId` 返回 `403 FORBIDDEN`，不返回路径详情。
- `AnalyticsControllerTest`：`GET /api/analytics/overview` 对 `teacher` / `alice` 返回 `403 FORBIDDEN`，对 `admin` 保持可访问。
- `HealthControllerTest`：检查原始响应体不包含 `minioadmin`、`access-key`、`secret-key`、`password`、`token`、数据库 URL、MinIO endpoint 和 bucket 等敏感或拓扑指纹。
- `RagQueryServiceTest`：混合合法和越权 `kbIds` 时返回 `FORBIDDEN`，且不写入 `kb_query_log` / `source_citation`。

## 夹具风险

- 原有学习流程测试存在 `X-User-Id=alice` 但 `learnerId=learner_1` 的旧夹具，需要改成 owner 一致。
- 原有 analytics overview 测试把 `teacher` 当作可访问方，需要改成 `admin` 成功路径并新增非 admin 拒绝路径。
- RAG strict 测试必须断言无持久化副作用，否则静默过滤仍可能通过响应级检查。

