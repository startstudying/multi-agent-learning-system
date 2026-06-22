# ACCEPT-20260611 学生端数据源去硬编码

## 验收对象

`PLAN-20260611-student-dashboard-data-source-dehardcoding.md`

## 验收结论

Accepted。

## 验收清单

| 验收项 | 结果 | 说明 |
|---|---|---|
| 生产学生端无固定演示 ID | PASS | 静态搜索 `frontend/src/pages/student frontend/src/api frontend/src/types` 无 `stu_001`、`kb_java_backend`、`goal_java_backend`、`kp_sql_join`、`q_sql_join_cardinality`、`res_local`、`trc_resource_local`。 |
| 初始资源不再伪造 | PASS | `resources` 初始为空，页面显示“尚未生成资源”；只有后端资源生成响应返回后才展示资源。 |
| 初始 trace 不再伪造 | PASS | `traceSteps` 初始为空，页面显示“尚无 Agent Trace”；画像、路径、问答、资源生成等后端动作完成后才展示 trace。 |
| 课程来自后端 API | PASS | 新增 `listCourses()` 调用 `GET /api/courses`，学生页以当前课程选择态驱动 `courseId`。 |
| 知识库来自后端 API | PASS | 学生页从 `GET /api/knowledge-bases` 初始化知识库，并用 `selectedKnowledgeBaseId` 驱动文档、RAG、上传。 |
| 文档来自后端 API | PASS | 学生页从 `GET /api/knowledge-bases/{kbId}/documents` 初始化文档列表。 |
| RAG / AI QA 使用选择态 | PASS | 请求使用当前 `selectedKnowledgeBaseId`；AI QA 同时带当前课程 `courseId`。 |
| 上传使用选择态 | PASS | 上传资料只传当前 `selectedKnowledgeBaseId`、可用 `selectedCourseId`、可用 `selectedPathNodeId`，不伪造课程或章节。 |
| 资源生成使用选择态 | PASS | 请求使用 `getDefaultUserId()`、`selectedGoalId`、`selectedPathNodeId` 和用户选择的资源类型。 |
| 无题目时不提交测评 | PASS | 无 `selectedQuestionId` 时不调用 `/api/assessment/answers`，显示明确空状态。 |
| 非法 SSE payload 明确失败 | PASS | malformed SSE event 会关闭 stream 并显示 `Invalid SSE event payload`，不静默 fallback。 |
| 前端测试通过 | PASS | `cd frontend && pnpm test -- --run`：34 tests passed。 |
| 前端构建通过 | PASS | `cd frontend && pnpm build`：`vue-tsc -b && vite build` 通过。 |

## 剩余风险与假设

- 当前用户详情 API 未确认，因此 `learnerId` 仍由 `getDefaultUserId()` 提供 dev fallback；后续可替换为真实登录上下文。
- 学习目标列表 API 未确认，因此本阶段使用课程派生或用户输入的目标选择态。
- 课程题目列表 API 未确认，因此测评提交保持空态，不伪造题目 ID。

## 最终判定

本 M 级切片满足 Done Definition：文档、实现、测试、构建、静态搜索、Evidence、Acceptance、Changelog 与 Memory 更新均已完成。
