# RUN-20260621：ChatGPT 级回答质量外部资料调研

## 调研定位

- 角色：外部资料 Research Expert
- 日期：2026-06-21
- Request Type：Comprehensive research（概念文档 + API/SDK 实现参考 + 评测/历史上下文）
- 范围：ChatGPT 级回答质量、OpenAI Responses API / Agents SDK、工具调用、状态/会话管理、Prompt 优化、评测闭环。
- 约束：只使用外部资料，不修改代码；核心依据优先 OpenAI 官方文档，其次权威论文/技术报告；不把二手博客作为核心依据。

## 直接结论

本项目要做“ChatGPT 级回答质量”，不应把重点放在单次 prompt 技巧，而应建立一条后端可治理链路：

1. 后端统一封装 OpenAI Responses API，作为新的模型调用与工具调用入口。
2. 会话状态由项目数据库主导，OpenAI 的 `previous_response_id` / conversation 能力作为续写和状态引用，不替代业务会话、权限、学习轨迹、RAG 引用和 Agent Trace。
3. 所有工具调用必须走 Service 层，使用严格 JSON Schema / Structured Outputs，工具结果必须带 `traceId`、来源、权限上下文和错误语义。
4. Prompt 按“任务开发者指令 + 结构化输入 + 输出 schema + citation 约束”版本化管理，避免把权限、工具边界、事实来源写成纯 prompt 约定。
5. 评测闭环以数据集、graders、trace、人工复核和回归阈值驱动；Prompt Optimizer 可作为辅助，但不能替代项目自己的基准集。
6. Agents SDK 的概念模式可借鉴，但本项目后端是 Java 21 + Spring Boot + Spring AI；除非未来做 Node/Python 边车，否则不建议直接把 Python/TypeScript Agents SDK 作为核心运行时依赖。

## 版本与资料上下文

- OpenAI 官方 API 文档当前明确把 Responses API 定位为新项目的推荐 API，并说明它支持内置工具、状态管理、MCP 等能力。
- OpenAI 官方 Agents SDK 文档显示 SDK 主要提供 Agent、Runner、工具、handoff、guardrails、session、tracing 等开发抽象，官方示例以 Python / TypeScript 为主。
- OpenAI Evals 文档带有重要时间上下文：旧 Evals API 计划于 2026-10-31 停止创建新 eval，2026-11-28 完全关闭；新工作应迁移到 Datasets、Graders、Stored Completions 和 Traces 相关路径。
- Prompt Optimizer 文档同样提示旧 optimizer API 有 2026-11-28 关闭节点；可用于辅助改写，但不应作为长期唯一优化链路。
- 本报告不做供应商/依赖选型结论；是否引入新 SDK 或新依赖，应另走项目依赖评审。

## 官方文档证据

- OpenAI Responses API migration guide
  https://developers.openai.com/api/docs/guides/migrate-to-responses
  说明 Responses API 是 OpenAI 当前面向 agentic 应用的高级接口，支持内置工具、MCP、状态管理，并建议新项目优先采用 Responses API。

- OpenAI Conversation state guide
  https://developers.openai.com/api/docs/guides/conversation-state
  说明 Responses API 可通过 Response ID 继续对话，也可使用 Conversations 对象保存输入/输出 item；同时文档给出手动管理上下文和自动状态管理两类路径。

- OpenAI Function calling guide
  https://developers.openai.com/api/docs/guides/function-calling
  说明工具调用流程、函数 schema、`strict` 模式、`tool_call` / `tool_result` 循环，以及并行工具调用对 schema 严格性的影响。

- OpenAI Tools guide
  https://developers.openai.com/api/docs/guides/tools
  汇总 Responses API 可用工具形态，包括 function calling、web search、file search、MCP、code interpreter、image generation、computer use 等。

- OpenAI Structured Outputs guide
  https://developers.openai.com/api/docs/guides/structured-outputs
  说明通过 JSON Schema 约束模型输出，适合把回答质量、引用、评分、后续动作等做成可校验结构。

- OpenAI Agents guide
  https://developers.openai.com/api/docs/guides/agents
  说明 Agents SDK 的核心原语：Agents、Handoffs、Guardrails、Tracing、Sessions 等，适合从概念上设计多 Agent 协作和可观测性。

