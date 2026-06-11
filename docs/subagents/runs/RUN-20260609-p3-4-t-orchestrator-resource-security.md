# RUN - P3-4-T Orchestrator RESOURCE_GENERATION Security Review

## 风险等级

HIGH - Broken Access Control。

## Exploit Path

1. 攻击者持有有效 Bearer JWT：`sub=admin`，`roles=["USER"]`。
2. 请求 `POST /api/orchestrator/workflows`，`workflowType=RESOURCE_GENERATION`，`learnerId=admin`，`payloadJson.goalId` 指向一个已存在 course。
3. `DevAuthFilter` 正确建立 `UserContext(userId=admin, roles=[USER])`。
4. Orchestrator 丢弃 roles，只传 `ownerUserId=admin`。
5. `resourceGenerationRequest(...)` 的 owner-only 检查通过。
6. legacy `ResourceGenerationService.createTaskInWorkflow(...)` 调用 `isAdmin("admin")`。
7. Course enrollment 检查被 subject-name admin bypass，普通 USER 可生成未 enrolled course 的资源并触发模型调用。

## 必须保留的安全语义

- Bearer roles 优先于 `X-User-Id`。
- `USER sub=admin` / `USER sub=teacher_1` 不得被 subject-name 推断成 admin/teacher。
- ResourceGeneration 保持 owner-only，admin/teacher 不新增代创建能力。
- course-bound `RESOURCE_GENERATION` 对普通 learner 要求 active enrollment。
- forbidden 请求不得写入 `resource_generation_task`、`learning_resource`、`resource_review`、`source_citation`、`model_call_log`、`token_usage_log`。

## Side Effects 策略

本切片采用当前 Orchestrator failure evidence 设计：

- owner mismatch / payload validation 在 workflow start 前拒绝，不能创建 `agent_task` / `agent_trace`。
- downstream course enrollment denied 发生在 workflow start 后，可保留一个 `FAILED agent_task` 和脱敏 `workflow_start + step_runtime_failure` evidence。
- 无论如何不能创建 ResourceGeneration 业务副作用或模型/token/citation副作用。

## Remaining Risk

`ResourceGenerationService.getTask/getTrace` 以及若干 legacy service overload 仍存在 subject-name inference 兼容路径；本切片只关闭 Orchestrator `RESOURCE_GENERATION` create/retry 调用链。

