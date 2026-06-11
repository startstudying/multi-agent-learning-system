# 答题提交幂等任务

## 任务清单

- [x] 读取项目记忆、TODO、assessment 代码和迁移。
- [x] 启动 Backend Architect 与 Security Reviewer 子代理分析。
- [x] 创建本轮 workflow 文档。
- [x] 写失败测试覆盖幂等 replay、冲突、learner 隔离和并发唯一键冲突 replay。
- [x] 实现 `requestId` DTO/entity/repository/service。
- [x] 新增 Flyway V5 迁移和迁移文本测试。
- [x] 跑聚焦测试。
- [x] 跑全量后端测试。
- [x] 更新 evidence、acceptance、memory、changelog。

## Done Criteria

- `POST /api/assessment/answers` 缺失 `requestId` 返回 400。
- 同一 learner 同一 requestId 相同 payload 返回首次响应且业务表计数不增加。
- 同一 learner 同一 requestId 不同 payload 返回 409。
- 不同 learner 同一 requestId 互不影响。
- `mvn test` 通过或记录失败原因。
- 并发同 learner 同 requestId 命中唯一索引时，不返回 500；相同 payload 重放首次响应，不同 payload 返回 409。
