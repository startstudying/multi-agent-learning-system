# 模型调用 Prompt 元数据开发计划

## 计划

1. 启动只读 subagent 分析架构、测试和安全边界。
2. 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
3. 写 RED 测试：
   - `AgentRunRecorderTest` 成功模型调用元数据。
   - `AgentRunRecorderTest` 失败模型调用元数据。
   - `SchemaConvergenceMigrationTest` V12 migration。
4. 运行 RED 并确认失败原因。
5. 实现 `ModelCallLog` 字段。
6. 在 `AgentRunRecorder` 写入 prompt metadata。
7. 新增 V12 migration。
8. 运行 focused 和相关回归测试。
9. 更新 evidence、acceptance、TODO、memory、changelog、retro。

## 风险

- 如果保存 raw prompt 或 raw output，会造成学习隐私泄露。
- 如果只记录成功调用，会导致失败分析缺少 prompt 版本。
- 如果 schema 字段过大或动态化，后续统计会难以聚合。

## 回滚

代码回滚可移除字段映射和日志写入；数据库新增列按向前兼容处理，不要求自动 drop。
