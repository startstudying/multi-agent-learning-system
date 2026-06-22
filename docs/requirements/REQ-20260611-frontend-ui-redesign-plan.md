# 前端 UI 修复计划需求

## 功能需求

### R1. 构建门禁修复

- 删除 `frontend/src/api/analytics.ts` 未使用的 `apiRequest` 导入。
- `cd frontend && pnpm build` 必须通过。

### R2. Shell 改版

- 桌面端保留左侧角色导航，但降低上下文 rail 的视觉重量。
- 移动端改为紧凑顶部身份条 + 导航，`shell-context` 默认不占据首屏主区域。
- 品牌、用户、角色导航不能挤压核心任务入口。

### R3. 学生端主任务重排

- 首屏主区聚焦 RAG 问答：问题输入、回答、引用摘要、下一步学习节点。
- 学习路径、画像、资源、测评保留，但调整为次级信息区。
- Trace、接口来源、状态 showcase 移到诊断区，默认靠后展示。
- no-source 状态保持可见，但视觉语气从错误警报改为“安全拒答/需补充来源”。

### R4. 教师端审核工作流重排

- 待审队列和当前资源决策区必须成为页面主体。
- 审核检查项、历史记录、接口状态作为辅助区。
- `Reject` 仍保持禁用，直到后端和类型契约支持 `REJECTED`。

### R5. 管理端可信 triage

- Admin Operations 首屏优先展示异常、降级、待处理、健康状态。
- 现有 CSS 假图表改为“待接入指标”或真实数据摘要，不伪造趋势。
- Model Provider 页面保持配置导向，突出密钥不回显、默认供应商、连通性状态。

### R6. 组件与样式收敛

- 抽取或统一以下可复用 UI 模式：
  - `StatusPill`
  - `EvidenceDrawer` 或等价证据展开区
  - `CitationPanel`
  - `TraceTimeline`
  - `MetricCard`
- 全局样式保留现有 CSS 体系，不新增依赖。
- 降低紫蓝渐变和高亮的使用频率，建立更安静的教育 SaaS 视觉基调。

## 非功能需求

- 可访问性：按钮、链接、输入框保留清晰 focus-visible。
- 响应式：390px 宽移动端无横向滚动，主要按钮文字不溢出。
- 安全：前端不直连 LLM，不存 API key。
- 可维护：组件抽取不能造成 API 模块或业务逻辑重写。

## 验收需求

- `pnpm build` 通过。
- `pnpm test` 通过。
- 使用浏览器截图检查桌面和移动端。
- 更新 Evidence、Acceptance、Changelog、Frontend Memory。
