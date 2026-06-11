# RUN-20260610 P3-4 broader forged-id / business-object penetration matrix resource-agent

## 范围

- 子任务：P3-4 broader forged-id / business-object penetration matrix
- 聚焦对象：ResourceGeneration / Orchestrator / AgentTask / AgentTrace
- 场景：forged `workflowId` status / retry
- 写入边界：
  - `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
  - `docs/subagents/runs/RUN-20260610-p3-4-broader-forged-id-business-object-penetration-matrix-resource-agent.md`

## 新增测试

1. `workflowStatusRejectsBearerNonOwnerForgedWorkflowIdWithoutLeakingMetadata`
   - owner `alice` 创建 `RESOURCE_GENERATION` workflow。
   - attacker `bob` 使用自己的 Bearer token 并伪造 `X-User-Id: alice` 查询该 `workflowId`。
   - 期望返回 `404 NOT_FOUND`，无 `data`，响应体不泄露 `workflowId`、`agentTaskId`、trace id、request id 等元数据。
   - 同时确认未新增 `AgentTask` / `AgentTrace` 副作用。

2. `workflowRetryRejectsBearerNonOwnerForgedWorkflowIdWithoutSideEffects`
   - owner `alice` 创建失败的 `RESOURCE_GENERATION` workflow。
   - attacker `bob` 使用自己的 Bearer token 并伪造 `X-User-Id: alice` retry 该 `workflowId`。
   - 期望返回 `404 NOT_FOUND`，无 `data`，响应体不泄露 `workflowId`。
   - 同时确认未新增 `AgentTask`、`AgentTrace`、`ResourceGenerationTask`、`LearningResource`、`ResourceReview`、`ModelCallLog`、`TokenUsageLog`、`SourceCitation` 副作用。

## 验证结论

- 未修改生产代码。
- 未发现生产缺陷。
- 该切片不需要升级为更大任务；仍建议 P3-4 父项保留未完成，后续继续补更宽业务权限矩阵和 legacy fallback / sensitive URL cleanup。

## 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

## 初次验证结果

- `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`
