# 前端中低保真 UI 原型重构规格

## 技术边界

本轮只修改前端页面、样式和前端测试。后端接口、数据库、权限、安全和 RAG/Agent 业务实现不在范围内。

允许修改：

- `frontend/src/App.vue`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`
- 本次工作流文档、Evidence、Acceptance、Changelog、Memory

禁止修改：

- `backend/**`
- `frontend/src/api/*`
- `frontend/src/router.ts`
- `frontend/src/types/api.ts`
- 数据库 migration
- 依赖声明

## 视觉 Token

CSS 变量必须至少覆盖：

| Token | 色值 | 用途 |
|---|---|---|
| `--color-primary` | `#4F46E5` | 主按钮、激活导航、Tab |
| `--color-primary-blue` | `#2563EB` | 链接、普通流程高亮 |
| `--color-ai` | `#8B5CF6` | AI/RAG/Agent/生成动作 |
| `--color-ai-strong` | `#9333EA` | AI 强强调 |
| `--color-success` | `#10B981` | approved/healthy/done |
| `--color-warning` | `#F59E0B` | pending/degraded/backlog |
| `--color-danger` | `#EF4444` | failed/rejected/down/no source |
| `--color-slate-400` | `#94A3B8` | 辅助文本、loading |
| `--color-slate-200` | `#E2E8F0` | 边框、骨架屏 |
| `--color-bg` | `#F8FAFC` | 页面背景 |
| `--color-panel` | `#FFFFFF` | 卡片背景 |
| `--color-text` | `#1E293B` | 主文本 |
| `--color-muted` | `#64748B` | 次文本 |

## 页面结构

### Shell

- 左侧为角色导航和用户身份。
- 顶部为页面标题、搜索、通知、帮助、用户、退出。
- 右侧内容区由 RouterView 承载。
- `data-test="shell-context"` 保留，用于角色上下文测试。

### 学生端

必须存在：

- `data-test="student-primary-workspace"`：学习路径、RAG 问答、引用/拒答区域。
- `data-test="student-support-workspace"`：画像、资源状态、测评反馈。
- `data-test="student-diagnostics"`：Agent Trace 和接口来源。
- `data-test="no-source-card"`：无来源拒答卡。
- `data-test="status-showcase"`：状态展示摘要。

### 教师端

必须存在：

- `data-test="teacher-review-workspace"`
- `data-test="teacher-evidence-checklist"`
- `data-test="review-detail"`
- `data-test="review-feedback-input"`
- `data-test="approve-selected-review"`
- `data-test="request-revision"`
- `data-test="reject-review"`

`reject-review` 在本轮作为 UI 原型按钮展示，若当前后端决策类型未支持 `REJECTED`，按钮不得触发未实现 API。

### 管理员端

必须存在：

- `data-test="admin-triage"`
- `data-test="admin-dependency-matrix"`
- `data-test="admin-alert-table"`
- `data-test="admin-api-sources"`
- `data-test="status-showcase-admin"`

管理员图表可以使用 CSS 占位图，不新增 chart 依赖。

## API 接入规则

- 真实请求保持现有模块：health、analytics、reviews、learning、rag、resources、assessment、documents。
- 原型中目标态但当前未确认的 endpoint 仅展示在“接口数据来源”面板中，不进行 fetch。
- no source 状态可由当前 `sources.length === 0` 或本地示例状态展示，不由前端推断知识正确性。

## 测试规格

新增或调整 `frontend/src/App.spec.ts`：

- 验证中文 UI 结构存在。
- 验证关键状态文案和颜色类对应的节点存在。
- 验证教师拒绝按钮为原型禁用态或不调用未支持 API。
- 保留所有现有 API 行为测试。

## 架构漂移检查

| 检查 | 预期 |
|---|---|
| 前端不直连 LLM | PASS |
| 前端不存 API key | PASS |
| API 调用通过共享 wrapper | PASS |
| 不新增依赖 | PASS |
| 不改后端 | PASS |
