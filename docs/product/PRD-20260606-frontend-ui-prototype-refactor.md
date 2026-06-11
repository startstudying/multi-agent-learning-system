# 前端中低保真 UI 原型重构 PRD

## 背景

当前前端已经具备学生端、教师端、管理员端三类页面，并通过共享 API client 连接后端现有接口。但界面仍偏工程调试台，页面文字以英文为主，视觉状态和用户角色差异不够清晰。用户提供了 5 张低保真原型和一张视觉风格设定图，要求将前端重构为符合 EdTech + AI Agent + SaaS 后台属性的中低保真 UI 原型。

## 目标用户

- 学生：在 Learning Loop 工作台完成画像、RAG 问答、路径学习、资源使用、测评反馈。
- 教师：在 Review Queue 中审核 AI 生成资源，检查引用、安全、画像适配并给出决策。
- 管理员：在 Operations Dashboard 中查看系统健康、Agent/RAG/Review/Token 等当前可用运营信号。

## 目标

1. 基于现有 Vue 3 前端完成 UI 重构，不改后端 API 和数据库。
2. 保留原型图中的核心模块、状态展示、接口数据来源和工程字段。
3. 使用用户指定的色彩体系：Indigo/Blue 为系统主色，Violet/Purple 表达 AI/RAG/Agent 行为，Emerald/Amber/Red/Slate 表达状态语义。
4. 页面文字主要改为中文，保留 API path、traceId、chunkId、documentId、state enum、代码标识等英文工程字段。
5. 保持 SaaS 后台信息密度，不做营销页、不做过度美化、不引入新 UI 框架。

## 非目标

- 不实现后端 P3：RAG 生产索引 worker、VectorDB、真实模型网关、RBAC、Micrometer、结构化日志。
- 不新增后端接口，不伪造未实现接口调用。
- 不让前端直接调用 LLM、Embedding、VectorDB 或保存 API key。
- 不引入 Element Plus、Naive UI、Tailwind、Pinia 或其他新依赖。
- 不把权限、安全、检索、模型调用判断下沉到前端。

## 成功标准

- 学生端、教师端、管理员端三个页面都体现用户提供的中低保真原型结构。
- 所有关键状态都有明确视觉表达：loading、failed、empty、pending review、no source、approved、returned、rejected、degraded、down、healthy、backlog warning。
- AI/RAG/Agent 相关 UI 使用 Violet/Purple 作为专属强调色。
- 页面在桌面和移动端无明显遮挡、横向溢出或按钮文字不可读。
- `cd frontend && npm test -- --run` 通过。
- `cd frontend && npm run build` 通过。
