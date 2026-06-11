# 模型调用 Prompt 元数据任务

## TASK-1 文档和范围固定

- [x] 启动只读 subagent。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- [x] 明确安全边界：只保存 schema 摘要，不保存 raw prompt/output。

## TASK-2 RED 测试

- [x] 成功模型调用日志元数据测试。
- [x] 失败模型调用日志元数据测试。
- [x] 失败模型调用 provider message 脱敏测试。
- [x] V12 migration 文本测试。
- [x] 运行并确认 RED。

## TASK-3 GREEN 实现

- [x] `ModelCallLog` 增加字段。
- [x] `AgentRunRecorder` 写入 prompt code/version/temperature/schema。
- [x] 新增 V12 migration。
- [x] 资源生成集成路径保持通过。

## TASK-4 验证和收尾

- [x] 运行 focused 测试。
- [x] 运行相关回归测试。
- [x] 更新 evidence / acceptance / TODO / memory / changelog / retrospective。

## Done Criteria

- 成功和失败模型调用均记录 prompt metadata。
- migration 文本测试覆盖新增列。
- P2-1 对应 TODO 项勾选。
