# REQ-20260611 学生端数据源去硬编码

## 背景

学生端 `StudentDashboard.vue` 当前仍保留演示期固定数据，例如固定 learner、知识库、学习目标、知识点、题目、初始资源和本地 trace。角色工作台已经完成分离，但学生端内部数据仍像演示台，不利于接入真实课程和知识库。

## 目标

- 学生端生产代码不再写死 `stu_001`、`kb_java_backend`、`goal_java_backend`、`kp_sql_join`、`q_sql_join_cardinality`、`res_local_*`、`trc_resource_local`。
- 学生端从现有后端 API 初始化课程、知识库、文档。
- RAG、AI QA、上传、学习路径、资源生成使用当前选择态。
- 初始资源和 Agent Trace 为空状态，只有后端动作返回后才展示。
- 后端尚未提供的数据源，例如题目列表，不再伪造，改为明确空状态或禁用动作。

## 非目标

- 不新增后端 API。
- 不修改 REST path、DTO、数据库 schema 或权限逻辑。
- 不新增前端依赖。
- 不重做教师端、管理员端。

## 验收要求

- `frontend/src/pages/student`、`frontend/src/api`、`frontend/src/types` 的生产代码中不再出现固定演示 ID。
- 无知识库、无课程、无学习路径、无题目时有明确空状态或提示。
- 文档上传使用当前选择的知识库与课程，不再传固定课程/章节。
- 资源生成必须依赖当前 learner、goal、path node 选择态。
- 测评提交在没有题目 ID 时不得调用 `/api/assessment/answers`。
- 前端测试和构建通过。
