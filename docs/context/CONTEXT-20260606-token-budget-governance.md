# Context Pack - P2-4 Token/Cost Budget Governance

## 当前任务

实现 TODO P2-4 后端最小切片：基于现有 `model_call_log`、`token_usage_log`、`agent_task` 和资源生成任务记录，提供 Token/成本预算治理统计、预算风险决策、高成本任务告警和异常模型调用识别。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 关联文档

- `docs/planning/backend-architecture-todolist.md` 中 P2-4
- `docs/specs/SPEC-20260605-analytics-summary.md`
- `docs/context/CONTEXT-20260605-analytics-summary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

## 已选 Skills

- `feature-development-workflow`
- `spring-ai-agent-backend`
- `agent-trace-governance`
- `test-driven-development`
- `architecture-drift-check`
- `security-review`

## Subagent 计划

### 是否启用 Subagent

否。

### 原因

本轮由 Worker B 执行已拆分的后端切片，文件范围明确，默认单 Codex 实现；不再创建额外并行实现者，避免共享工作区冲突。

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 2 | 是 | 是 |

### 选中的专家

- Backend Expert
- Agent/RAG Expert
- Security & Quality

### 并行级别

- [x] L1 - 仅分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

单 Codex 实现。

### 文件归属

| 领域 | 负责专家 | 允许修改的文件 |
|---|---|---|
| 后端 analytics | Backend Expert | `backend/src/main/java/com/learningos/analytics/**` |
| 后端 agent 支撑 | Agent/RAG Expert | `backend/src/main/java/com/learningos/agent/**` 中预算统计必要支撑 |
| 测试 | Security & Quality | `backend/src/test/java/com/learningos/analytics/**`、必要的 agent 相关测试 |
| 交付证据 | Integration Reviewer | `docs/context/**`、`docs/evidence/**`、`docs/acceptance/**` |

## 关联代码区域

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/main/java/com/learningos/agent/domain/AgentTask.java`
- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
- `backend/src/main/java/com/learningos/agent/domain/TokenUsageLog.java`
- `backend/src/main/java/com/learningos/agent/repository/*`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

## 允许修改的文件

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/context/CONTEXT-20260606-token-budget-governance.md`
- `docs/evidence/EVIDENCE-20260606-token-budget-governance.md`
- `docs/acceptance/ACCEPT-20260606-token-budget-governance.md`

## 禁止修改的文件

- `backend/src/main/resources/db/migration/**`，除非确认必须新增表/列
- RAG evaluation scripts
- migration smoke files
- frontend 目录
- docs/superpowers 归档目录
- 未列入本 Context Pack 的其他模块文件

## 测试命令

```bash
cd backend && mvn "-Dtest=AnalyticsControllerTest" test
```

必要时补充：

```bash
cd backend && mvn test
```

## 任务边界

仅完成 P2-4 的后端最小治理面：统计维度、预算决策、高成本任务和异常模型调用识别。不接入真实模型供应商，不新增依赖，不新增数据库结构，不修改 RAG evaluation 或 migration smoke。
