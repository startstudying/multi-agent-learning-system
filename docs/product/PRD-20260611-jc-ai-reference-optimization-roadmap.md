# PRD - 基于 jc-ai 参考的系统优化路线

## 1. 背景

用户希望参考 `jc-ai` 仓库，为当前 AI 个性化学习多智能体系统做一次优化计划。`jc-ai` 覆盖 Java AI 应用中的 RAG、Agent、MCP、A2A、Prompt、评测、语音导购和前端工作台样例，适合作为能力雷达参考。

当前项目已经完成大量生产化基础：RAG citation、Agent Trace、模型网关、token/cost、评测集、Review Gate、权限矩阵、POST streaming、Qdrant adapter、可观测性等。因此本次优化不是“照搬 jc-ai”，而是基于参考仓库识别下一阶段高收益优化主题。

## 2. 产品目标

- 将外部参考能力转化为本项目可执行的优化路线。
- 聚焦 AI 学习系统的质量闭环：RAG/Agent 评测、红队、trace 评分、引用标注、教师/管理员工作台。
- 补齐未来工具调用和协议集成前的治理边界：高风险工具审批、MCP/A2A ADR、outbound allowlist、scoped token budget。
- 保持当前生产安全基线：后端拥有 AI 调用、前端不接触密钥、RAG 必须有 citations、Agent 必须 trace、工具必须经 Service 层。
- 形成可拆分、可验收、可逐步实施的 P0/P1/P2/P3 路线，而不是一次性大改。

## 3. 用户价值

| 用户 | 价值 |
|---|---|
| 学生 | 获得更可信的 AI 答疑、资源推荐和学习路径，能看到来源、状态和必要解释。 |
| 教师 | 能管理评测样本、标注期望来源、审查 AI 输出质量和高风险工具动作。 |
| 管理员 | 能通过红队、trace 评分、成本预算和协议边界判断系统是否可安全扩展。 |
| 开发者 | 有明确优先级和上下文包，后续实现不会在 RAG、Agent、MCP/A2A 中盲目扩散。 |

## 4. 产品范围

本轮只交付优化路线和配套文档，不改运行时代码。

本轮纳入规划：

- RAG/Agent 质量评测工作台。
- 红队与 Prompt 注入评测。
- Agent Trace 评分。
- RAG expected-source / expected-chunk 标注体验。
- HyDE 可选检索分支治理。
- 高风险工具人审队列。
- 学习工作流 step board / structured working memory。
- MCP/A2A 协议边界 ADR。
- 前端 Citation/Trace/Eval/Index 状态组件抽取。
- Outbound allowlist 与 user/course/agent scoped token budget。

本轮不纳入：

- 不复制 `jc-ai` 代码。
- 不引入新依赖。
- 不修改 API、DTO、数据库 schema。
- 不实现 MCP/A2A、语音、多模态或新 Agent runtime。
- 不改变当前安全、权限和 Review Gate 规则。

## 5. 成功指标

- 形成一份 P0-P3 优化路线，且每一项都有目标、边界、风险和验证方式。
- 第一优先级任务可以直接进入项目 S/M/L workflow。
- 所有建议都保持当前架构规则：后端 AI 调用、Service 层工具边界、权限代码执行、RAG citations、Agent Trace。
- 明确哪些 jc-ai 模式可借鉴，哪些不采纳。
- 产生研究报告、PRD/REQ/SPEC/PLAN/TASK/CONTEXT、Evidence 和 Acceptance。

## 6. 推荐里程碑

| 里程碑 | 目标 |
|---|---|
| M1 | 完成本轮路线文档和参考研究，确定 P0 优化方向。 |
| M2 | 实施红队/Prompt 注入评测扩展，并接入现有 Evaluation Run。 |
| M3 | 实施 RAG/Agent 质量工作台第一版，支持 expected chunks 标注和 trace 评分展示。 |
| M4 | 评估并实现受控 HyDE 分支和高风险工具审批队列。 |
| M5 | 完成 MCP/A2A ADR，决定是否进入 POC。 |
