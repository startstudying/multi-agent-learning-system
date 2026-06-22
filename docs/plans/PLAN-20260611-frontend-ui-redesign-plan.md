# 前端 UI 修复实施计划

## Skill Selection Report

### Task Type

前端 UI 改版计划 / 可见产品行为优化。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求所有原始需求经过记忆、技能选择、分级、文档和证据流程。 |
| `frontend-design` | 本任务核心是视觉方向、信息架构、响应式和前端体验。 |
| `writing-plans` | 用户要求写修复计划，需要拆成可执行步骤。 |
| `vue3-component-design` | 项目技能注册中用于 Vue 页面、组件、状态管理。 |
| `ai-streaming-ui` | 学生端 RAG/SSE 问答区域需要保留流式状态体验。 |
| `dashboard-visualization` | 管理端需要从假图表转为可信 triage。 |

### Missing Skills

无。

### GitHub Research Needed

Yes。用户明确要求“参考别人的前端”。参考报告见 `docs/research/github-references/REF-20260611-frontend-ui-redesign.md`。

### New Project-Specific Skill To Create

暂不创建。若实施后沉淀出稳定的“AI 学习工作台 UI 模式”，再抽取到 `docs/skills/project-specific/`。

## Size Classification

Size: M

Reason: 本次是一个前端模块内的标准改版，涉及 shared shell、学生端、教师端、管理员端和样式/测试，改变可见体验，但不改变 REST API、DTO、DB、依赖、后端或 Agent/RAG 业务语义。

Required Documents:

- PRD
- REQ
- SPEC
- PLAN
- TASK
- CONTEXT

Can Skip:

- 后端设计文档
- DB 设计文档
- 依赖安全审查
- 独立 subagent 报告

Upgrade Trigger:

- 如果新增依赖、改 API/DTO、改后端、引入新路由契约或重做 Agent/RAG 工作流，升级为 L。

## Subagent Decision

Use Subagents: No

Reason: 虽然改动文件较多，但边界都在前端模块内，且当前任务只写修复计划，不实施代码。后续实施时可在执行前启用 Frontend Expert 做 L1 设计复核。

Parallelism Level: N/A

Implementation Mode: Single Codex plan, later single-task execution.

## Architecture Drift Check

| Check | Result |
|---|---|
| 前端不直连 LLM | PASS，计划不改 API 边界 |
| 前端不存 API key | PASS，Model Provider 仍只展示 masked key/输入新 key |
| API 调用走共享 wrapper | PASS，计划不改 `client.ts` |
| AI streaming 使用既有封装 | PASS，计划保留 `streamRagQuery` |
| 不新增依赖 | PASS |
| 不改后端/DB | PASS |

## 分阶段实施计划

### Phase 0：构建门禁修复

1. 修改 `frontend/src/api/analytics.ts`，删除未使用 `apiRequest`。
2. 运行 `cd frontend && pnpm build`。
3. 如果仍失败，先停下记录新失败，不进入 UI 改版。

### Phase 1：Shell 和移动端入口

1. 调整 `frontend/src/App.vue` 的 shell 文案和上下文密度。
2. 调整 `frontend/src/style.css` 中 `.app-shell`、`.sidebar`、`.role-switcher`、`.context-rail` 移动端规则。
3. 保留 `data-test="shell-context"`。
4. 移动端目标：390px 宽首屏能看到页面标题和主操作。

### Phase 2：学生端重排

1. 修改 `StudentDashboard.vue` 模板结构，不改 API 调用逻辑。
2. 将 RAG 问答、引用摘要、下一步学习节点作为首屏主区。
3. 将画像、资源、测评作为次级区域。
4. 将 Trace、接口来源、状态 showcase 移到底部诊断区。
5. no-source 改为可信拒答状态，不使用强错误警报视觉。

### Phase 3：教师端重排

1. 修改 `TeacherReviewQueue.vue` 模板结构，不改审核 API。
2. 队列和当前资源审核决策成为主区。
3. 审核检查项和历史进入辅助区。
4. `reject-review` 保持 disabled。

### Phase 4：管理端可信化

1. 修改 `AdminOperations.vue`，首屏改为异常/降级/待处理/健康 triage。
2. 删除或弱化 CSS 假图表，改为待接入指标空状态。
3. 修改 `AdminModelProviders.vue`，让默认供应商、启用状态、密钥说明更清楚。

### Phase 5：组件与样式收敛

1. 如果重复明显，新增轻量组件：
   - `frontend/src/components/StatusPill.vue`
   - `frontend/src/components/MetricCard.vue`
   - `frontend/src/components/CitationPanel.vue`
   - `frontend/src/components/TraceTimeline.vue`
2. 不为了抽象而抽象；如果单文件改动更安全，则先统一 class 和样式。
3. 调整 CSS token，降低紫蓝占比，明确 success/warning/danger/info 语义。

### Phase 6：测试与视觉验收

1. 更新 `frontend/src/App.spec.ts` 结构断言。
2. 运行 `cd frontend && pnpm test`。
3. 运行 `cd frontend && pnpm build`。
4. 启动 `pnpm dev`，截取桌面与移动端：
   - `/`
   - `/teacher/reviews`
   - `/admin/operations`
   - `/admin/model-providers`
5. 创建 Evidence 和 Acceptance。

## 风险与回退

| Risk | Mitigation |
|---|---|
| 大模板改动导致测试大量失效 | 每个角色页面单独改，单独跑测试 |
| 视觉改版误伤 API 行为 | 不改 API 模块，不改类型，不改服务调用 |
| 移动端修复导致桌面密度不足 | 每阶段保留桌面截图检查 |
| 组件抽取过度 | Phase 5 只抽高重复组件，必要时延后 |
