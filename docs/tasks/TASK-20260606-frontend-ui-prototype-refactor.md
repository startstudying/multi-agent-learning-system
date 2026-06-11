# 前端中低保真 UI 原型重构任务

## Task 1：文档与边界

- [x] 创建 PRD。
- [x] 创建 REQ。
- [x] 创建 SPEC。
- [x] 创建 PLAN。
- [x] 创建 TASK。
- [x] 创建 Context Pack。

Done Criteria：实现前所有必需文档存在，且 Context Pack 明确允许/禁止修改文件。

## Task 2：测试先行

- [x] 在 `frontend/src/App.spec.ts` 添加中文 UI 原型结构测试。
- [x] 运行 focused test 并观察失败。

Done Criteria：新增测试因 UI 结构/文案尚未满足而失败，不是语法错误。

## Task 3：Shell UI 重构

- [x] `App.vue` 改为中文 SaaS Shell。
- [x] 保留学生/教师/管理员路由入口。
- [x] 保留 `data-test="shell-context"`。

Done Criteria：Shell 文案中文化，角色上下文清晰。

## Task 4：学生端 UI 重构

- [x] 重构 Learning Loop 工作台结构。
- [x] 展示 RAG 问答、citation、no source、学习路径、资源状态、测评反馈、Agent Trace。
- [x] 保留现有 API 调用和核心按钮选择器。

Done Criteria：学生端符合用户第 1、2 张原型和第 6 张视觉体系。

## Task 5：教师端 UI 重构

- [x] 重构 Review Queue。
- [x] 展示待审核列表、资源详情、三类检查、反馈区、审核历史。
- [x] 增加禁用态拒绝按钮作为目标态原型，不调用未实现 API。

Done Criteria：教师端符合用户第 3 张原型和状态色要求。

## Task 6：管理员端 UI 重构

- [x] 重构 Operations Dashboard。
- [x] 展示 KPI、服务状态、趋势占位、异常告警、接口来源和状态示例。
- [x] 只使用当前 health / analytics 数据和占位图。

Done Criteria：管理员端符合用户第 4/5 张原型和状态色要求。

## Task 7：验证和交付

- [x] 运行 `cd frontend && npm test -- --run`。
- [x] 运行 `cd frontend && npm run build`。
- [x] 用浏览器检查 `/`、`/teacher/reviews`、`/admin/operations` 桌面端。
- [x] 用浏览器检查 `/` 移动端。
- [x] 创建 Evidence 和 Acceptance。
- [x] 更新 Changelog 和 Memory。
- [x] 创建 Retrospective。
- [x] 沉淀前端 UI 原型重构模式到项目技能。

Done Criteria：测试/build 通过或限制被记录，Evidence/Acceptance 完整。
