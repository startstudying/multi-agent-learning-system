# 模型调用 Prompt 元数据验收

## 1. 验收结论

通过。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `model_call_log` 具备 prompt metadata 列 | 通过 | `SchemaConvergenceMigrationTest#v12MigrationAddsModelCallPromptMetadataColumns` |
| 成功模型调用写入 `promptCode` | 通过 | `AgentRunRecorderTest#startsRunAndRecordsTraceModelAndTokenEvidence` |
| 成功模型调用写入 `promptVersion` | 通过 | 同上 |
| 成功模型调用写入 `temperature` | 通过 | 同上 |
| 成功模型调用写入白名单 schema 摘要 | 通过 | 同上 |
| 失败模型调用写入 prompt metadata | 通过 | `AgentRunRecorderTest#recordsFailureByUpdatingTaskAndWritingTraceAndModelError` |
| 失败 provider message 不进入 model log | 通过 | `errorMessage = MODEL_PROVIDER_ERROR` |
| 失败 provider message 不进入 task output / trace summary | 通过 | `doesNotContain("provider timeout")` 和资源生成失败测试 |
| 资源生成集成路径持久化模型日志元数据 | 通过 | `ResourceGenerationControllerTest#createsResourceGenerationTaskAndExposesAgentTrace` |

## 3. 验证命令

- `mvn "-Dtest=AgentRunRecorderTest,SchemaConvergenceMigrationTest" test`
- `mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,PromptVersionServiceTest,PromptVersionControllerTest,AiModelGatewayTest" test`

## 4. 未完成事项

- evaluation set、prompt version 质量对比指标仍未完成。
- PromptVersion API 的角色权限和 `promptText` 暴露策略仍属于后续安全任务。
