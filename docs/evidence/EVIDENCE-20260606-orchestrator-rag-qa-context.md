# Orchestrator RAG_QA 上下文收敛证据

## 范围

本轮完成 P0-1 的 `RAG_QA` 最小切片：

- `POST /api/orchestrator/workflows` 支持 `workflowType=RAG_QA`
- Orchestrator 复用同一 `workflowId / agentTaskId / traceId`
- RAG 查询通过显式 `traceId` 入口落库
- 同一 `agent_task` 下追加 `workflow_start + step_rag_safety + step_rag_retrieval + step_rag_answer`
- 无效 payload 在创建 task 之前拒绝

## 代码证据

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
  - 新增 `RAG_QA` payload 解析
  - 调用 `RagQueryService.queryWithTraceId(...)`
  - 追加 RAG trace steps
  - 将 workflow task 转为 `DONE`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
  - 新增 `queryWithTraceId(...)`
  - `kb_query_log.traceId` 和 `source_citation.traceId` 复用外部 traceId
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
  - 覆盖有来源、无来源、GET、无效 payload 四条路径
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
  - 覆盖外部 traceId 落库

## TDD 记录

### RED

首次聚焦测试失败于编译阶段，失败原因正是缺少 `RagQueryService.queryWithTraceId(...)`。

命令：

```bash
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest" test
```

### GREEN

补齐实现后，聚焦测试通过。

命令：

```bash
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest" test
```

结果：

- `Tests run: 16`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

### FULL

全量后端测试通过。

命令：

```bash
cd backend
mvn test
```

结果：

- `Tests run: 102`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

## 验证覆盖

| 项目 | 结果 | 说明 |
|---|---|---|
| `RAG_QA` workflow 返回 `DONE` | PASS | Orchestrator 集成测试覆盖 |
| `workflowId / agentTaskId / traceId` 一致 | PASS | response 与数据库一致 |
| `kb_query_log.traceId` 复用 Orchestrator traceId | PASS | RAG service 单测 + 集成测试覆盖 |
| `source_citation.traceId` 复用 Orchestrator traceId | PASS | 有来源路径覆盖 |
| 无来源 workflow 不写 citation | PASS | no-source 路径覆盖 |
| GET workflow 返回完整 steps / summary | PASS | workflow 查询接口覆盖 |
| 无效 payload 不创建 `agent_task` | PASS | 先校验再 startRun |
| 直接 `query(...)` 行为保持兼容 | PASS | 现有 RAG service 测试全绿 |

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 仍只做转发，业务在 Service |
| Agent / RAG rules | PASS | Agent trace 仍由 Orchestrator 记录 |
| Security | PASS | 权限仍由 RAG service 过滤，不信任前端 kbIds |
| API / Database | PASS | 未新增接口、未改 schema |

## 限制

- 本轮不处理 `ANSWER_SUBMISSION`
- 本轮不处理 workflow retry/recovery
- 运行期权限/安全失败的 durable failed workflow evidence 留给后续切片
