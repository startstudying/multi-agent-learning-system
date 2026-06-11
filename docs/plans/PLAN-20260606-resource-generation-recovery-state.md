# 资源生成任务恢复状态开发计划

## 计划

1. 补齐 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
2. 在 `ResourceGenerationControllerTest` 增加失败任务恢复状态测试。
3. 在 `SchemaConvergenceMigrationTest` 增加 V11 migration 文本测试。
4. 运行测试确认 RED。
5. 为 `ResourceGenerationTask` 增加恢复字段和默认值。
6. 扩展 `ResourceGenerationResponse`。
7. 在失败分支持久化恢复字段。
8. 新增 V11 Flyway migration。
9. 运行 focused 和相关回归测试。
10. 更新 evidence、acceptance、TODO、memory、changelog、retro。

## 风险

- `lastError` 如果保存原始 provider message，可能扩大敏感错误暴露面。
- `retryCount` 语义需要保持为业务任务恢复次数，不等同于模型网关内部 attempt 次数。
- 新增响应字段不能破坏既有 review gate 行为。

## 回滚方式

- 删除新增字段映射和响应字段。
- 回滚 V11 migration 文件需要数据库侧单独处理；当前实现按新增列向前兼容设计。
