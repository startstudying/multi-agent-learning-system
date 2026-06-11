# 前端中低保真 UI 原型重构计划

## Skill Selection Gate

| Skill | 选择原因 |
|---|---|
| feature-development-workflow | 本项目要求所有需求先走文档和 Context Pack |
| frontend-design | 用户提供 UI 原型和视觉体系，需要做前端设计实现 |
| vue-edu-admin-frontend | 本轮涉及学生、教师、管理员 Vue 页面 |
| vue-ai-learning-ui | 学生端涉及 AI Chat、SSE 状态、学习工作台 |
| rag-citation-viewer | RAG 引用查看和 no source 状态是核心要求 |
| agent-trace-design | Agent Trace 只读时间线是核心模块 |
| test-driven-development | 需要先用 UI 契约测试锁定结构 |
| verification-before-completion | 完成前必须运行测试和 build |

缺失技能：无。

GitHub Research：不需要，用户已提供完整低保真原型和视觉规范。

## Multi-Expert Subagent Gate

Use Subagents：No。

原因：本轮只修改前端一个模块和交付文档，不涉及后端/API/DB/安全实现。若后续要把目标态 API 全量接入，再启用 Frontend Expert + Backend Expert + Integration Reviewer。

## Confidence Check

| 检查项 | 结论 |
|---|---|
| 重复实现 | 当前已有前端 cockpit 基础，但未满足用户中文中低保真视觉稿，需要在现有页面上重构，不新增重复入口 |
| 架构合规 | 使用 Vue 3、TypeScript、Vue Router、lucide-vue-next 和全局 CSS，不新增依赖 |
| 官方文档 | 本轮不使用新库和新 API，沿用现有 Vue/Vite 写法 |
| OSS 参考 | 不需要，用户原型和项目约束已足够 |
| 根因识别 | 当前 UI 与用户给定原型差异在中文信息架构、视觉 token、关键状态、教师/管理员密度表达 |

信心：0.92。可以进入实现，但必须先补齐 Context Pack 并执行测试。

## 实施步骤

1. 创建 PRD、REQ、SPEC、PLAN、TASK、CONTEXT。
2. 在 `App.spec.ts` 增加 UI 结构和状态契约测试，先运行确认失败。
3. 重构 `App.vue` 为中文 Shell 和角色导航。
4. 重构 `StudentDashboard.vue`，保留 API 行为，调整为 Learning Loop 工作台。
5. 重构 `TeacherReviewQueue.vue`，强化审核队列、证据检查和操作层级。
6. 重构 `AdminOperations.vue`，强化 KPI、服务状态、图表占位和异常表。
7. 重写 `style.css` 的视觉 token 和响应式规则。
8. 运行 `npm test -- --run` 和 `npm run build`。
9. 启动 Vite，用浏览器检查桌面和移动端布局。
10. 创建 Evidence、Acceptance，更新 Changelog 和 Memory。

## 风险

- 当前测试文件较大，UI 文案改中文可能影响旧英文断言。
- 教师拒绝按钮是目标态原型，但当前 `ReviewDecisionPayload` 只允许 `APPROVED` / `REVISION_REQUESTED`，因此本轮只能显示禁用原型按钮，不能触发 API。
- 管理员图表只能做占位，不可假装后端已有完整生产观测 API。

## 测试命令

```bash
cd frontend && npm test -- --run
cd frontend && npm run build
```

## 架构漂移前置检查

| 检查 | 状态 | 说明 |
|---|---|---|
| Frontend rules | PASS | 不直连 LLM，不存 key，保留 shared API |
| Agent/RAG rules | PASS | 只读展示 trace/citation，不实现 Agent/RAG 业务逻辑 |
| Security | PASS | 不新增依赖，不改权限 |
| API/Database | PASS | 不改 API contract，不改 DB |
