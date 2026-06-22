# EVIDENCE-20260611 学生端数据源去硬编码

## 任务

完成 `PLAN-20260611-student-dashboard-data-source-dehardcoding.md`：将学生端 `StudentDashboard.vue` 从固定演示数据改为后端数据与选择态驱动。

## 实现证据

### 已去除的生产源码固定演示 ID

学生端生产源码不再内置以下演示 ID：

- `stu_001`
- `kb_java_backend`
- `goal_java_backend`
- `kp_sql_join`
- `q_sql_join_cardinality`
- `res_local`
- `trc_resource_local`

说明：测试 fixture 中仍保留部分历史 ID，用于模拟“后端返回什么，前端就使用什么”的回归场景；静态验收范围限定为 `frontend/src/pages/student`、`frontend/src/api`、`frontend/src/types` 的生产源码。

### 数据源调整

- `learnerId` 由 `frontend/src/api/client.ts` 的 `getDefaultUserId()` 集中提供，默认 fallback 为 `student_dev`，不再散落在学生页。
- 课程列表由 `frontend/src/api/courses.ts` 的 `listCourses()` 调用 `GET /api/courses` 初始化。
- 知识库与文档由 `GET /api/knowledge-bases`、`GET /api/knowledge-bases/{kbId}/documents` 初始化。
- RAG / AI QA 使用当前选择的 `selectedKnowledgeBaseId` 和 `selectedCourseId`。
- 上传资料使用当前选择的 `selectedKnowledgeBaseId`，`courseId` / `chapterId` 只在存在选择态时传递，不伪造。
- 资源生成使用 `learnerId`、`selectedGoalId`、`selectedPathNodeId` 与用户选择的资源类型。
- 初始 `resources` 与 `traceSteps` 为空；仅后端动作返回后展示资源或 trace。

### 后端缺口处理

- 当前用户详情 API 未确认：本阶段只集中 dev fallback，不新增后端 API。
- 学习目标列表 API 未确认：目标由课程派生或用户输入，缺失时提示补充目标。
- 课程题目列表 API 未确认：无 `selectedQuestionId` 时不调用 `POST /api/assessment/answers`，页面显示“暂无可提交的课程题目；请先接入题目列表。”。

## 验证命令

### 前端测试

命令：

```bash
cd frontend && pnpm test -- --run
```

结果：

- Exit code: 0
- `src/App.spec.ts`: 34 tests passed
- Test Files: 1 passed

### 前端构建

命令：

```bash
cd frontend && pnpm build
```

结果：

- Exit code: 0
- `vue-tsc -b && vite build` 通过
- Vite production build 成功生成 `dist/`

### 固定 ID 静态搜索

命令：

```bash
rg -n "stu_001|kb_java_backend|goal_java_backend|kp_sql_join|q_sql_join_cardinality|res_local|trc_resource_local" frontend/src/pages/student frontend/src/api frontend/src/types
```

结果：

- Exit code: 1
- 无输出，表示生产前端验收范围内没有匹配项。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 本任务未修改后端 production code。 |
| Frontend rules | PASS | API 调用继续通过 `api/client.ts` 共享 wrapper；前端未直连 LLM API，未放置 API key。 |
| Agent / RAG rules | PASS | RAG / AI QA 仍走后端接口，引用与 trace 由后端响应驱动；非法 SSE payload 现在明确进入错误状态。 |
| Security | PASS | 未新增依赖、未新增密钥、未把权限控制放到前端。 |
| API / Database | PASS | 未修改 REST API contract、DTO、数据库 schema 或 migration。 |

## 结论

验收通过。学生端生产代码已完成数据源去硬编码，关键动作改由后端数据和选择态驱动；缺失后端能力以明确空状态表达，不再伪造业务 ID。
