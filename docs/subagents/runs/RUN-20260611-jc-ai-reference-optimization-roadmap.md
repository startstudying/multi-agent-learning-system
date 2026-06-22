# Multi-Expert Gate - jc-ai 参考优化路线

## 1. Gate Mode

本轮任务按项目规则触发 Multi-Expert Subagent Gate。由于当前可用子代理工具要求用户明确请求“子代理/并行代理”才可实际 spawn，而用户仅要求优化计划，本轮不启动工具级子代理。

以下内容为主线 Codex 基于专家角色的文档化评审，用于满足 L 级规划的多专家分析和集成决策。

## 2. Product Analyst

结论：

- `jc-ai` 是能力集合型参考，适合作为产品能力雷达。
- 当前项目已经有较强生产治理，不应从示例仓库反向重写基础架构。
- 下一阶段最有价值的是让教师/admin 能看到并管理 AI 质量闭环。

建议：

- P0 聚焦红队评测、RAG/Agent 质量工作台、Trace 评分。
- P1 再做 HyDE、高风险工具审批和 step board。
- MCP/A2A 只作为 P2 ADR，不进入近期实现。

## 3. Backend Expert

结论：

- 当前 backend 已有 Evaluation、RAG、Trace、Model Gateway、Token/Cost、RBAC 等基础。
- P0 任务应最大化复用现有表和服务，避免新增 schema。
- HyDE 如实施必须通过 `AiModelGateway`，不能在 RAG service 中直接调用 provider。

风险：

- 若把 `jc-ai` 的 PGVector/JPA 示例直接搬入，会冲突当前 Qdrant adapter 和权限过滤路线。
- 若评测工作台为了 UI 便利暴露 chunk 全文，会形成权限和隐私风险。

## 4. Agent/RAG Expert

结论：

- `jc-ai` 的 HyDE、expectedChunkIds、Agent trace evaluator、human approval 都有启发。
- 本项目已有更严格的 citations 和 trace 基线，应把这些模式纳入治理，而不是作为自由功能。

建议：

- HyDE 先以评测开关验证质量增益。
- Agent trace scoring 先计算指标，不直接驱动自动决策。
- Working memory 只允许结构化 step board，不允许无约束长期记忆。

## 5. Frontend Expert

结论：

- `jc-rag-kb-front` 的页面组织有参考价值：Chat、KnowledgeBase、Documents、Evaluation、Dashboard。
- 本项目必须使用 Vue 3 + TypeScript 当前设计系统实现，不能复制 React/AntD。
- 生产流式通道已避免 GET query 泄露，不能倒退到 EventSource query 参数。

建议：

- 优先做 Evaluation Workbench，而不是再做大范围 UI 重写。
- 抽取 CitationPanel、TraceTimeline、EvalMetricCard、IndexStatusBadge。
- 管理端图表只显示真实 API 数据，不做虚假占位指标。

## 6. Security & Quality

结论：

- MCP、A2A、web search、代码分析和外部写工具都是高风险边界。
- jc-ai 示例中的简单 API key/filter 只能作为概念参考，不能作为生产安全方案。

必须项：

- outbound URL allowlist。
- tool risk level。
- human approval gate。
- token/cost budget by user/course/agent。
- safe error and sanitized trace。
- dependency/security review before any new SDK。

## 7. Integration Reviewer

综合结论：

- 本轮路线通过，前提是保持 no-code/no-dependency/no-schema-change。
- 推荐第一实现切片：`P0-1 红队与 Prompt 注入评测扩展`。
- MCP/A2A 不进入实现，只进入 ADR。
- HyDE 不作为默认 RAG 主链路，只作为受控可选分支。

冲突处理：

- Product 希望尽快补工作台，Security 建议先有安全评测基线；集成结论是先做红队评测，再做工作台展示。
- RAG Expert 认为 HyDE 有潜在收益，Backend/Security 要求默认关闭；集成结论是 P1 评测驱动、配置关闭、trace/token 受控。
