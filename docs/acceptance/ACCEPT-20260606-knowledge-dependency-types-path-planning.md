# Knowledge DAG 依赖类型与路径规划验收报告

## 1. 追踪

- PRD：`docs/product/PRD-20260606-knowledge-dependency-types-path-planning.md`
- REQ：`docs/requirements/REQ-20260606-knowledge-dependency-types-path-planning.md`
- SPEC：`docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md`
- 证据：`docs/evidence/EVIDENCE-20260606-knowledge-dependency-types-path-planning.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：`dependencyType` 只允许 `PREREQUISITE`、`RELATED`、`ADVANCED`。
- [x] FR-02：非法 `dependencyType` 返回 HTTP 400 与 `VALIDATION_ERROR`。
- [x] FR-03：合法依赖创建响应返回规范化后的 `dependencyType`。
- [x] FR-04：`RELATED` 依赖可保存，但不参与路径前置锁定。
- [x] FR-05：`ADVANCED` 依赖可保存，但不参与路径前置锁定。
- [x] FR-06：`PREREQUISITE` 仍参与路径排序和未满足前置时的 `LOCKED` 判断。

### 架构验收

- [x] Controller 保持薄路由。
- [x] 业务校验位于 Service 层。
- [x] 路径规划策略位于 `LearningWorkflowService`。
- [x] 未新增数据库迁移。
- [x] 未新增依赖。
- [x] 未修改前端。

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
| RED：非法 `dependencyType` | PASS | 修复前返回 200，测试失败在目标行为上 |
| RED：`RELATED/ADVANCED` 路径规划 | PASS | 修复前被重排并锁定，测试失败在目标行为上 |
| GREEN：相关后端测试 | PASS | `CourseKnowledgeControllerTest,LearningWorkflowControllerTest` 共 6 个测试通过 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 依赖创建接口缺少课程教师权限边界 | High | P3-4 权限与安全加固 |
| 掌握度阈值补救策略未实现 | Medium | P1-2 后续切片 |
| 路径节点推荐原因、预计时长、资源类型和测评绑定未扩展 | Medium | P1-2 后续切片 |
| 历史非法依赖类型未清洗 | Low | 数据治理/迁移清洗任务 |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Codex | 2026-06-06 | 通过 |