- OpenAI Running agents guide
  https://developers.openai.com/api/docs/guides/agents/running-agents
  说明 SDK 中 Runner 运行 agent loop，并强调会话管理策略需要选择一致路径；对本项目的状态管理设计有直接参考价值。

- OpenAI Agent orchestration guide
  https://developers.openai.com/api/docs/guides/agents/orchestration
  说明 manager、handoff、agent-as-tool、deterministic workflow 等编排模式，适合映射到“规划 / 检索 / 生成 / 评估 / 精修”角色边界。

- OpenAI Guardrails and approvals guide
  https://developers.openai.com/api/docs/guides/agents/guardrails-approvals
  说明输入/输出 guardrails、人类审批、敏感工具保护等机制，适合 AI 资源生成、学习建议、外部动作调用前的质量与安全闸口。

- OpenAI Prompt engineering guide
  https://developers.openai.com/api/docs/guides/prompt-engineering
  给出 prompt 开发原则：明确目标、约束输出、拆解任务、提供上下文、使用 examples、持续评测等。

- OpenAI Reasoning best practices
  https://developers.openai.com/api/docs/guides/reasoning-best-practices
  说明推理模型的 instruction、上下文、工具与验证实践；对复杂学习路径规划、错因诊断、长程推理尤其相关。

- OpenAI Evals guide
  https://developers.openai.com/api/docs/guides/evals
  说明用 datasets、graders、stored completions、traces 构建评测工作流，并提示旧 Evals API 的停用时间。

- OpenAI Prompt Optimizer guide
  https://developers.openai.com/api/docs/guides/prompt-optimizer
  说明 Prompt Optimizer 可结合数据集、graders 和 trace 优化 prompt，同时提示旧 API 关闭窗口。

- OpenAI Model Spec
  https://model-spec.openai.com/
  提供 OpenAI 对消息层级、指令遵循、工具行为和安全边界的公开规范背景；适合作为 prompt 优先级和指令层级设计参考。

## 权威论文 / 技术报告补充依据

- ReAct: Synergizing Reasoning and Acting in Language Models
  https://arxiv.org/abs/2210.03629
  支撑“推理 + 行动 + 观察”的工具调用闭环。对本项目含义：工具调用不应是一次性 RPC，而应有可追踪的 observation、错误处理和后续决策。

- Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks
  https://arxiv.org/abs/2005.11401
  支撑 RAG 对知识密集型任务的必要性。对本项目含义：高质量学习回答要把“可追溯资料来源”作为回答质量的一部分，而不是事后附加装饰。

- Toolformer: Language Models Can Teach Themselves to Use Tools
  https://arxiv.org/abs/2302.04761
  支撑模型借助外部工具提升能力的方向。对本项目含义：工具要小而明确，输入输出可学习、可评测，避免一个万能工具承载过多业务语义。

- G-Eval: NLG Evaluation using GPT-4 with Better Human Alignment
  https://arxiv.org/abs/2303.16634
  支撑 LLM-as-judge 在生成质量评估中的实用性。对本项目含义：可以用 LLM grader 评估完整性、引用正确性、教学适配度，但关键指标仍需抽样人工复核。

- Self-Refine: Iterative Refinement with Self-Feedback
  https://arxiv.org/abs/2303.17651
  支撑“生成 - 反馈 - 精修”的迭代模式。对本项目含义：资源生成和错因诊断可以引入受限迭代，但必须设置最大轮数、trace 和停止条件。

## P0：立即落地的设计模式

### P0-1：Responses API 后端门面模式

适用场景：所有新 AI 对话、AI 辅导、资源生成、学习路径规划、评测解释。

设计建议：

- 在后端建立统一 `ModelGateway` / `OpenAIResponseService` 类抽象，业务服务不直接拼 OpenAI 请求。
- 默认面向 Responses API 建模：`input`、developer/system 指令、tools、structured output、metadata、traceId。
- Chat Completions 仅保留为历史兼容层，不作为新能力入口。
- 每次调用记录：model、promptVersion、tool schema version、conversation/session id、traceId、response id、latency、token usage、finish reason、error。

原因：

- OpenAI 官方文档已将 Responses API 定位为更适合新项目、状态管理和 agentic workflows 的接口。
- 统一门面能避免前端直接接触 LLM API，符合本项目架构规则。

落地注意：

