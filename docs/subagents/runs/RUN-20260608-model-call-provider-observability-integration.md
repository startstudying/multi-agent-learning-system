# RUN-20260608 P3-3-B model_call_log.provider integration review

## 范围

P3-3-B：`model_call_log.provider` schema / entity / recorder / gateway provider observability。

## 专家输入摘要

### DB/Migration Expert

- 当前 Flyway 最新为 V17，MySQL smoke 版本和数量硬编码为 `17`。
- `model_call_log` 在 V1/V2 创建，V12 只补 prompt metadata，没有 provider。
- 建议新增 `V18__model_call_provider_observability.sql`。
- 建议 `provider varchar(80) not null default 'none'`，兼容历史数据。
- 必须同步 `SchemaConvergenceMigrationTest` 和 `MysqlMigrationSmokeTest`。

### Security & Quality Expert

- provider 字段属于安全观测字段，不能直接写配置原文。
- 风险包括 API key、endpoint、tenant、deployment path、高基数字符串进入 DB / metrics。
- 必须低基数白名单归一化：`none/openai/dashscope/anthropic/gemini/mock/other`。
- 失败路径不能从 raw exception message 解析 provider。
- `errorMessage` 仍只允许安全错误码。

### Test Integration Reviewer

- 现有测试覆盖 gateway response provider 和 metrics，但没有 DB provider 持久化断言。
- RED 顺序建议：
  1. migration 文本测试；
  2. recorder 成功 provider 测试；
  3. gateway unknown provider 归一化测试；
  4. failure provider 传递测试；
  5. MySQL smoke V18 更新。

## 冲突解决

| Conflict | Resolution |
|---|---|
| DB Expert 建议最小 blank -> `none`，Security Expert 建议白名单 | 采用白名单，unknown -> `other`，同时 null/blank -> `none`。 |
| 是否给 provider 建索引 | 本切片不加索引，避免未验证查询模式；后续 analytics 真实高频查询再补。 |
| 是否修改 `recordFailure` 原签名 | 保留旧签名，新增 provider overload，降低调用点风险。 |
| 是否接真实 provider | 不接；真实 provider adapter 是 P3-3-C。 |

## 最终方案

1. 新增 V18 provider column。
2. `ModelCallLog` entity 增加 provider 默认值。
3. `AiModelGateway` provider 统一安全归一化。
4. `AgentRunRecorder` 成功/失败模型日志写 provider。
5. 测试覆盖 RED/GREEN 与 MySQL smoke。

## Implementation Mode

主 Codex 串行实现；不并行修改文件。

