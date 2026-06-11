# Knowledge DAG 掌握度阈值补救优先验收报告

## 1. 追踪

- PRD：`docs/product/PRD-20260606-knowledge-mastery-threshold-remediation.md`
- REQ：`docs/requirements/REQ-20260606-knowledge-mastery-threshold-remediation.md`
- SPEC：`docs/specs/SPEC-20260606-knowledge-mastery-threshold-remediation.md`
- 证据：`docs/evidence/EVIDENCE-20260606-knowledge-mastery-threshold-remediation.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：系统定义独立 `REMEDIATION_THRESHOLD = 0.60`。
- [x] FR-02：`COMPLETION_THRESHOLD = 0.80` 仍用于 `DONE` 和前置依赖满足判断。
- [x] FR-03：低掌握度且有下游依赖的前置知识点优先于普通可学习节点。
- [x] FR-04：补救优先节点仍为 `ACTIVE`，不新增状态。
- [x] FR-05：下游节点在前置未达 `0.80` 时仍为 `LOCKED`。
- [x] FR-06：`reasonSummary` 包含补救阈值和下游解锁说明。
- [x] FR-07：`RELATED` / `ADVANCED` 不参与补救优先判断。

### 架构验收

- [x] 只修改路径规划 Service 和测试。
- [x] 未新增数据库迁移。
- [x] 未新增依赖。
- [x] 未修改前端。
- [x] 未修改 Assessment 掌握度更新算法。

### 文档验收

- [x] PRD 已创建。
- [x] REQ 已创建。
- [x] SPEC 已创建。
- [x] PLAN 已创建。
- [x] TASK 已创建。
- [x] Context Pack 已创建。
- [x] Evidence 已创建。
- [x] Subagent run 已创建。
- [x] Memory 和 Changelog 已更新。

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| RED：低掌握度前置知识未优先 | PASS | 修复前失败在节点顺序 |
| GREEN：LearningWorkflowControllerTest | PASS | 5 个测试通过 |
| 相关回归：CourseKnowledgeControllerTest + LearningWorkflowControllerTest | PASS | 7 个测试通过 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 路径节点缺少推荐资源、预计时长、资源类型和测评绑定 | Medium | P1-2 后续切片 |
| 没有独立 `recommendedNext` 字段，只能通过排序表达优先级 | Medium | 路径节点字段扩展 |
| 当前补救优先只考虑直接下游依赖 | Low | 后续多跳 DAG 优先级优化 |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Codex | 2026-06-06 | 通过 |
