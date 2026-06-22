# jc-ai 参考仓库优化研究报告

## 1. 来源与取证范围

- 用户给定参考地址：`https://gitcode.com/jingdianjichi/jc-ai.git`
- 本地参考包：`D:/迅雷下载/jc-ai-master (1).zip`
- 解压目录：`.tmp/external-references/jc-ai-zip/jc-ai-master`
- 取证时间：2026-06-11

GitCode 页面可访问但主要内容以 SPA 形式加载；直接 `git clone`、raw/archive/API 路径在当前环境中不可稳定取得仓库内容。因此本次分析以用户提供的本地 zip 为主证据，仅把 GitCode 地址作为来源标识。

重要约束：`jc-ai` README 带有版权和二次公开限制说明。本项目只抽象工程模式和能力方向，不复制代码、不搬运实现、不新增依赖。

## 2. 仓库性质判断

`jc-ai` 更像 Java AI 应用课程/样例集合，不是一个统一的生产级业务系统。它覆盖 Spring AI、LangChain4j、AgentScope、RAG、Agent、MCP、A2A、Prompt、评测和工程化主题，并通过多个独立模块展示能力。

主要模块：

| 模块 | 参考价值 |
|---|---|
| `jc-rag-kb` | 企业知识库 QA、文档上传、RAG 查询、评测数据集、HyDE、混合检索、上下文裁剪 |
| `jc-rag-kb-front` | KB/文档/评测/聊天/Dashboard 管理页面，包含 expectedChunkIds 标注思路 |
| `eval-framework` | Prompt 评测、回归基线、红队数据集、Agent Trace 评分 |
| `agent-ai` / `agent-scope-ai` | 工具调用、Human-in-the-loop 审批、记忆、计划执行、Hook/Monitor 思路 |
| `mcp-tools-server` / `mcp-tools-client` | MCP Server/Client、工具暴露、资源、Prompt、简单 API key 过滤 |
| `a2a-server` / `a2a-client` | Agent Card、JSON-RPC task、SSE、Webhook、异步任务状态 |
| `jc-sales-agent` / `jc-voice-shopping` | 业务 Agent 场景包装和语音/导购类体验启发 |

## 3. 可借鉴模式

### 3.1 RAG 质量闭环

`jc-rag-kb` 的有价值模式不是具体代码，而是“知识库运营 + 评测标注 + 检索质量迭代”的闭环：

- 文档上传后展示解析、索引、chunk 数量和状态。
- 评测样本支持 `expectedChunkIds`，便于衡量 Recall@K 和 citation 命中。
- 检索链路包含 HyDE、混合检索、RRF、reranker、confidence filter、context trimming。
- 前端提供评测数据集、chunk 标注、评测历史和知识库统计入口。

本项目现状：已经有 RAG parser/provider、chunk metadata、embedding/vector adapter、Qdrant adapter、hybrid/RRF/reranker fallback、RAG quality evaluation、citation governance 和严格权限过滤。更值得补的是“教师/管理员可操作的评测工作台”和“可治理的 HyDE 可选分支”，不是重写 RAG 主链路。

### 3.2 评测与红队

`eval-framework` 提供了独立评测模块思路：

- Prompt evaluation、A/B testing、regression baseline/check。
- Red-team runner/dataset，覆盖 prompt injection、privilege escalation、jailbreak、data exfiltration 等攻击类型。
- Agent trace evaluator，可从 tool accuracy、step efficiency、answer quality 等维度评分。

本项目现状：已有 Evaluation Set/Run、Prompt Version quality comparison、RAG quality evaluation、grading evaluation、Agent Trace governance、model/token/cost logging。下一步最有收益的是在现有评测模型上扩展红队/Prompt 注入样本和 Agent Trace 评分，而不是引入新的独立评测框架。

### 3.3 Agent 人审与高风险工具治理

`agent-ai` / `agent-scope-ai` 展示了 human approval、pending approval、hook/monitor、working memory、plan-and-execute 的样例。

本项目现状：已有 AI-generated resource Review Gate，但主要围绕资源发布。若未来加入 web search、MCP、A2A、邮件、外部系统写操作、代码分析等工具，需要更通用的“高风险工具审批队列”：

- 工具执行前判断风险等级。
- 高风险工具进入 `PENDING_APPROVAL`。
- 教师/管理员批准或拒绝后继续/终止 workflow。
- 审批动作写入 trace、tool call 和审计记录。

### 3.4 MCP / A2A 协议边界

`jc-ai` 展示了 MCP server/client 和 A2A task/SSE/webhook 的基本形态。对本项目而言，这两类能力不能直接当作功能需求落地，需要先做 ADR：

