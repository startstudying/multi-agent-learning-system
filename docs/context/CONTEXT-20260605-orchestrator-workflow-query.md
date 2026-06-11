# Context Pack - Orchestrator Workflow 查询与状态上下文收敛

## 当前任务

实现 TODO P0-1 的 Orchestrator Workflow 查询与状态上下文收敛：扩展 create 响应、新增 GET 查询、统一 missing workflow 错误。

## 相关记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`

## 相关文档

- PRD：`docs/product/PRD-20260605-orchestrator-workflow-query.md`
- REQ：`docs/requirements/REQ-20260605-orchestrator-workflow-query.md`
- SPEC：`docs/specs/SPEC-20260605-orchestrator-workflow-query.md`
- PLAN：`docs/plans/PLAN-20260605-orchestrator-workflow-query.md`
- TASK：`docs/tasks/TASK-20260605-orchestrator-workflow-query.md`
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 测试命令：`docs/harness/TEST_COMMANDS.md`

## 已选 Skills

- `feature-development-workflow`：执行项目指定完整开发工作流。
- `test-driven-development`：先写失败测试再实现。
- `spring-ai-agent-backend`：保持 Spring Boot 后端分层。
- `agent-trace-governance`：聚合 Agent task/trace 查询上下文。
- 项目内：`spring-boot-architecture`、`api-contract-design`、`agent-workflow-design`、`agent-trace-design`、`test-generator`。

## Subagent 计划

### 是否启用 Subagent

否，不启用额外并行 worker。

### 原因

当前会话本身是后端 worker，任务边界限定在 Orchestrator 查询与 trace 聚合。涉及 Agent Trace 语义，但不改 Agent 执行链路、不改 RAG、不改 DB，因此采用单 worker 实现，并用相关 Agent Trace skill 做设计校验。

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 1 个主模块，1 个最小 repository 查询 | 是：只读 trace | 否 |

### 选中的专家

- 后端专家：由当前后端 worker 承担。
- Agent/RAG 专家：由 `agent-trace-design` 规则校验承担。

### 并行级别

- [x] L1 - 分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

单 Codex / 单后端 worker。

## 相关代码区域

- `backend/src/main/java/com/learningos/orchestrator/**`
- `backend/src/test/java/com/learningos/orchestrator/**`
- `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java`

## 允许修改的文件

- `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowResponse.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowStepResponse.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowTraceSummary.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java`
- `docs/api/contract.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/evidence/EVIDENCE-20260605-orchestrator-workflow-query.md`
- `docs/acceptance/ACCEPT-20260605-orchestrator-workflow-query.md`
- 本任务 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 文档

## 禁止修改的文件

- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- `backend/pom.xml`
- 非 Orchestrator 的 Controller/Service
- Agent domain/entity 文件

## 测试命令

```bash
cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

## 任务边界

只完成 `TASK-20260605-orchestrator-workflow-query`。不实现 P0-2 状态机、不实现 P0-3 重试恢复、不实现 P0-4 Review Gate 强化。

