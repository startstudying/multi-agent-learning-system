# Knowledge DAG 掌握度阈值补救优先证据

## 1. 追踪

- TASK：`docs/tasks/TASK-20260606-knowledge-mastery-threshold-remediation.md`
- SPEC：`docs/specs/SPEC-20260606-knowledge-mastery-threshold-remediation.md`
- 日期：2026-06-06

## 2. 实现内容

本次补齐 P1-2 的掌握度阈值补救最小切片：

- 新增 `REMEDIATION_THRESHOLD = 0.60`，保留 `COMPLETION_THRESHOLD = 0.80`。
- 路径规划在不违反 `PREREQUISITE` 拓扑约束的前提下，对同一批可学习节点做补救优先排序。
- `mastery < 0.6` 且有下游 `PREREQUISITE` 依赖的前置知识点优先于普通可学习节点。
- 补救优先节点仍使用 `ACTIVE` 状态，避免新增状态影响 analytics 和前端。
- `reasonSummary` 会说明低于 `0.6` 补救阈值，并指出该前置知识会解锁下游知识。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java` | 修改 | 增加补救阈值、下游依赖表、支持 mastery 的拓扑排序和补救 reason |
| `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java` | 修改 | 增加低掌握度前置知识优先补救 RED/GREEN 测试 |
| `docs/product/PRD-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 产品目标和非目标 |
| `docs/requirements/REQ-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 功能、数据边界和验收需求 |
| `docs/specs/SPEC-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 阈值、排序、状态和 reason 规格 |
| `docs/plans/PLAN-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 执行计划和子代理决策 |
| `docs/tasks/TASK-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 任务拆解 |
| `docs/context/CONTEXT-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 修改边界和测试命令 |
| `docs/subagents/runs/RUN-20260606-knowledge-mastery-threshold-remediation.md` | 新增 | 子代理核查结果集成记录 |

## 4. TDD 证据

### RED

执行命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest" test
```

失败结果：

```text
LearningWorkflowControllerTest.prioritizesLowMasteryPrerequisiteForRemediationBeforeOrdinaryReadyNodes
JSON path "$.data.nodes[0].nodeId" expected weak prerequisite but was ordinary entry node
```

说明当前实现只按创建顺序和拓扑约束排序，没有把低掌握度前置知识提前。

### GREEN

执行命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest" test
```

通过结果：

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 相关回归

执行命令：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test
```

通过结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 5. 架构漂移检查

- [x] 未修改 Controller。
- [x] 未修改 DTO。
- [x] 未新增数据库 migration。
- [x] 未新增依赖。
- [x] 未新增前端页面。
- [x] 未扩散到 Assessment 或 Replan 服务。

## 6. 已知限制

- 本切片只通过节点顺序和 `reasonSummary` 表达优先级，没有新增 `recommendedNext` 字段。
- 孤立低掌握度节点不会因低掌握度自动抢优先级，已在路径规划集成测试中覆盖。
- 本切片不实现资源推荐、测评绑定或预计时长。
