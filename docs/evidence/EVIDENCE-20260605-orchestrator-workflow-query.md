# 证据文档 - Orchestrator Workflow 查询与状态上下文收敛

## 1. 追踪

- TASK：`docs/tasks/TASK-20260605-orchestrator-workflow-query.md`
- SPEC：`docs/specs/SPEC-20260605-orchestrator-workflow-query.md`
- 日期：2026-06-05

## 2. 实现内容

本次实现扩展 Orchestrator Workflow 创建响应，新增 workflow 查询接口，并从 `agent_task` 与 `agent_trace` 聚合状态上下文。查询接口限定当前用户，缺失或不可访问的 `workflowId` 通过统一 `ApiException(NOT_FOUND)` 返回。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java` | 更新 | 新增 `GET /api/orchestrator/workflows/{workflowId}` |
| `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` | 更新 | 聚合 workflow 状态、trace steps、recent failed step、trace summary、next actions |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowResponse.java` | 更新 | 响应追加 workflow 查询上下文字段 |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowStepResponse.java` | 新增 | workflow step DTO |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowTraceSummary.java` | 新增 | trace summary DTO |
| `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java` | 更新 | 新增 owner + inputJson marker 查询方法 |
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 更新 | 覆盖 create response、create 后 get、missing workflow |

## 4. 测试结果

### TDD RED

```bash
cd backend; mvn clean "-Dtest=OrchestratorWorkflowControllerTest" test
```

结果：失败符合预期。失败点包括：

- create 响应缺少 `$.data.steps`
- GET workflow 尚无映射，返回 500
- missing workflow 未返回 404 `NOT_FOUND`

### GREEN

```bash
cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

结果：通过。

| 命令 | 结果 | 备注 |
|---|---|---|
| `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test` | 通过 | Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 |

## 5. 架构漂移检查

- [x] 已通过：Controller 只做 HTTP 委托，业务聚合在 Service。
- [x] 已通过：未新增依赖、未新增数据库迁移、未修改前端。
- [x] 已通过：创建 workflow 继续写 Agent Trace，查询接口只读 trace。

## 6. 日志说明

测试期间出现 Mockito 动态 agent warning，为现有测试依赖行为，不影响本任务结果。测试过程中曾有一次被并行 Analytics 编译改动短暂阻塞，随后相关文件稳定后目标测试通过；本任务未修改 Analytics 文件。

## 7. 已知限制

- workflowId 当前通过 `agent_task.inputJson` envelope marker 查询，后续若要支持复杂查询、索引或恢复，应引入独立 workflow 状态表或持久化上下文字段。
- 本任务不实现 retry/cancel/recovery，只返回可继续动作标识。

## 8. 评审备注

- `workflowId` 不存在和不属于当前用户统一返回 `NOT_FOUND`，避免泄露资源存在性。

