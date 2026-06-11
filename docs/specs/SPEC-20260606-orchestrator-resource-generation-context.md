# Orchestrator Resource Generation 上下文统一规格

## API 行为

### 创建 workflow

```http
POST /api/orchestrator/workflows
```

当 `workflowType=RESOURCE_GENERATION`：

1. Orchestrator 创建 workflow envelope 和一个 `agent_task`。
2. 写入第 1 个 trace step：`workflow_start`。
3. Orchestrator 解析 `payloadJson` 为资源生成请求。
4. 调用资源生成服务的 workflow-aware 方法，并传入已有 `AgentExecutionContext`。
5. 资源生成服务不再新建 `agent_task`，而是在同一 `agentTaskId/traceId` 下追加 Planner、Teacher、Resource、Question、Critic、Tutor、Safety 等步骤。
6. 任务最终进入 `WAITING_REVIEW`，等待 Critic/教师审核。

## 状态规则

```text
RUNNING
  -> WAITING_REVIEW 资源草稿生成完成，等待审核
  -> FAILED         模型或生成流程失败
```

`GET /api/orchestrator/workflows/{workflowId}` 读取同一个 `agent_task` 状态，因此应返回资源生成后的实际状态。

## Trace 规则

同一个 `agent_task` 下，`sequenceNo` 必须递增且不重复：

```text
1 workflow_start / Orchestrator / RUNNING
2 step_planner   / PlannerAgent / DONE
3 step_teacher   / TeacherAgent / DONE
4 step_resource  / ResourceAgent / DONE
5 step_question  / QuestionAgent / DONE
6 step_critic    / CriticAgent / PENDING
7 step_tutor     / TutorAgent / WAITING
8 step_safety    / SafetyAgent / DONE
```

为支持追加 trace，`AgentRunRecorder.recordTraceSteps(...)` 应基于已有 trace 数量计算起始 `sequenceNo`。

## 服务边界

- `OrchestratorWorkflowService` 负责 workflow envelope、workflow trace 起点、payload 解析和编排。
- `ResourceGenerationService` 负责资源生成业务、权限校验、安全检查、资源和审核记录持久化。
- `AgentRunRecorder` 负责 Agent Task/Trace 状态与序号治理。
- Controller 不承载业务逻辑。

## 兼容性

- 直接资源生成接口保持原行为：仍自行创建 `agent_task`，trace 从资源生成步骤开始。
- 本轮不改变数据库 schema。
- 本轮不改变 Review Gate 规则。
