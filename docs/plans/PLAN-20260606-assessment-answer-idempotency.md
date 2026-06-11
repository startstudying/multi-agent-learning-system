# 答题提交幂等计划

## 执行顺序

1. 读取 assessment、learning、migration 当前实现。
2. 启动 Backend Architect 与 Security Reviewer 子代理做 L1 并行分析。
3. 创建 PRD、REQ、SPEC、PLAN、TASK、Context Pack。
4. 先写失败测试：
   - 相同 requestId 重放不重复写。
   - 相同 requestId 不同 payload 返回 409。
   - 不同 learner 同 requestId 隔离。
   - 并发同 requestId 命中数据库唯一约束后重新读取胜出记录并重放。
   - 迁移文本包含约束。
5. 实现 DTO、entity、repository、service、migration。
6. 跑 assessment 聚焦测试和迁移文本测试。
7. 跑相关回归和全量后端测试。
8. 更新 TODO、evidence、acceptance、memory、changelog。

## 风险

- 当前测试 profile 使用 H2 `ddl-auto=create-drop`，无法证明 MySQL Flyway 真执行成功。
- 同一 learner 不同 requestId 并发提交同一知识点仍可能竞争最新 mastery。
- 并发同 requestId 的唯一约束冲突已在 Service 层捕获并重放；完整 `PROCESSING/COMPLETED/FAILED` 幂等状态机后续可增强为独立幂等表。

## 回滚

移除 DTO 字段、AnswerRecord 字段、repository 查询、service replay 逻辑和 V5 迁移即可回滚；无业务数据破坏性删除。
