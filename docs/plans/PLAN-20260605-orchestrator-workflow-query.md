# PLAN - Orchestrator Workflow 查询与状态上下文收敛

## 1. 追踪

- PRD：`docs/product/PRD-20260605-orchestrator-workflow-query.md`
- REQ：`docs/requirements/REQ-20260605-orchestrator-workflow-query.md`
- SPEC：`docs/specs/SPEC-20260605-orchestrator-workflow-query.md`

## 2. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 更新控制器测试，覆盖 create 后 get 与 missing workflow | TASK-01 | 待办 |
| 2 | 扩展 DTO 与 Service 查询聚合逻辑 | TASK-01 | 待办 |
| 3 | 暴露 GET endpoint，保持 Controller 轻薄 | TASK-01 | 待办 |
| 4 | 运行目标测试并记录证据与验收 | TASK-01 | 待办 |

## 3. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 更新 | 1 | 后端 worker |
| `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java` | 更新 | 3 | 后端 worker |
| `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` | 更新 | 2 | 后端 worker |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowResponse.java` | 更新 | 2 | 后端 worker |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowStepResponse.java` | 新增 | 2 | 后端 worker |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowTraceSummary.java` | 新增 | 2 | 后端 worker |
| `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java` | 最小更新 | 2 | 后端 worker |

## 4. 依赖

- 前置条件：已有 `agent_task` 和 `agent_trace` JPA 实体与 repository。
- 新增依赖：无。

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| workflowId 当前只在 `inputJson` 中 | 查询依赖 JSON 字符串匹配 | 使用带引号的 marker 降低误匹配；后续可引入 workflow 表 |
| API 响应字段新增 | 前端需要适配新字段 | 保持原字段不变，只追加字段 |
| 其他 worker 并行修改 | 文件冲突 | 只改 Context Pack 允许文件，不 revert 他人改动 |

## 6. 回滚策略

回滚 Orchestrator DTO、Service、Controller 和测试改动即可；无数据库迁移和依赖变更。

## 7. 测试策略

```bash
cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

## 8. Subagent 计划

| 专家 | 是否需要 | 职责 |
|---|---|---|
| 后端专家 | 是 | API 与 Service 实现 |
| Agent/RAG 专家 | 是 | Trace 聚合语义检查 |
| 安全与质量 | 否 | 无权限、DB、依赖扩展；当前用户限定由后端实现覆盖 |
| 集成评审员 | 否 | 单模块最小变更 |

并行级别：L1 分析。当前会话已经是后端 worker，编码采取单 Codex 实现，不启用并行实现。

## 9. 架构漂移检查

实施前检查：

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 委托 Service |
| Frontend rules | PASS | 不涉及前端 |
| Agent / RAG rules | PASS | 创建继续写 trace，查询只读 trace |
| Security | PASS | 不新增依赖和 secrets |
| API / Database | PASS | SPEC 已记录 API；无 DB 变更 |

