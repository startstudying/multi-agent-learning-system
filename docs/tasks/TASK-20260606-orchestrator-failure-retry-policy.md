# Orchestrator 失败与重试策略任务

## 1. 任务边界

只处理 Orchestrator backend TODO P0-1/P0-3 的本切片：

- 通用 `RuntimeException` 失败证据。
- 最小 retry endpoint。
- 支持 workflow 的失败/重试策略文档。

## 2. 允许修改

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/*`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/*/*orchestrator-failure-retry-policy.md`

## 3. 禁止修改

- RAG document upload/idempotency
- `IndexService`
- `docs/memory/*`
- `docs/changelog/*`
- `docs/planning/backend-architecture-todolist.md`

## 4. 待办

- [x] 创建本切片 workflow 文档。
- [x] 写 RED 测试：RuntimeException evidence。
- [x] 写 RED 测试：retry FAILED workflow。
- [x] 写 RED 测试：retry 非 FAILED workflow 返回 409。
- [x] 实现 service/controller。
- [x] 运行 `mvn "-Dtest=OrchestratorWorkflowControllerTest" test`。
- [x] 创建 Evidence / Acceptance。

## 5. Done Criteria

- RuntimeException 失败后 task/trace/GET workflow 可查。
- retry endpoint 只允许 owner 对 FAILED workflow 操作。
- 非 FAILED workflow retry 返回 409。
- 聚焦测试通过或记录失败原因。
