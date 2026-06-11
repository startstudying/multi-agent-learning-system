# 资源生成任务恢复状态任务

## TASK-1 文档和范围固定

- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- [x] 明确 `lastError` 使用安全错误码。

## TASK-2 RED 测试

- [x] 增加 migration 文本测试。
- [x] 增加资源生成失败恢复状态测试。
- [x] 运行并确认 RED。

## TASK-3 GREEN 实现

- [x] `ResourceGenerationTask` 增加字段和默认值。
- [x] `ResourceGenerationResponse` 增加字段。
- [x] 失败分支写入恢复状态。
- [x] 新增 V11 migration。

## TASK-4 验证和收尾

- [x] 运行 focused 测试。
- [x] 运行相关回归测试。
- [x] 更新 evidence / acceptance / TODO / memory / changelog / retrospective。

## Done Criteria

- 失败任务可通过 Repository 和 GET API 查询恢复状态。
- V11 migration 文本测试覆盖新增列。
- 相关测试通过。
