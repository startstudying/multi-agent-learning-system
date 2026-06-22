# GitHub 参考报告：前端 UI 修复计划

## 研究目标

为 AI Learning OS 前端 UI 改版寻找可借鉴的公开前端产品模式，重点解决当前审查中发现的四类问题：

- 页面像接口验收台，而不是角色工作台。
- 学生端、教师端、管理员端首屏缺少稳定主任务。
- AI/RAG/Trace/Citation 信息暴露过重。
- 移动端 shell 占用首屏，核心任务入口下沉。

## 参考来源

| 项目 | 链接 | 可借鉴点 | 不采用点 |
|---|---|---|---|
| Open edX Learning MFE | https://github.com/openedx/frontend-app-learning | 学习者课程体验聚焦课程大纲、进度、实际内容等学习页；适合借鉴“学习主路径优先，工程信息后置”的信息架构。 | 不迁移 React/Paragon，不照搬课程页结构。 |
| LearnHouse | https://github.com/learnhouse/learnhouse | 课程、集合、作业、讨论、分析和 AI playgrounds 等教育产品能力按学习/教学任务组织；适合借鉴“内容产品化”和“教师管理体验”。 | 不引入 CMS/编辑器，不扩大本次范围到课程创建能力。 |
| Kotaemon | https://github.com/Cinnamon/kotaemon | RAG QA 产品强调 clean/minimal UI、文档聊天和可定制 RAG pipeline；适合借鉴“答案为主、来源为证据层”的 RAG 对话布局。 | 不引入 Gradio，不复制 Python/RAG pipeline 结构。 |
| assistant-ui | https://github.com/assistant-ui/assistant-ui | AI chat 体验强调生产级对话组件；适合借鉴消息流、输入区、工具状态、线程体验的结构。 | 不引入 React 包，不改变 Vue 技术栈。 |
| Vuestic Admin | https://github.com/epicmaxco/vuestic-admin | Vue 3 + Vite + Pinia 后台模板，强调响应式、可访问性、快速维护；适合借鉴管理端导航、指标分组、状态密度和后台信息架构。 | 不新增 Vuestic UI/Tailwind 依赖，不套模板。 |

## 提炼模式

### 1. 学生端：学习任务优先，证据可展开

参考 Open edX 和 Kotaemon，学生端首屏应以“提问/学习内容/下一步”为主。RAG sources、traceId、API path、chunkId 属于可信证据层，不应该和主答案抢视觉焦点。

落地到本项目：

- 首屏主区：问题输入、回答、引用摘要、下一步学习节点。
- 次级区：学习路径、资源、测评、画像。
- 诊断区：Trace、接口来源、状态枚举，默认折叠或放页面底部。

### 2. 教师端：队列 + 决策，不做长表演

参考 LearnHouse 的教学/作业管理思路，教师端应围绕“待审资源列表、当前资源、批准/退回理由”组织，而不是把审核规则、历史、接口状态都铺在首屏。

落地到本项目：

- 左侧/上方为待审队列。
- 主区为当前资源摘要、引用核验、安全核验、画像适配。
- 决策动作固定在当前资源区域底部。

### 3. 管理端：运维 triage，不展示假图

参考 Vuestic Admin，后台应稳定、密集但不花。当前 Admin 的 CSS 假图表容易削弱可信度。

落地到本项目：

- 首屏按严重程度展示：异常、降级、待处理、健康。
- 未接入的生产图表展示为骨架/空状态，而不是假趋势图。
- Model Provider 独立为配置页，保留清楚的密钥不回显语义。

### 4. AI 对话：输入稳定、消息流清楚、来源不遮挡

参考 assistant-ui 和 Kotaemon，AI 对话不应把所有技术状态同时铺开。

落地到本项目：

- 输入区固定在主任务区域，移动端也优先可见。
- streaming 状态用轻量 token/status 行表达。
- citation 以摘要卡 + 展开详情表达。
- no-source 拒答是产品状态，不是错误大红警告。

## 计划约束

- 不复制外部代码。
- 不新增前端依赖。
- 不改变后端 API/DTO。
- 不让前端直连 LLM。
- 不在前端存储 API key。
- 不伪造生产指标。
