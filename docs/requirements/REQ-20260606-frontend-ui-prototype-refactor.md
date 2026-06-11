# 前端中低保真 UI 原型重构需求

## 功能需求

### FR-01 通用 Shell / Navigation

- 保留三类角色入口：学生端、教师端、管理员端。
- 侧边栏和 Header 使用中文文案。
- 当前页面和当前角色必须有清晰高亮。
- 展示全局搜索、通知、帮助、用户、退出等原型入口，但不实现新业务逻辑。

### FR-02 学生端 Learning Loop 工作台

- 展示学习目标摘要、当前画像、学习路径节点、RAG 问答区、引用来源列表、资源生成状态、测评反馈、Agent Trace 小时间线。
- RAG 问答区必须区分“有来源回答”和“无可靠来源拒答”。
- 学习路径节点必须区分已完成、进行中、未开始、失败、待反馈。
- 资源生成状态必须展示生成中、生成成功、生成失败、待教师审核。
- Agent Trace 必须展示 traceId、耗时、状态。

### FR-03 RAG Chat + Citation Viewer

- 展示 Chat 区域、Citation Viewer 引用查看器、引用展开态、no source 拒答卡片、接口数据来源说明。
- Citation 编号使用 `[1]`、`[2]`、`[3]`，并展示 documentId、chunkId、pageNum、sectionTitle、score。
- no source 状态使用 Red 警告图标和浅色说明卡，文案明确“无可靠来源，系统暂不回答”。

### FR-04 教师 Review Queue

- 展示待审核列表、资源详情、引用检查、安全检查、画像适配检查、教师反馈区、审核历史。
- 审核按钮包含批准、退回修改、拒绝，并形成明确视觉层级。
- pending review 使用 Amber，approved 使用 Emerald，rejected/high risk 使用 Red。
- 保持当前已实现的审核 API 调用边界，不新增未实现接口。

### FR-05 管理员 Operations Dashboard

- 展示 KPI 概览、服务状态卡片、指标趋势占位、异常告警区、关键按钮、接口数据来源说明。
- 只对接当前已有 health / analytics 数据。
- 未由当前 API 支撑的运营指标只作为原型占位或待接入说明，不写真实请求。
- 服务状态必须支持 healthy、degraded、down、loading、empty、failed。

### FR-06 状态和视觉系统

- 颜色体系必须符合用户提供设定：
  - 主色 Indigo `#4F46E5`
  - AI/RAG/Agent Violet `#8B5CF6`
  - Success Emerald `#10B981`
  - Pending/Warning Amber `#F59E0B`
  - Failed/Rejected Red `#EF4444`
  - Neutral Slate `#94A3B8` / `#E2E8F0`
  - 背景 `#F8FAFC`
- 卡片圆角控制在 8px-12px，使用轻量阴影。
- 不使用营销页 Hero 或装饰性大图。

## 非功能需求

- 保持 Vue 3 + TypeScript + Vite + Vue Router + lucide-vue-next。
- 保持 API 调用通过 `frontend/src/api/client.ts` 和现有 API 模块。
- 保持核心 `data-test` 选择器，新增选择器用于 UI 契约测试。
- 移动端不得出现明显横向滚动。

## 约束

- 不修改 `backend/**`。
- 不修改 `frontend/src/api/*` 的请求语义，除非只是测试不涉及的类型展示调整；本轮默认不改。
- 不新增依赖。
- 不在 `docs/superpowers/` 创建新文件。
