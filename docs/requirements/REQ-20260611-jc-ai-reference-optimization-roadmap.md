# REQ - 基于 jc-ai 参考的系统优化路线

## 1. Skill Selection Report

### Task Type

优化计划 / 架构路线 / RAG + Agent + Evaluation + Frontend + Security 治理规划。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 用户给出“优化计划”原始需求，项目规则要求走 S/M/L 工作流。 |
| ai-learning-architecture | 优化涉及多智能体职责、Orchestrator、工具边界、状态和失败降级。 |
| educational-rag-pipeline | jc-ai 的 RAG KB、HyDE、评测标注和 citations 需要映射到本项目 RAG 质量闭环。 |
| agent-trace-governance | 红队、Agent trace 评分、tool call、人审和 token/cost 都依赖 trace 治理。 |
| spring-ai-agent-backend | 后续优化会影响 Spring AI 模型网关、tool calling、SSE 和结构化输出。 |
| vue-edu-admin-frontend | 计划包含教师/管理员评测工作台、citation/trace/index 状态可视化。 |
| tech-review | 需要从可行性、性能、安全、可扩展和可运维性评审 jc-ai 模式是否适合本项目。 |
| security-review | MCP/A2A、HyDE、外部搜索、高风险工具和 outbound allowlist 涉及安全边界。 |

### Missing Skills

无。现有技能覆盖本轮规划。若后续进入 MCP/A2A POC，可再沉淀项目专属“protocol boundary review”技能。

### GitHub / External Research Needed

Yes。用户明确要求参考 `https://gitcode.com/jingdianjichi/jc-ai.git`。当前 GitCode clone/raw 获取不稳定，本轮使用用户提供的本地 zip 作为主要证据，并创建参考报告：

- `docs/research/github-references/GITHUB-20260611-jc-ai-optimization-reference.md`

### New Project-Specific Skill To Create

本轮不创建。若后续 MCP/A2A 或高风险工具审批形成稳定规则，再抽取项目技能。

## 2. Size Classification

Size: L

Reason:

- 覆盖 frontend、backend、RAG、Agent workflow、evaluation、security、operations、potential protocol integration。
- 涉及产品能力路线和架构边界，不是单模块小改。
- MCP/A2A、HyDE、高风险工具审批均可能影响依赖、安全和运行时治理。

Required Documents:

- `docs/product/PRD-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/requirements/REQ-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/specs/SPEC-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/plans/PLAN-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/tasks/TASK-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/context/CONTEXT-20260611-jc-ai-reference-optimization-roadmap.md`

Can Skip:

- 本轮不实施代码，可跳过依赖审查、DB migration、接口实现测试。
- 不需要创建独立 PRD 之外的产品原型。

Upgrade Trigger:

- 若用户要求直接实现 P0/P1 任一项，必须为该子任务重新进行 size classification，并创建对应 Context Pack。
- 若新增依赖、协议端口、数据库 schema、前后端 API 合同，必须升级到 M/L 实施流程。

## 3. Subagent Decision

Use Subagents: Gate required by project rules; no live spawn in this turn.

Reason:

- 任务涉及 3+ 模块且包含 Agent/RAG，项目规则要求 Multi-Expert Subagent Gate。
- 当前可用 `spawn_agent` 工具要求用户明确要求子代理或并行代理；本轮用户只要求优化计划。
- 因此本轮采用文档化专家评审，不实际启动子代理，不创建并行实现分支。

Parallelism Level: L1/L2 documented analysis.

Selected Expert Roles:

- Product Analyst
- Backend Expert
- Agent/RAG Expert
- Frontend Expert
- Security & Quality
- Integration Reviewer

Report:

- `docs/subagents/runs/RUN-20260611-jc-ai-reference-optimization-roadmap.md`

## 4. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | 输出一份 jc-ai 能力雷达，说明哪些模式可借鉴、哪些不采纳。 | Must |
| FR-02 | 优化路线必须优先利用本项目已有 RAG、Evaluation、Trace、Review Gate、RBAC 和 model gateway 基线。 | Must |
| FR-03 | 规划红队/Prompt 注入评测扩展，覆盖 prompt injection、jailbreak、privilege escalation、data exfiltration、citation bypass、tool abuse。 | Must |
| FR-04 | 规划 Agent Trace 评分能力，至少覆盖工具准确率、步骤效率、回答质量、citation 合规和安全拒答。 | Must |
| FR-05 | 规划教师/管理员 RAG 评测工作台，支持 expected-source / expected-chunk 标注、评测历史和运行对比。 | Must |
| FR-06 | 规划 HyDE 可选检索分支，但必须默认关闭，并受模型网关、token budget、trace、日志脱敏和权限过滤约束。 | Should |
| FR-07 | 规划高风险工具人审队列，用于未来 web search、MCP/A2A、外部系统写操作、代码分析等能力。 | Should |
| FR-08 | 规划学习工作流 step board / structured working memory，不允许任意自由记忆绕过学习档案和 trace。 | Should |
| FR-09 | MCP 必须先做 ADR、安全审查和依赖审查，不能直接引入运行时依赖。 | Must |
| FR-10 | A2A 必须先做 ADR，定位为未来跨服务 Agent 互操作，不替代内部 Orchestrator。 | Must |
| FR-11 | 前端优化必须使用 Vue 3 + TypeScript + 当前 API wrapper，不复制 `jc-rag-kb-front` React/Ant Design 代码。 | Must |
| FR-12 | 计划必须记录 no-code/no-dependency/no-schema-change 边界。 | Must |

## 5. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | 所有后续 AI 能力仍由后端持有模型、工具、密钥和权限判断。 |
| NFR-02 | RAG 或文件检索类输出必须保留 citations；无来源时必须有明确 no-source 或 fallback 策略。 |
| NFR-03 | Agent loop、tool calls、HyDE、MCP/A2A 均必须有最大轮数、超时、失败降级和 trace。 |
| NFR-04 | 任何外部调用必须先经过 outbound URL allowlist 和 provider 配置治理。 |
| NFR-05 | 任何新依赖必须创建 `docs/security/` 依赖审查并得到计划批准。 |
| NFR-06 | 前端不得回退到 GET EventSource 携带 question/kbIds/token。 |
| NFR-07 | 评测和 trace 记录不得保存原始密钥、完整私有文档、未脱敏工具输出或敏感个人数据。 |

## 6. Open Questions

- P0 首个实现切片应优先做红队评测，还是评测工作台 UI？
- HyDE 是否只在 teacher/admin 评测环境先开放，还是允许学生 QA 在配置开启后使用？
- 高风险工具审批队列是否复用现有 Resource Review Gate 表达，还是新建更通用的 Tool Approval 模型？
- MCP/A2A 是否有明确外部系统对接方？若没有，是否只保留 ADR 和 POC，不进入产品路线。
