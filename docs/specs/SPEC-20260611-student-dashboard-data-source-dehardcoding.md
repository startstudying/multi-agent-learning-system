# SPEC-20260611 学生端数据源去硬编码

## 设计原则

- 保持后端 API 合同稳定。
- 借鉴 `jc-rag-kb-front` 的分层：API、composable、页面组件分离。
- 不把后端权限或身份可信性放到前端；前端只维护当前选择态。
- 不用演示数据冒充真实后端状态。

## 数据源

| 数据 | 来源 | 缺失行为 |
|---|---|---|
| learnerId | `getDefaultUserId()` 集中读取 `VITE_DEV_USER_ID` fallback | 后续可替换为 `GET /api/me` |
| courses | `GET /api/courses` | 显示暂无课程，上传不传 `courseId` |
| knowledgeBases | `GET /api/knowledge-bases` | 显示暂无知识库，RAG/上传提示先选择或创建 |
| documents | `GET /api/knowledge-bases/{kbId}/documents` | 显示暂无已索引文档 |
| pathNodes | `POST /api/learning-paths` 返回 nodes | 未生成路径时资源生成禁用 |
| questionId | 暂无现有接口 | 测评提交显示空状态，不伪造题目 |
| resources | `POST/GET /api/resources/generation-tasks` | 初始为空 |
| traceSteps | 后端动作返回 trace 或 `GET /api/agent/tasks/{taskId}/trace` | 初始为空 |

## 前端结构

- `frontend/src/api/courses.ts`：课程 API。
- `frontend/src/composables/useStudentWorkbench.ts`：学生端统一数据源、选择态、文档映射、初始化错误。
- `frontend/src/pages/student/components/*`：学生端面板组件，优先保持展示组件和 `data-test` 稳定。
- `StudentDashboard.vue`：页面组装与跨面板 action 编排。

## 行为约束

- RAG / AI QA 请求的 `kbIds` 必须来自当前选中的知识库。
- AI QA 的 `courseId` 必须来自当前选中的课程，可为空。
- 上传文档的 `courseId` 和 `chapterId` 必须来自选择态；没有选择时不伪造。
- 资源生成缺少 `goalId` 或 `pathNodeId` 时不得请求后端。
- 测评提交缺少 `questionId` 时不得请求后端。

## 测试策略

- 更新 `App.spec.ts` 使 mock 数据使用后端返回 ID。
- 增加无题目时不提交测评的断言。
- 增加静态搜索作为 Evidence：生产学生端/API/types 不再出现固定演示 ID。
