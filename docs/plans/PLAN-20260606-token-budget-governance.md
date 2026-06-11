# PLAN - Token / Cost 预算治理

## 1. 追踪

- PRD：`docs/product/PRD-20260606-token-budget-governance.md`
- REQ：`docs/requirements/REQ-20260606-token-budget-governance.md`
- SPEC：`docs/specs/SPEC-20260606-token-budget-governance.md`
- Context Pack：`docs/context/CONTEXT-20260606-token-budget-governance.md`

## 2. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 补齐 workflow 文档，明确本切片只覆盖 analytics 治理视图。 | TASK-01 | 完成 |
| 2 | 验证当前后端实现覆盖 P2-4 三个未勾选项。 | TASK-01 | 完成 |
| 3 | 更新 TODO、证据、验收、记忆和 changelog。 | TASK-01 | 待执行 |

## 3. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `docs/product/PRD-20260606-token-budget-governance.md` | 新增 | 1 | Main Codex |
| `docs/requirements/REQ-20260606-token-budget-governance.md` | 新增 | 1 | Main Codex |
| `docs/specs/SPEC-20260606-token-budget-governance.md` | 新增 | 1 | Main Codex |
| `docs/plans/PLAN-20260606-token-budget-governance.md` | 新增 | 1 | Main Codex |
| `docs/tasks/TASK-20260606-token-budget-governance.md` | 新增 | 1 | Main Codex |
| `docs/context/CONTEXT-20260606-token-budget-governance.md` | 更新 | 1 | Main Codex |
| `docs/evidence/EVIDENCE-20260606-token-budget-governance.md` | 新增 | 3 | Main Codex |
| `docs/acceptance/ACCEPT-20260606-token-budget-governance.md` | 新增 | 3 | Main Codex |
| `docs/planning/backend-architecture-todolist.md` | 更新 | 3 | Main Codex |
| `docs/memory/*.md` | 更新 | 3 | Main Codex |
| `docs/changelog/CHANGELOG.md` | 更新 | 3 | Main Codex |

## 4. 依赖

- 前置条件：现有 analytics 代码和测试已存在。
- 新增依赖：无。

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| 将 analytics 治理视图误认为调用前预算门禁 | 后续真实模型成本仍可能失控 | 在 SPEC、TASK、TODO 中明确后续 P3 仍需预算门禁 |
| 当前实现使用 `findAll()` 内存聚合 | 大数据量时性能不足 | P3 生产化再改 repository 聚合查询 |
| `X-User-Id` dev auth 可伪造 | 生产权限风险 | P3-4 RBAC 安全加固单独处理 |

## 6. 回滚策略

本轮主要是文档和状态同步。若发现测试不满足 P2-4，则撤回 TODO 勾选并把缺口恢复为待办。

## 7. 测试策略

```bash
cd backend; mvn "-Dtest=AnalyticsControllerTest" test
```

## 8. Subagent 计划

| 专家 | 是否需要 | 职责 |
|---|---|---|
| Backend Expert / Spec Architect | 是 | 分析 P2-4、P3-3、P3-5 实现边界 |
| Agent/RAG Expert | 是 | 并行分析 RAG evaluation 与 RAG production 缺口 |
| Security & Quality | 是 | 并行审查 MySQL smoke、权限和安全测试缺口 |
| Integration Reviewer | 是 | Main Codex 汇总专家输出并更新计划 |

并行级别：L1 并行分析 / L2 局部设计；本轮不启用 L3 并行实现。

## 9. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 已执行 |
