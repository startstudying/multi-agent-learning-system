# Knowledge DAG 依赖类型与路径规划证据

## 1. 追踪

- TASK：`docs/tasks/TASK-20260606-knowledge-dependency-types-path-planning.md`
- SPEC：`docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md`
- 日期：2026-06-06

## 2. 实现内容

本次补齐 P1-2 Knowledge DAG 最小切片：

- `POST /api/knowledge-dependencies` 创建依赖时，`dependencyType` 只接受 `PREREQUISITE`、`RELATED`、`ADVANCED`。
- 创建端会对合法值做 trim + uppercase 规范化后保存。
- 学习路径规划只把 `PREREQUISITE` 作为前置依赖参与拓扑排序和节点锁定。
- `RELATED` 与 `ADVANCED` 仍可保存到知识图谱，但不会影响学习路径顺序和 `LOCKED` 状态。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java` | 修改 | 增加依赖类型白名单和规范化，非法类型返回 `VALIDATION_ERROR` |
| `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java` | 修改 | 路径规划过滤出 `PREREQUISITE` 依赖后再构建前置依赖表 |
| `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java` | 修改 | 增加非法依赖类型测试，并断言合法创建返回 `dependencyType` |
| `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java` | 修改 | 增加 `RELATED` / `ADVANCED` 不参与路径锁定和排序的测试 |
| `docs/product/PRD-20260606-knowledge-dependency-types-path-planning.md` | 新增 | 产品目标和非目标 |
| `docs/requirements/REQ-20260606-knowledge-dependency-types-path-planning.md` | 新增 | 功能、数据、权限和验收需求 |
| `docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md` | 新增 | API、依赖类型语义和路径规划规则 |
| `docs/plans/PLAN-20260606-knowledge-dependency-types-path-planning.md` | 新增 | 执行计划、子代理决策和 confidence check |
| `docs/tasks/TASK-20260606-knowledge-dependency-types-path-planning.md` | 新增 | 任务拆解 |
| `docs/context/CONTEXT-20260606-knowledge-dependency-types-path-planning.md` | 新增 | 修改边界和测试命令 |
| `docs/subagents/runs/RUN-20260606-knowledge-dependency-types-path-planning.md` | 新增 | 子代理核查结果集成记录 |

## 4. TDD 证据

### RED

执行命令：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test
```

失败结果：

- `CourseKnowledgeControllerTest.rejectsUnsupportedKnowledgeDependencyType`：期望 400，实际 200。说明非法 `dependencyType=BLOCKING` 当前会被保存。
- `LearningWorkflowControllerTest.ignoresRelatedAndAdvancedDependenciesWhenPlanningKnowledgeDagPath`：期望第一个节点保持创建顺序中的 `Repository Patterns`，实际被 `RELATED` 边重排为 `Entity Mapping`。说明非前置边当前被当作前置依赖。

### GREEN

执行命令：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test
```

通过结果：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 5. 架构漂移检查

- [x] Controller 未增加业务逻辑。
- [x] `KnowledgeCatalogService` 负责创建端业务校验。
- [x] `LearningWorkflowService` 负责路径规划策略过滤。
- [x] 未新增数据库 migration。
- [x] 未新增依赖。
- [x] 未修改前端。
- [x] 未新增 Agent/RAG 执行链路。

## 6. 已知限制

- 当前依赖创建接口仍没有课程教师权限校验，属于 P3-4 权限与安全加固范围。
- 本切片不实现掌握度低于 0.6 时优先补救前置知识。
- 本切片不扩展路径节点的预计时长、资源类型和测评绑定字段。
- 历史库中如果已有非法 `dependency_type`，路径规划会忽略非 `PREREQUISITE` 的值，但本次不做历史数据清洗。
