# PLAN - 基于 jc-ai 参考的系统优化路线

## 1. Current State

当前项目已经完成较强的生产基线：

- RAG parser/provider、chunk metadata、embedding/vector adapter、Qdrant adapter、hybrid/RRF/reranker fallback。
- RAG citation governance、strict permission filter、no-source/fallback 策略。
- Evaluation Set/Run、Prompt Version comparison、RAG quality evaluation、grading evaluation。
- Agent Trace、tool call governance、model_call_log、token_usage_log、token/cost governance。
- Review Gate、Resource citation、AI-generated resource publish 审核。
- JWT/OAuth2/JWK、roles-first RBAC、对象级权限和反枚举矩阵。
- Vue 角色工作区、AI QA 入口、POST production streaming。

因此本轮优化重点不是补基础链路，而是把质量闭环、红队、安全工具治理和前端运营体验做成下一阶段路线。

## 2. Integrated Expert Result

| Expert | Result |
|---|---|
| Product Analyst | jc-ai 适合做能力雷达。当前项目应优先补“评测/治理/运营工作台”，而不是重写 RAG 或 Agent。 |
| Backend Expert | P0 应复用现有 Evaluation/Trace/Model/RAG 表达；新增 schema 必须由子任务证明必要性。 |
| Agent/RAG Expert | HyDE、working memory、人审和工具调用都必须受 permission、trace、max rounds、token budget 约束。 |
| Frontend Expert | 借鉴 jc-rag-kb-front 的信息架构，但用 Vue 当前工作区实现，不复制 React/AntD。 |
| Security & Quality | MCP/A2A、web search、代码分析和外部写工具必须先有 ADR、依赖审查、allowlist 和审批队列。 |
| Integration Reviewer | 第一实现切片建议从红队/Prompt 注入评测开始，风险小、收益高、复用现有能力最多。 |

## 3. Recommended Roadmap

### P0-1 红队与 Prompt 注入评测扩展

Size: M

目标：

- 在现有 Evaluation Set/Run 中新增安全评测类型。
- 覆盖 prompt injection、jailbreak、privilege escalation、data exfiltration、citation bypass、tool abuse。
- 输出安全通过率、拒答准确率、越权泄露率、citation 合规率。

边界：

- 不新增外部依赖。
- 不改变模型供应商。
- 优先 Mock/fixture 评测，后续再接真实模型回归。

验证：

- Evaluation service/controller focused tests。
- 安全样本 verdict 聚合测试。
- Evidence archive 生成检查。

### P0-2 RAG / Agent 质量工作台

Size: M or L

目标：

- 教师/admin 可管理评测样本。
- 支持 expected-source / expected-chunk 标注。
- 展示 RAG evaluation、Agent trace scoring、run comparison。

边界：

- 前端使用 Vue 3 + TypeScript 和现有 API wrapper。
- 后端先复用现有 evaluation APIs；不足时再设计最小 API。
- 不暴露无权限 chunk 全文。

验证：

- 前端 loading/error/empty/done 状态测试。
- API scope 和 object permission 测试。
- 移动端/桌面布局截图。

### P0-3 Agent Trace Scoring

Size: M

目标：

- 对已存在 trace/tool/model/token 记录计算质量指标。
- 评分维度包括 tool accuracy、step efficiency、answer quality、citation compliance、safety compliance、cost efficiency。
- 支持按 evaluation run 或 agent task 查看评分证据。

边界：

- 不保存原始敏感工具输出。
- 评分结果先作为评测指标，不直接影响生产决策。

### P1-1 HyDE 可选 RAG 分支

Size: M

目标：

- 增加默认关闭的 HyDE query rewrite 分支。
- 在评测环境先验证 Recall/Citation 改善。
- 失败降级到原始 query。

硬约束：

- allowed KB scope 先于检索执行。
- HyDE 生成通过后端模型网关。
- token/cost/trace 必须记录。
- HyDE 文本不进入普通日志和前端响应。

### P1-2 高风险工具审批队列

Size: L

