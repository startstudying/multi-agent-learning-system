# EVIDENCE-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards

## 1. 任务

P3-4 子任务：Orchestrator workflow forged-id object-oracle guards

## 2. 变更摘要

测试-only 补强 Orchestrator workflow status / retry 的 forged `workflowId` 对象枚举与拒绝无副作用矩阵。

新增测试：

- `workflowStatusRejectsBearerNonOwnerForgedWorkflowIdWithoutLeakingMetadata`
- `workflowRetryRejectsBearerNonOwnerForgedWorkflowIdWithoutSideEffects`

验证点：

- owner `alice` 创建 `RESOURCE_GENERATION` workflow 后，attacker `bob` 使用自己的 Bearer token 并伪造 `X-User-Id: alice` 查询该 `workflowId`，返回 `NOT_FOUND`，无 `data`，不泄露 workflowId、agentTaskId、traceId、requestId。
- owner `alice` 创建失败的 `RESOURCE_GENERATION` workflow 后，attacker `bob` 使用自己的 Bearer token 并伪造 `X-User-Id: alice` retry 该 `workflowId`，返回 `NOT_FOUND`，无 `data`，不泄露 workflow 元数据。
- forbidden retry 不新增 `AgentTask`、`AgentTrace`、`ResourceGenerationTask`、`LearningResource`、`ResourceReview`、`ModelCallLog`、`TokenUsageLog` 或 `SourceCitation`。

## 3. 修改文件

- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards.md`
- `docs/subagents/runs/RUN-20260610-p3-4-broader-forged-id-business-object-penetration-matrix-resource-agent.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

结果：

- `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,AgentTraceControllerTest test
```

结果：

- `Tests run: 73, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 572, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 仅补测试，未改 Controller / Service / Repository 分层。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG runtime。 |
| Security | PASS | 补强 workflow forged-id / object oracle / spoofed header 测试；未新增 secret。 |
| API / Database | PASS | 未改 API contract 或 schema。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| Orchestrator workflow status forged-id 防枚举测试已补齐 | PASS |
| Orchestrator workflow retry forged-id 防枚举测试已补齐 | PASS |
| forbidden retry 无业务、trace、model/token/citation 副作用 | PASS |
| 无生产代码、API、DTO、DB、依赖、前端变更 | PASS |
| focused / adjacent / full backend 验证完成 | PASS |

最终结论：PASS。P3-4 父项仍保持 open。
