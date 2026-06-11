# TASK-20260611 后端架构后续增强计划收口

## 目标

完成 `docs/planning/backend-architecture-todolist.md` 后续增强计划的事实收口：保存专家报告、补齐 external smoke 实测化、同步已完成状态，并形成证据与验收。

## Context Pack

### Related Memory / Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/plans/PLAN-20260611-backend-followup-enhancements-epic.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

### Selected Skills

- feature-development-workflow
- multi-agent-coder
- educational-rag-pipeline
- security-review
- verification-before-completion

### Subagent Plan

- Agent/RAG Expert：P3-2 Parser/VectorDB 后续增强分析。
- Backend Expert：P3-3 Provider 与 P3-5/P2-4 后端实现分析。
- Security & Quality：权限、依赖、外呼、凭证、VectorDB 风险分析。

### Allowed Files

- `backend/src/test/java/com/learningos/rag/vector/QdrantVectorExternalSmokeTest.java`
- `backend/src/test/java/com/learningos/agent/application/ModelProviderExternalSmokeTest.java`
- `docs/subagents/runs/RUN-20260611-backend-architecture-*.md`
- `docs/product/PRD-20260611-backend-followup-plan-completion.md`
- `docs/requirements/REQ-20260611-backend-followup-plan-completion.md`
- `docs/specs/SPEC-20260611-backend-followup-plan-completion.md`
- `docs/plans/PLAN-20260611-backend-followup-plan-completion.md`
- `docs/tasks/TASK-20260611-backend-followup-plan-completion.md`
- `docs/context/CONTEXT-20260611-backend-followup-plan-completion.md`
- `docs/evidence/EVIDENCE-20260611-backend-followup-plan-completion.md`
- `docs/acceptance/ACCEPT-20260611-backend-followup-plan-completion.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

### Disallowed Files

- 前端源码。
- 生产 Java 代码。
- DB migration。
- Maven dependency。
- secrets / env 文件。

### Test Commands

```powershell
cd backend
mvn --% -Dtest=QdrantVectorExternalSmokeTest,ModelProviderExternalSmokeTest,QdrantVectorIndexAdapterTest,RagVectorConfigurationTest,EmbeddingServiceTest,AiModelGatewayTest,TokenBudgetGateServiceTest,BusinessPermissionMatrixRegressionTest test
mvn test
```

### Acceptance Criteria

- [x] 专家报告与集成评审落盘。
- [x] Qdrant external smoke 不是占位测试，默认跳过，opt-in 时真实检查 collection/dimension。
- [x] Model Provider external smoke 不是占位测试，默认跳过，opt-in 时真实调用 OpenAI-compatible chat endpoint。
- [x] 聚焦测试通过。
- [ ] 全量 backend 测试通过。
- [x] 计划、证据、验收、memory、changelog 更新。
