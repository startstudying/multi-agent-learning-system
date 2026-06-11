# 前端中低保真 UI 原型重构复盘

## 结果

本轮完成学生端 Learning Loop、RAG Citation Viewer、教师 Review Queue、管理员 Operations Dashboard 和通用 Shell 的中文中低保真 UI 重构。实现遵守前端边界，不修改后端、API client、路由、类型定义或依赖。

## 有效做法

- 先用 UI 契约测试把中文页面结构、no source、状态示例和角色上下文锁住，再进入实现。
- 保留英文工程别名，避免破坏已有行为测试和 API 字段可读性。
- 对后端未支持的目标态能力使用禁用按钮和占位说明，避免前端伪造能力。
- 使用 headless Chrome 做桌面和移动端截图检查，发现并修复移动端按钮截断。

## 风险和遗留

- 当前 UI 仍是页面级实现，后续需要抽出 `StatusPill`、`NoSourceCard`、`CitationPanel`、`TraceTimeline` 等组件。
- 管理员图表是 CSS 占位，真实生产观测 API 接入后需要替换为真实数据。
- 教师 Reject 按钮保持禁用，等待 review decision contract 支持 `REJECTED`。

## 可复用模式

- 中低保真 UI 实现时，先明确“当前 API 可实现态”和“目标态占位”。
- 对 AI/RAG/Agent 功能，必须同时展示来源、traceId、状态和失败/拒答语义。
- 视觉 QA 至少覆盖核心桌面路由和一个移动端主流程。