- MCP 适合作为“受信工具客户端访问本系统能力”的协议边界。
- A2A 适合作为未来跨服务 Agent 协作或外部 Agent 互操作边界。
- 当前项目已经有内部 Orchestrator，不应为了协议示例而拆散现有单体边界。
- 任何 MCP/A2A 引入前都必须完成认证、授权、scope、allowlist、traceId、tool/service boundary、依赖审查和运维关闭开关设计。

### 3.5 前端工作台

`jc-rag-kb-front` 的页面结构对本项目前端有启发：

- Chat 页面展示 citations、latency、来源。
- KnowledgeBase / KbDocuments 展示文档索引状态和 chunk 数。
- Evaluation 页面管理评测样本、expectedChunkIds 标注、评测历史。
- Dashboard 展示 token、文档、查询、命中情况。

本项目必须使用 Vue 3 + TypeScript + 当前设计系统实现，不能复制 React/Ant Design 代码。另一个关键差异：`jc-rag-kb-front` 使用 GET EventSource 携带问题参数，本项目已完成生产 POST streaming，不能倒退。

## 4. 与当前项目能力对比

当前项目已经更强的部分：

- Spec-first workflow、Project Memory、Evidence/Acceptance 和 changelog 纪律。
- Java 21 + Spring Boot 3.x + Spring AI 的生产边界。
- JWT/OAuth2/JWK、roles-first RBAC、对象级权限和反枚举矩阵。
- RAG citation governance、no-source 策略、POST production streaming。
- Agent Trace、model_call_log、token_usage_log、tool_call governance。
- Review Gate、AI 资源发布审核和 source citation 强约束。
- RAG parser/provider、OCR provider SPI、VectorDB adapter、hybrid/RRF/reranker fallback。
- opt-in 外部 smoke、health/ops alerts、Micrometer 观测。

当前项目可优化的部分：

- 缺少一个从外部参考归纳出来的能力雷达和优化路线。
- 评测工作台对红队、Prompt 注入、Agent trace 评分的覆盖还不够系统。
- RAG 质量闭环缺少可视化 expected-source/chunk 标注体验。
- HyDE 可作为可选增强，但必须放在后端模型网关、token budget 和 trace 治理之内。
- 高风险工具执行缺少通用人审队列。
- MCP/A2A 目前应作为协议研究和 ADR，不应直接实现。
- Outbound URL allowlist、user/course/agent scoped token budget 仍是治理补强项。

## 5. 建议转化为本项目路线

| 优先级 | 优化方向 | 来源启发 | 本项目落点 |
|---|---|---|---|
| P0 | 红队/Prompt 注入评测扩展 | `eval-framework` | 基于现有 Evaluation Set/Run 增加安全评测类型和基线 |
| P0 | RAG/Agent 评测工作台 | `jc-rag-kb-front` + `eval-framework` | 教师/管理员可管理样本、expected chunks、运行对比、trace 评分 |
| P0 | Agent Trace 评分 | `AgentTraceEvaluator` | 对工具准确率、步骤效率、回答质量、citation 合规评分 |
| P1 | HyDE 可选检索分支 | `QueryRewriterService` | 默认关闭，受 token/cost/trace/日志脱敏约束 |
| P1 | 高风险工具人审队列 | `HumanApprovalHook` / `ApprovalService` | 通用 approval gate，服务层权限与 trace 绑定 |
| P1 | 工作流 step board / working memory | Agent memory examples | 对学习路径/资源生成/QA 只记录结构化状态，不做任意自由记忆 |
| P2 | MCP 边界 ADR | MCP modules | 先研究和依赖/安全审查，不直接接入 |
| P2 | A2A 边界 ADR | A2A modules | 仅面向跨服务 Agent 互操作，不替代内部 Orchestrator |
| P2 | 语音/多模态学习助理可行性 | `jc-voice-shopping` | 等 QA/RAG/评测闭环稳定后评估 |
| P3 | 前端状态组件抽取 | `jc-rag-kb-front` 页面结构 | CitationPanel、TraceTimeline、EvalMetricCard、IndexStatus 等 |
| P3 | 运维治理补强 | 工程化模块 | outbound URL allowlist、scoped token budget、scheduled eval runner |

## 6. 不采纳项

- 不复制 `jc-ai` 的代码、前端页面、表结构或依赖版本。
- 不把 PGVector 替代当前 Qdrant adapter 路线。
- 不恢复 GET EventSource 携带 query 参数。
- 不把 MCP/A2A 作为短期必须能力。
- 不把示例仓库中的简化鉴权、安全过滤、日志输出直接应用到生产系统。

## 7. 结论

`jc-ai` 的最大价值是能力地图：RAG 知识库、评测、Agent、人审、MCP/A2A、前端工作台。当前项目已经具备更强的生产治理基线，因此优化重点应从“补基础能力”转向“质量闭环、评测红队、工具治理和可视化运营”。