- 不在前端存放模型 API key。
- 不让 Controller 直接构建复杂 prompt。
- 不把 OpenAI response id 当作业务主键；它是外部调用状态引用。

### P0-2：项目数据库主导的会话状态模式

适用场景：学习者长期画像、AI tutoring、多轮答疑、RAG citation、学习路径节点推进。

设计建议：

- 项目数据库保存权威业务状态：`learnerId`、`sessionId`、`traceId`、学习目标、知识点、资源、RAG chunk、工具调用、评估结果。
- OpenAI `previous_response_id` 或 Conversation ID 作为模型上下文续写引用，存入项目会话表或 trace 表。
- 对“短对话续写”可用 `previous_response_id`；对“长期学习记忆”必须通过项目自己的 learner profile、learning event、RAG context 重建。
- 每轮请求显式组装当前可用上下文：用户当前问题、学习者画像摘要、课程/知识点、检索片段、最近交互摘要、约束输出 schema。

原因：

- OpenAI 文档提供自动状态管理能力，但本项目有权限、学习记录、引用可见性和审计要求，不能把业务记忆交给模型会话隐式保存。
- 这也避免跨用户串话、权限漂移和上下文不可解释。

落地注意：

- 每个会话只选择一种主要状态策略，避免同一条链路同时混用手动消息拼接、`previous_response_id` 和长期 Conversations 导致不可复现。
- 对长期上下文使用摘要 + 检索，不把无限历史原文塞回 prompt。

### P0-3：严格工具契约 + Service 层执行模式

适用场景：检索课程资料、查询学习画像、生成资源、读取评测结果、更新学习路径。

设计建议：

- 工具 schema 使用 JSON Schema，并尽量开启 strict structured output。
- 每个工具只暴露一个清晰业务动作，例如 `retrieveCourseChunks`、`getLearnerProfileSummary`、`createResourceDraft`、`gradeSubmissionDraft`。
- 工具实现只能调用 Service 层，不能直接访问 Mapper / Repository。
- 工具结果返回结构化 observation：`status`、`data`、`citations`、`traceId`、`warnings`、`recoverable`。
- 对写操作、资源发布、路径状态变更加人工审批或后端权限校验。

原因：

- OpenAI function calling / tools 文档把工具 schema 和工具结果作为模型与外部系统协作边界。
- ReAct 和 Toolformer 的论文也支持“小工具 + 可观察反馈”的模式。

落地注意：

- Prompt 不能作为权限控制；权限必须在后端工具实现中检查。
- 工具失败要给模型可恢复错误，而不是抛裸异常文本。
- 不要设计一个 `doAnything` 式万能工具。

### P0-4：结构化回答质量 Schema 模式

适用场景：AI tutoring、RAG 问答、资源生成、错因诊断、学习路径解释。

设计建议：

- 为关键回答定义统一结构，例如：
  - `answer`
  - `reasoningSummary`
  - `citations`
  - `learnerFit`
  - `nextSteps`
  - `uncertainty`
  - `requiresReview`
  - `safetyFlags`
- RAG 回答的 `citations` 必填，且 citation 必须能映射到项目 chunk / document / resource。
- 对 AI 资源生成，输出初始状态为 `DRAFT` 或 `PENDING_REVIEW`，禁止模型直接发布。
- 对评测反馈，要求区分“事实依据”“推断”“建议练习”。

原因：

- OpenAI Structured Outputs 文档支持用 schema 约束可解析输出。
- RAG 论文和项目架构规则都要求知识密集型回答有来源依据。

落地注意：

- Schema 不要过度复杂；先覆盖 P0 质量字段。
- 前端展示可从结构化字段读取，不再解析自然语言。

### P0-5：Prompt 版本化与最小职责模式

适用场景：所有 agent prompt、RAG prompt、资源生成 prompt、评测 prompt。

设计建议：

- Prompt 拆成：角色/任务目标、输入变量说明、工具使用规则、输出 schema、质量标准、拒答/不确定性规则。
- 每个 prompt 有 `promptVersion`，与 eval 结果、trace、模型版本绑定。
- Developer 指令负责长期行为约束；用户输入只作为任务数据，不允许覆盖系统边界。
- 对同一任务保留少量高质量 examples，而不是堆长 prompt。
- 修改 prompt 必须跑最小回归 eval。

