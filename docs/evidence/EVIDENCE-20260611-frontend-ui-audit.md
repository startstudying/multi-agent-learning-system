# 前端 UI 界面审查证据与验收

## 审查结论

Verdict: COMMENT / 需要进入一次有设计方向的 UI 改版。

当前界面不是“完全不能用”，但整体更像接口验收台或中保真原型，而不是面向学生、教师和管理员的成熟产品界面。最大问题不是单个颜色，而是信息层级、页面密度、视觉语言和状态表达都在抢注意力。

## 证据

### 读取的上下文

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/specs/SPEC-20260606-frontend-ui-prototype-refactor.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/harness/TEST_COMMANDS.md`
- `frontend/src/App.vue`
- `frontend/src/style.css`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`
- `frontend/src/pages/admin/AdminModelProviders.vue`

### 视觉证据

已查看既有截图：

- `frontend/target-ui-check/student-desktop.png`
- `frontend/target-ui-check/teacher-desktop.png`
- `frontend/target-ui-check/admin-desktop-fixed.png`
- `frontend/target-ui-check/student-mobile-fixed.png`

截图显示当前页面有基础响应式适配、卡片边界和状态色，但桌面端页面密度偏碎，移动端首屏被侧栏上下文和导航占据过多，核心任务入口下沉。

### 验证命令

`cd frontend && pnpm build`

Result: Failed。

Failure:

```text
src/api/analytics.ts(1,10): error TS6133: 'apiRequest' is declared but its value is never read.
```

说明：本次审查按只读边界执行，未修改 `frontend/src/api/analytics.ts`。该问题会阻塞前端生产构建。

## Findings

### P0 - 前端构建失败

File: `frontend/src/api/analytics.ts:1`

`apiRequest` 被导入但未使用，`vue-tsc -b` 在当前配置下直接失败。即使 UI 视觉改好，构建门禁不过也不能交付。

Recommended fix: 删除未使用的 `apiRequest` 导入，保留 `adminApiRequest`。

### P1 - 页面定位像“接口验收台”，不是角色工作台

Files:

- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`

学生端、教师端、管理员端都把大量 API 名称、状态枚举、traceId、接口来源、占位说明直接暴露在主视觉区域。对开发验收有用，但对目标用户会显得吵、硬、重，缺少“我现在该做什么”的主路径。

Recommended fix: 把接口来源、traceId、状态展示方式收进诊断抽屉或二级区域；首屏只保留角色任务、当前状态、下一步操作和必要证据。

### P1 - 信息密度过高，卡片和指标过多导致视觉疲劳

File: `frontend/src/style.css`

相关布局包括 `.workflow-strip`、`.metric-row`、`.summary-strip`、`.workbench-grid`、`.student-primary-workspace`、`.student-support-workspace`、`.student-diagnostics`。截图中学生端首屏同时出现流程条、六个摘要卡、RAG 卡、引用卡、学习路径卡和多个状态提示，用户注意力没有稳定落点。

Recommended fix: 为每个角色定义一个主工作流骨架：

- 学生端：问题输入/答案/引用为主，路径和资源为辅。
- 教师端：待审资源列表 + 当前决策为主，历史和规则为辅。
- 管理端：异常/健康/成本风险为主，接口来源和状态示例为辅。

### P1 - 移动端首屏核心任务入口下沉

Evidence: `frontend/target-ui-check/student-mobile-fixed.png`

移动端先显示品牌、用户、三段导航、上下文说明，再进入页面标题和主按钮。学生端最重要的提问入口在首屏下半部分，路径太长。

Recommended fix: 移动端将侧栏改为顶部紧凑身份条 + 横向/下拉导航；上下文 rail 默认折叠；主操作按钮和核心输入前置。

### P2 - 视觉语言偏单一，紫蓝状态色使用过重

File: `frontend/src/style.css`

主色、AI 色、按钮、导航激活态、图表占位和多处强调都集中在紫蓝系。虽然符合 AI 语义，但缺少分层和行业感，容易出现“模板 SaaS 原型”的观感。

Recommended fix: 保留紫蓝作为 AI/Agent 语义色，但引入更克制的中性色版面、少量学习领域语义色，以及更明确的状态色边界。避免所有重点都发光或渐变。

### P2 - 管理端图表仍是静态占位，产品可信度不足

File: `frontend/src/pages/admin/AdminOperations.vue`

`Agent / Token / RAG / Review` 图表区域是 CSS 占位。项目记忆允许占位，但从“好看”和“可信”的角度，它会让管理页显得像演示稿。

Recommended fix: 如果后端指标未齐，改成明确的运维 triage 列表、趋势占位骨架和“数据接入中”的低调状态，不要用看似真实的假图。

### P2 - 双语混排太多，削弱中文产品体验

Files:

- `frontend/src/App.vue`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`

大量标题使用“中文 + English Alias”，例如 `Learning Loop 工作台 / Student Learning Loop`。这对测试和工程沟通友好，但正式界面显得啰嗦。

Recommended fix: 用户可见标题优先中文；英文枚举、API path、traceId、状态码保留在开发诊断区或小字辅助信息。

## Suggested Redesign Direction

建议下一步做 M 级前端改版，不是微调几个颜色。

方向：安静、可信、教育 SaaS 工作台。保留 AI/RAG/Agent 的证据链优势，但把“证据”从主屏噪音变成可展开的可信层。

优先级：

1. 修复构建失败。
2. 重做移动端 shell，让核心任务进入首屏。
3. 为三类角色分别定义首屏主任务区。
4. 抽取 `StatusPill`、`EvidenceDrawer`、`CitationPanel`、`TraceTimeline`、`MetricCard`，统一状态和密度。
5. 管理端去掉假图表感，改成真实/待接入清晰分层。

## Acceptance

- 已完成 Project Memory、技能选择和 S 级任务分类。
- 已读取前端相关记忆、架构基线、历史 UI 规格和核心源码。
- 已检查现有 UI 截图，覆盖学生端、教师端、管理员端和移动端。
- 已运行 `pnpm build` 并记录失败原因。
- 本次未修改前端生产代码，符合只读审查边界。
