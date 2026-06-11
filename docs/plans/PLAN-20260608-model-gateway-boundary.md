# PLAN-20260608 模型网关结构化校验与日志补齐

## 执行计划

Status: Done

1. [x] 收集 Model Boundary、Security & Quality、Test/Integration 三份专家分析并落盘。
2. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT，将当前边界切换到 P3-3-A。
3. [x] TDD RED：在 `AiModelGatewayTest`、`AgentRunRecorderTest`、`ResourceGenerationControllerTest` 增加失败用例并确认失败。
4. [x] GREEN：最小修改 `AiModelGateway`、`AgentRunRecorder`、`ResourceGenerationService`。
5. [x] 运行聚焦测试：`mvn --% -Dtest=AiModelGatewayTest test`。
6. [x] 运行集成测试：`mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test`。
7. [x] 运行相邻回归：`mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test`。
8. [x] 可行时运行完整后端测试：`mvn test`。
9. [x] 更新 Evidence / Acceptance / Changelog / Memory / Retrospective / Skill。

## 验证结果

- `mvn --% -Dtest=AiModelGatewayTest test`：BUILD SUCCESS；8 tests，0 failures，0 errors，0 skipped。
- `mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test`：BUILD SUCCESS；24 tests，0 failures，0 errors，0 skipped。
- `mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test`：BUILD SUCCESS；75 tests，0 failures，0 errors，0 skipped。
- `mvn test`：BUILD SUCCESS；275 tests，0 failures，0 errors，1 skipped。

## 风险

| Risk | Mitigation |
|---|---|
| 不改 DB schema 导致 provider 无法持久化到 `model_call_log` | 文档明确 provider 本轮仅在 gateway response / metrics 覆盖，DB provider 字段留给后续切片 |
| 结构化校验影响现有 placeholder 测试 | 只对 `agent-resource-v1` 执行 schema 校验，并让 resource prompt 的 placeholder 输出合法 schema |
| raw provider error 经异常 message 泄露 | gateway 最终抛出的 message 改为安全错误码；recorder 参数也传安全错误码 |
