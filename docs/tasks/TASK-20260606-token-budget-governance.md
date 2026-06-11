# TASK - Token / Cost 预算治理

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260606-token-budget-governance.md`
- SPEC：`docs/specs/SPEC-20260606-token-budget-governance.md`
- 任务编号：TASK-20260606-token-budget-governance

## 2. 目标

完成 P2-4 analytics 层 Token / 成本预算治理闭环的文档、验证、验收和 TODO 状态同步。

## 3. 范围

### 纳入范围

- 补齐 PRD / REQ / SPEC / PLAN / TASK。
- 更新 Context Pack。
- 验证 `/api/analytics/token-budget/governance` 当前实现。
- 创建 Evidence / Acceptance / Retrospective。
- 更新 TODO、Changelog 和 Memory。

### 排除范围

- 不新增或修改生产代码，除非测试暴露缺陷。
- 不实现调用前预算门禁。
- 不接入真实模型 provider。
- 不新增数据库迁移。
- 不修改前端。

## 4. 允许修改的文件

- `docs/product/PRD-20260606-token-budget-governance.md`
- `docs/requirements/REQ-20260606-token-budget-governance.md`
- `docs/specs/SPEC-20260606-token-budget-governance.md`
- `docs/plans/PLAN-20260606-token-budget-governance.md`
- `docs/tasks/TASK-20260606-token-budget-governance.md`
- `docs/context/CONTEXT-20260606-token-budget-governance.md`
- `docs/evidence/EVIDENCE-20260606-token-budget-governance.md`
- `docs/acceptance/ACCEPT-20260606-token-budget-governance.md`
- `docs/retrospectives/RETRO-20260606-token-budget-governance.md`
- `docs/subagents/runs/RUN-20260606-backend-architecture-cost-model-observability-architect.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 5. 禁止修改的文件

- `backend/src/main/resources/db/migration/**`
- `backend/pom.xml`
- `frontend/**`
- `docs/superpowers/**`
- 与本任务无关的生产代码

## 6. 实施步骤

1. 补齐 workflow 文档。
2. 运行 `AnalyticsControllerTest` 验证现有实现。
3. 基于测试结果创建 Evidence 和 Acceptance。
4. 将 P2-4 三个未勾选项同步为完成。
5. 更新 changelog 和 memory。
6. 保留 P3 预算门禁、真实模型接入、安全和可观测性缺口。

## 7. 测试命令

```bash
cd backend; mvn "-Dtest=AnalyticsControllerTest" test
```

## 8. 完成标准

- [x] PRD / REQ / SPEC / PLAN / TASK 存在。
- [x] Context Pack 存在并反映实际 subagent 分析。
- [x] 聚合维度、预算决策、高成本告警、异常模型调用已通过测试验证。
- [x] Evidence 文档创建。
- [x] Acceptance 文档创建。
- [ ] TODO、Changelog、Memory 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 进行中 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 待定 |