目标：

- 为未来 web search、MCP/A2A、外部系统写操作、代码分析建立通用审批队列。
- 审批状态与 agent_task / agent_trace / agent_tool_call 绑定。
- 支持 approve/reject/expire/cancel。

边界：

- 不替代 Resource Review Gate；先定义通用 tool approval。
- 高风险工具默认关闭。

### P1-3 学习工作流 Step Board

Size: M

目标：

- 为复杂 QA、资源生成、路径规划记录结构化当前步骤、待决策项、已检索来源和下一步。
- 替代不可审计的自由 working memory。

边界：

- 不把 step board 当作长期 learner profile。
- 不存储未脱敏私有文档全文。

### P1-4 Outbound URL Allowlist 与 Scoped Token Budget

Size: M

目标：

- 所有 provider/webhook/search/MCP/A2A 外呼走 outbound allowlist。
- token budget 从全局扩展到 user/course/agent scope。

边界：

- 先覆盖后端 provider/webhook/search 类外呼。
- 前端不参与密钥或 provider URL 配置。

### P2-1 MCP 边界 ADR

Size: M planning

目标：

- 决定是否、何时、如何暴露 MCP server/client。
- 明确认证、scope、trace、tool registry、资源模板、关闭开关。

边界：

- ADR 通过前不引入运行时依赖。
- 不开放公网协议入口。

### P2-2 A2A 边界 ADR

Size: M planning

目标：

- 判断 A2A 是否用于未来跨服务 Agent 协作。
- 设计 task/send/get/cancel、webhook、SSE 和 traceId 传播边界。

边界：

- 不替代当前内部 Orchestrator。
- 没有明确外部对接方时只做研究和 POC。

### P2-3 语音/多模态学习助理可行性

Size: L planning

目标：

- 参考 voice shopping 场景，评估语音问答、听说训练、口语反馈、课堂辅助。

边界：

- 等 AI QA、RAG、评测和权限闭环稳定后再做。
- 必须先完成音频隐私、存储、转写 provider 和成本审查。

### P3-1 前端质量组件抽取

Size: S/M

目标：

- 抽取 `CitationPanel`、`TraceTimeline`、`EvalMetricCard`、`IndexStatusBadge`、`NoSourceState` 等组件。

边界：

- 不改变 API 合同。
- 不做大规模 UI 重写。

## 4. Recommended First Slice

推荐优先实施：

```text
P0-1 红队与 Prompt 注入评测扩展
```

理由：

- 复用现有 Evaluation Set/Run 和安全治理基础。
- 不需要新增依赖或协议端口。
- 能直接提高 AI QA、RAG、Agent 后续迭代的质量门槛。
- 可为 HyDE、MCP/A2A、工具审批等后续高风险能力提供回归基线。

## 5. Risk Controls

- 新依赖：必须先写 `docs/security/` dependency review。
- 外部调用：必须有 outbound allowlist、timeout、safe error、trace。
- 工具调用：必须有 risk level、max calls、approval gate、sanitized summary。
- RAG：必须先权限过滤，后检索/生成；citation 不可伪造。
- Frontend：不得直接接触模型、密钥、搜索、MCP/A2A 或任意文件系统。
- Logs：不得记录原始密钥、完整私有文档、未脱敏工具输出和敏感个人数据。

## 6. Test Strategy

本轮文档任务只做文档存在性和内容检索验证。

后续实现子任务应按风险运行：

- Backend focused tests: Evaluation service/controller、trace scoring、HyDE policy、approval state。
- Backend adjacent tests: RAG query、Agent trace、RBAC、model gateway、token budget。
- Frontend tests: Eval workbench state、chunk annotation、trace score rendering。
- Security tests: prompt injection verdict、forged object scope、tool approval bypass、URL allowlist。
- Full tests: M/L 子任务在集成风险较高时运行。

## 7. Architecture Drift Result

本轮不改运行时代码、API、DB 或依赖，未产生实际架构漂移。后续每个实现切片必须重新执行 Architecture Drift Check。
