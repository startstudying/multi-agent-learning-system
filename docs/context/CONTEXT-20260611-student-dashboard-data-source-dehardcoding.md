# CONTEXT-20260611 学生端数据源去硬编码

## 完成状态

2026-06-11 已完成。生产前端学生端固定 ID 静态搜索无匹配；前端测试 34 个通过；前端构建通过。

## 当前边界

本任务只做前端学生端数据源去硬编码和面板结构整理。后端缺失的当前用户、学习目标列表、题目列表接口不在本任务新增；前端必须通过空状态和禁用动作避免伪造。

## 已确认 API

- `GET /api/courses`
- `GET /api/courses/{courseId}/knowledge-graph`
- `GET /api/knowledge-bases`
- `GET /api/knowledge-bases/{kbId}/documents`
- `POST /api/knowledge-bases/{kbId}/documents`
- `POST /api/profile/dialogue/extract`
- `POST /api/learning-paths`
- `POST /api/rag/query`
- `POST /api/rag/query/stream`
- `POST /api/ai/qa`
- `POST /api/resources/generation-tasks`
- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/tasks/{taskId}/trace`
- `POST /api/assessment/answers`

## 已确认缺口

- 当前用户详情 API 未确认。
- 学习目标列表 API 未确认。
- 课程题目列表 API 未确认。

## 关键生产代码禁用字符串

- `stu_001`
- `kb_java_backend`
- `goal_java_backend`
- `kp_sql_join`
- `q_sql_join_cardinality`
- `res_local`
- `trc_resource_local`

## 验证证据

必须提供测试、构建和静态搜索结果。
