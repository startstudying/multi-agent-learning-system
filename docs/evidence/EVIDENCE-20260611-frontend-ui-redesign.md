# 证据文档 - 前端 UI 重设计执行

## 1. 追踪

- PRD：`docs/product/PRD-20260611-frontend-ui-redesign-plan.md`
- REQ：`docs/requirements/REQ-20260611-frontend-ui-redesign-plan.md`
- SPEC：`docs/specs/SPEC-20260611-frontend-ui-redesign-plan.md`
- PLAN：`docs/plans/PLAN-20260611-frontend-ui-redesign-plan.md`
- TASK：`docs/tasks/TASK-20260611-frontend-ui-redesign-plan.md`
- Context：`docs/context/CONTEXT-20260611-frontend-ui-redesign-plan.md`
- 日期：2026-06-11

## 2. 实现内容

- 学生端首屏改为参考 GPT Web 的三栏对话式工作台：左侧轻导航，中间课程问答线程与底部 composer，右侧活动 / 思考 / 来源 / 下一步 / 记忆面板。
- 保留统一 AI QA 合同能力：`FAST` / `THINKING` / `EXPERT` 模式选择、`reasoningSummary` 安全摘要、生产环境 `/api/ai/qa` POST 调用，不回退到旧 URL SSE。
- 教师端改为审核队列优先，管理端改为异常 / 降级 / 待处理 / 健康 triage，不再展示伪造百分比图表。
- 修复 `analytics.ts` 未使用导入导致的 build gate 失败。

## 3. 变更文件

| 文件 | 摘要 |
|---|---|
| `frontend/src/App.vue` | 增加 ChatGPT 风格左侧导航、项目与聊天列表、移动摘要。 |
| `frontend/src/pages/student/StudentDashboard.vue` | 学生端主区改为聊天线程 + composer + 活动侧栏，并保留 QA 模式与引用/trace 状态。 |
| `frontend/src/pages/teacher/TeacherReviewQueue.vue` | 审核队列置前，当前决策置后。 |
| `frontend/src/pages/admin/AdminOperations.vue` | 管理端改为运维 triage 与待接入指标空态。 |
| `frontend/src/pages/admin/AdminModelProviders.vue` | 优化默认供应商与 API key 文案。 |
| `frontend/src/api/analytics.ts` | 移除未使用导入。 |
| `frontend/src/style.css` | 新增轻导航、学生端三栏聊天布局、composer、活动面板、移动端样式。 |
| `frontend/src/App.spec.ts` | 更新 UI 与统一 AI QA 合同测试断言。 |

## 4. 测试结果

| 命令 | 结果 | 备注 |
|---|---|---|
| `cd frontend && pnpm test -- --run` | 通过 | 1 个测试文件，31 个测试通过。 |
| `cd frontend && pnpm build` | 通过 | `vue-tsc -b && vite build` 通过。 |

## 5. 视觉证据

- 桌面截图：`frontend/target-ui-redesign-20260611/student-reference-desktop.png`
- 移动截图：`frontend/target-ui-redesign-20260611/student-reference-mobile.png`

截图验证点：

- 桌面端同屏包含左侧学习导航、中间课程问答、底部 composer、右侧学习上下文面板。
- QA 模式选择器已从原生 fieldset 视觉改为紧凑分段控件。
- 移动端保留可访问的学生端工作台与问答输入，不展示 API 噪音作为首屏主体。

## 6. 架构漂移检查

- 未改后端、数据库、API client wrapper、类型合同之外的公共接口。
- 未新增依赖。
- 前端仍通过项目 API wrapper 调用后端，未直接调用 LLM 或保存密钥。
- AI QA 生产路径保持后端拥有模型调用与 reasoning effort 映射。

## 7. 已知限制

- 当前视觉仍使用现有系统字体和局部旧组件，后续若继续打磨，可抽取 `ChatComposer`、`ActivityPanel`、`SidebarNav` 等组件。
- 本次没有改登录态、真实消息历史持久化或后端数据合同。
