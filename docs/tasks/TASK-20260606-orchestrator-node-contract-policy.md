# Orchestrator 节点契约与策略显式化任务

## 1. 允许修改

- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowStepResponse.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/product/PRD-20260606-orchestrator-node-contract-policy.md`
- `docs/requirements/REQ-20260606-orchestrator-node-contract-policy.md`
- `docs/specs/SPEC-20260606-orchestrator-node-contract-policy.md`
- `docs/plans/PLAN-20260606-orchestrator-node-contract-policy.md`
- `docs/tasks/TASK-20260606-orchestrator-node-contract-policy.md`
- `docs/context/CONTEXT-20260606-orchestrator-node-contract-policy.md`
- `docs/subagents/runs/RUN-20260606-orchestrator-node-contract-policy.md`
- 完成阶段的 evidence / acceptance / memory / changelog / TODO / retrospective

## 2. 禁止修改

- RAG index、document upload、assessment 业务逻辑。
- 数据库迁移。
- 前端页面。
- 新增外部依赖。

## 3. 待办

- [x] 建立本切片文档。
- [x] 写 RED 测试并确认失败原因正确。
- [x] 实现 DTO 字段和 Service 映射。
- [x] 运行聚焦测试。
- [x] 运行相关回归测试。
- [x] 更新证据、验收、TODO、记忆、changelog、复盘。

## 4. Done Criteria

- `steps[]` 和 `recentFailedStep` 都返回节点契约字段。
- 资源生成失败 workflow 显示 endpoint retry 能力。
- RAG/答题失败 workflow 显示重新提交原始请求策略。
- 验证命令通过，或清楚记录失败原因。
