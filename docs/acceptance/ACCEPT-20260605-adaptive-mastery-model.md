# 自适应掌握度模型验收

## 验收结论

状态：通过。

## 验收项

| 验收项 | 结果 | 说明 |
|---|---|---|
| 读取历史掌握度 | PASS | 有 `mastery_record` 时，`beforeMastery` 使用最新记录 |
| 默认初始掌握度 | PASS | 无历史记录时保留默认 `0.42` |
| 动态掌握度更新 | PASS | 按错因和当前 mastery 计算 `afterMastery` |
| 掌握度边界 | PASS | 结果限制在 `[0.0, 1.0]`，避免 NaN/Infinity |
| 路径重规划联动 | PASS | 动态 mastery 继续进入 `LearningPathReplanService` |
| 默认路径持久化 | PASS | 无历史 mastery 时断言默认更新和最新 `mastery_record` |
| API 兼容 | PASS | `/api/assessment/answers` 请求和响应字段未变 |
| 架构分层 | PASS | Controller 未增加业务逻辑；Repository 读取在 Service 层 |
| 数据库变更 | PASS | 无新增迁移、无新增依赖 |
| 全量测试 | PASS | `mvn test`：80 tests, 0 failures |

## 限制

- 当前知识点由 `questionId` 规则推导，后续接入 `Question` 实体后应改为读取题目绑定的 `knowledgePointId`。
- BKT-lite 是确定性规则，适合 MVP 和答辩解释；完整 IRT/BKT 仍是后续增强。
- 低到高跨越 `0.80` 的路径重规划测试需要等后续支持正确答案/满分 grading 后补充。
- 答题提交幂等仍未完成，重复提交可能产生重复答题和 mastery 记录。