原因：

- OpenAI Prompt engineering 文档强调明确目标、上下文、输出格式和持续评测。
- Model Spec 可作为指令层级和工具边界的规范背景。

落地注意：

- 不把权限、审计、引用强制性仅写在 prompt 里。
- 不为每个页面复制一份 prompt；应模块化复用。

### P0-6：评测闭环优先模式

适用场景：ChatGPT 级回答质量的持续提升。

设计建议：

- 建立项目 eval dataset，覆盖：
  - RAG 引用正确性
  - 学习者画像适配度
  - 回答事实正确性
  - 教学可理解性
  - 工具调用正确性
  - 安全与越权拒绝
  - JSON schema 合规
- 使用混合 grader：
  - 规则 grader：schema、引用存在、禁止字段、权限边界。
  - LLM grader：教学质量、完整性、个性化程度。
  - 人工抽样：高风险反馈、低置信案例、资源发布前审核。
- 每次 prompt/model/tool 变更保存 before/after 结果。
- 评测结果进入 Agent Trace 或质量看板。

原因：

- OpenAI Evals 文档明确把 datasets、graders、traces 作为评测工作流核心。
- G-Eval 论文支持 LLM-as-judge 与人类偏好更一致，但仍需治理和人工校准。

落地注意：

- 不要只用线上用户反馈作为质量依据；必须有离线回归集。
- 旧 Evals API 有停用计划，应采用当前 Datasets / Graders / Traces 思路设计。

## P1：中期增强的设计模式

### P1-1：Manager + Specialist Agent 编排模式

适用场景：个性化资源生成、学习路径规划、复杂答疑。

设计建议：

- Manager 负责目标拆解、上下文裁剪、质量闸口。
- Specialist 作为 tool 使用，而不是都能自由 handoff：
  - Retriever：只负责检索与引用。
  - Generator：只负责生成资源草稿。
  - Evaluator：只负责质量检查和 rubric。
  - Refiner：只负责按反馈修订。
- Handoff 只用于“另一个 agent 应完整接管用户响应”的场景；普通子任务优先 agent-as-tool。

原因：

- OpenAI Agent orchestration 文档区分 manager、handoff、agent-as-tool、deterministic workflow。
- 本项目架构要求 Agent 角色边界清晰，避免 agent proliferation。

### P1-2：Guardrails + Human Review 模式

适用场景：AI 生成资源、评测反馈、学习路径建议、敏感个人画像更新。

设计建议：

- 输入 guardrails：识别越权、提示注入、跨用户数据请求、无课程上下文请求。
- 输出 guardrails：检查无引用 RAG、泄露隐私、过度诊断、错误发布状态、JSON 不合规。
- 人工审批：资源发布、教师可见反馈、路径重大调整。

原因：

- OpenAI Guardrails and approvals 文档提供输入/输出 guardrail 与审批模式。
- 本项目有教育场景和 AI-generated resource review，属于必须治理的生成内容。

### P1-3：Trace 驱动调试模式

适用场景：工具调用失败、RAG 答非所问、prompt 回归、模型升级。

设计建议：

- Trace 粒度覆盖：用户请求、上下文组装、检索 query、召回 chunk、模型请求、工具调用、grader 结果、最终回答。
- 每条 trace 可回放关键输入，但不记录敏感原文或密钥。
- 前端 Agent Trace Console 展示给教师/管理员的是裁剪后的可解释链路。

原因：

- Agents SDK 文档把 tracing 作为核心能力之一。
- Evals 文档把 traces 纳入评测和优化工作流。

### P1-4：Prompt Optimizer 辅助迭代模式

适用场景：已有 eval dataset 后，优化具体 prompt。

设计建议：

- 先手写 baseline prompt 和 eval dataset。
- Prompt Optimizer 只用于候选 prompt 改写。
- 候选 prompt 必须跑项目 eval，通过阈值和人工 spot check 后才能合并。

原因：

- OpenAI Prompt Optimizer 文档强调与 dataset、grader、trace 结合。
- 官方文档同时提示旧 optimizer API 关闭时间，因此不应绑定不可替代流程。

## P2：后续探索的设计模式

### P2-1：MCP 工具生态接入

适用场景：未来需要把多个外部教育系统、知识库、日历、任务系统接入 AI agent。

