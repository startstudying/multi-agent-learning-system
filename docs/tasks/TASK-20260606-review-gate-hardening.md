# Review Gate 强约束任务

## 任务清单

- [x] 读取项目记忆、总 TODO、资源生成和审核代码。
- [x] 启动并集成并行 subagent 审查：RAG 幂等、Review Gate、Orchestrator。
- [x] 创建本轮 workflow 文档。
- [x] 先写失败测试，覆盖未审核正文不可见。
- [x] 在 Service 层实现资源正文屏蔽。
- [x] 跑聚焦测试。
- [x] 集成 subagent 审查结论。
- [x] 跑全量后端测试。
- [x] 更新 Evidence、Acceptance、Memory、Changelog。

## Done Criteria

- [x] 未审核 `POST /api/resources/generation-tasks` 响应不返回 `markdownContent`。
- [x] 未审核 `GET /api/resources/generation-tasks/{taskId}` 不返回 `markdownContent`。
- [x] 未审核正式发布接口仍返回 403。
- [x] 全部审核通过后可以返回正文。
- [x] `mvn test` 通过或记录失败原因。
