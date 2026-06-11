# 资源生成任务恢复状态复盘

## 1. 做对的事

- 先写 RED 测试，证明实体和 migration 缺口真实存在。
- 将 `lastError` 设计为安全错误码，避免任务详情接口扩散 provider 原始错误。
- 沿用现有 JPA/Flyway/ControllerTest 风格，没有新增依赖。
- 保持本 slice 范围小，只补齐恢复元数据，不引入自动调度器。

## 2. 发现的问题

- 资源生成任务此前依赖 `agent_task` 和 trace 承载失败信息，业务任务表自身无法直接支持恢复筛选。
- `retryCount` 需要区分业务恢复次数和模型网关内部 attempt 次数，否则后续调度策略容易混乱。

## 3. 后续建议

- 为资源生成恢复任务增加后台扫描和 retry limit。
- 为失败 trace 的 public summary 做统一脱敏策略。
- 将恢复状态接入教师端或 admin trace dashboard。

## 4. Skill Extraction

不新增项目专属 skill。本 slice 可继续复用 `agent-trace-governance`、`spring-ai-agent-backend` 和 `learning-resource-agent`。