设计建议：

- 当前优先使用项目内 function tools。
- 当工具数量增长、需要标准协议或跨系统复用时，再评估 MCP。
- MCP 工具同样必须经过后端权限、审计和数据脱敏。

原因：

- OpenAI Tools / Responses 文档支持 MCP。
- 对当前 Java 后端项目，直接 function calling 更易治理。

### P2-2：更复杂的自我精修循环

适用场景：高价值资源生成、长答案润色、学习路径方案比较。

设计建议：

- 生成、评审、修订分离。
- 每次最多 1-2 轮自动修订。
- 超过阈值转人工 review，不允许无界循环。

原因：

- Self-Refine 支持迭代改进方向。
- 本项目架构规则要求 Agent loop 有最大轮数。

### P2-3：模型自动路由与成本质量分层

适用场景：线上规模化后平衡成本、延迟和质量。

设计建议：

- 简单分类、schema 校验、摘要可用低成本模型。
- 学习路径规划、复杂错因诊断、高风险反馈使用更强推理模型。
- 路由策略必须进入 eval 回归，不按主观感觉切换。

原因：

- OpenAI Reasoning best practices 支持按任务复杂度设计推理调用。
- 本项目需要在教学质量、成本和响应速度之间做可观测权衡。

## 本项目建议的参考架构

```text
Vue Frontend
  -> Spring Boot Controller
  -> AI Application Service
  -> Context Builder
       -> Learner Profile Service
       -> Course / Resource Service
       -> RAG Retrieval Service
       -> Recent Session Summary
  -> OpenAI Responses Gateway
       -> Prompt Version Registry
       -> Tool Schema Registry
       -> Structured Output Schema
  -> Tool Dispatcher
       -> Service-layer tools only
       -> Permission checks
       -> Trace logging
  -> Quality Gate
       -> Schema validation
       -> Citation validation
       -> Guardrails
       -> Optional LLM grader
  -> Persist
       -> Agent Trace
       -> Model Call Log
       -> Token Usage
       -> Evaluation Result
       -> Review Status
```

关键原则：

- 前端只消费后端结构化结果，不直接调用 OpenAI。
- Prompt、tool schema、output schema、eval dataset 同版本演进。
- RAG 来源、工具 observation、grader verdict 都进入 trace。
- AI 生成内容默认进入 review workflow，而不是直接发布。

## 可复用检查清单

### 开发前

- 是否明确本任务属于 tutoring、RAG QA、resource generation、path planning 还是 evaluation？
- 是否已有 promptVersion 和 output schema？
- 是否列出允许调用的工具？
- 是否定义 citation / trace / review 要求？
- 是否有至少 5-20 条最小 eval case？

### 开发中

- 工具是否只走 Service 层？
- 工具输入是否有 JSON Schema？
- 工具输出是否有 traceId、status、citations 或 warnings？
- 模型回答是否可被后端 schema validator 校验？
- 会话状态是否由项目 DB 主导？

### 上线前

- 是否跑过 eval dataset？
- 是否比较了 baseline 与新版本？
- 是否抽样人工复核？
- 是否记录 prompt/model/tool schema 版本？
- 是否有失败回滚策略？

## Caveats / Ambiguity Flags

- OpenAI 官方文档变化较快；实现前应再次确认 Responses API、Agents SDK、Evals、Prompt Optimizer 的当前页面和 SDK 版本。
- Agents SDK 官方示例主要是 Python / TypeScript；本项目是 Java/Spring AI，直接引入 SDK 可能意味着新增运行时或边车服务，应走依赖评审。
- LLM-as-judge 能提升评测覆盖，但不能替代人工审核，尤其在教育评价、个性化建议和资源发布场景。
- OpenAI 自动状态能力不能替代项目内权限、学习轨迹、RAG 引用、审计和合规要求。

## Reusable Takeaway

适合本项目的核心落地路线是：

> 以后端 Responses API 门面为统一入口，以项目数据库主导会话和学习记忆，以严格工具 schema + Service 层执行保障边界，以结构化输出和 citation 保障可见性，以 eval dataset + graders + trace 建立持续质量闭环。Agents SDK 的编排、guardrails、sessions、tracing 概念值得吸收，但不应在没有依赖评审的情况下直接替代 Java/Spring AI 后端架构。
