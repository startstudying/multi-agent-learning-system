# 前端 UI 修复任务清单

## 任务边界

本任务清单用于后续实施 UI 修复，不在本轮直接修改生产前端代码。

## Allowed Files

- `frontend/src/api/analytics.ts`
- `frontend/src/App.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`
- `frontend/src/pages/admin/AdminModelProviders.vue`
- `frontend/src/components/*.vue`
- 本任务相关 docs/evidence/acceptance/changelog/memory 文件

## Disallowed Files

- `backend/**`
- `frontend/src/api/client.ts`
- `frontend/src/types/api.ts`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- 数据库 migration

## Task 1：修复构建失败

- [ ] 删除 `frontend/src/api/analytics.ts` 中未使用的 `apiRequest`。
- [ ] 运行 `cd frontend && pnpm build`。
- [ ] 若通过，记录为 GREEN；若失败，记录新错误并先修复构建。

## Task 2：Shell 与移动端首屏

- [ ] 调整 `App.vue` shell 文案，减少上下文 rail 技术噪音。
- [ ] 调整 `style.css` 移动端布局，让主页面标题和主操作进入首屏。
- [ ] 保留角色导航和 `shell-context` 测试钩子。
- [ ] 更新 App.spec shell 断言。

## Task 3：学生端主工作流

- [ ] 重排 `StudentDashboard.vue`：RAG 问答、引用摘要、下一步学习节点进入主区。
- [ ] 将画像、资源、测评降为次级区。
- [ ] 将 Trace/API 来源/状态 showcase 移到底部诊断区。
- [ ] 调整 no-source 视觉为安全拒答。
- [ ] 更新学生端结构测试。

## Task 4：教师端审核工作流

- [ ] 重排 `TeacherReviewQueue.vue`：待审队列 + 当前资源决策为主。
- [ ] 审核检查项、历史和说明降为辅助。
- [ ] 保持 `reject-review` disabled。
- [ ] 更新教师端结构测试。

## Task 5：管理端可信 triage

- [ ] 重排 `AdminOperations.vue`：异常/降级/待处理/健康优先。
- [ ] 将假图表替换为待接入指标空状态或真实摘要。
- [ ] 调整 `AdminModelProviders.vue` 的密钥说明、默认供应商和连通性状态。
- [ ] 更新管理端结构测试。

## Task 6：样式与组件收敛

- [ ] 评估是否抽取 `StatusPill`、`MetricCard`、`CitationPanel`、`TraceTimeline`。
- [ ] 降低紫蓝渐变使用，统一状态色语义。
- [ ] 检查按钮、卡片、状态标签在移动端不溢出。

## Task 7：验证与交付文档

- [ ] `cd frontend && pnpm test`
- [ ] `cd frontend && pnpm build`
- [ ] 桌面/移动端截图验证四个路由。
- [ ] 创建 Evidence 和 Acceptance。
- [ ] 更新 Changelog、Project Memory、Frontend Memory。

## Done Definition

- 构建和测试通过，或限制被明确记录。
- 三类角色页面首屏主任务清楚。
- 移动端核心入口不再下沉到首屏外。
- API/Trace/状态示例不再占据主视觉中心。
- 不新增依赖、不改后端、不改 API 契约。
