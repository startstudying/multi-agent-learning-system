# REQ - AI 问答能力六阶段演进路线

## 1. Skill Selection Report

### Task Type

新产品能力路线规划 / Agent workflow / Frontend + Backend API linkage / RAG and tool-calling governance。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 用户给出原始功能路线，必须走项目 S/M/L 工作流。 |
| brainstorming | 六个版本属于产品和架构设计，需要先收敛方案，不直接编码。 |
| ai-learning-architecture | 涉及 Orchestrator、多 Agent、状态机、工具边界、失败降级。 |
| spring-ai-agent-backend | 后端涉及 Spring AI、模型网关、结构化输出、SSE、tool calling。 |
| vue-edu-admin-frontend | 前端涉及 Vue 问答界面、模式选择、流式状态、Trace/Citation 展示。 |
| educational-rag-pipeline | 文件检索和课程资料问答必须保持 RAG citation 和权限过滤。 |
| agent-trace-governance | 工具调用、多 Agent、思考摘要和模型调用都必须可追踪、可审计。 |
| security-review | 联网搜索、文件检索、代码分析和多 Agent 工具执行涉及权限与数据泄漏风险。 |

### Missing Skills

无。现有项目技能可以覆盖路线设计；联网搜索工具具体实现时可能需要后续依赖审查或项目专属工具安全技能。

### GitHub Research Needed

No。本轮只做项目内路线规划，已有 Spring AI / RAG / Orchestrator / SSE / Trace 基线足够。后续若选择具体联网搜索 SDK、代码分析引擎或 Agent graph 框架，需要单独 GitHub/官方文档参考和依赖审查。

### New Project-Specific Skill To Create

暂不创建。若 V5/V6 落地时形成可复用的“受控工具调用网关”模式，再沉淀项目技能。

## 2. Size Classification

Size: L

Reason:

- 涉及前端、后端、RAG、工具调用、多 Agent 编排、Trace、安全和测试。
- V5/V6 可能影响外部联网、文件检索、代码分析工具边界和 Agent 状态机。
- 这是产品能力路线，不是单模块小改。

Required Documents:

- `docs/product/PRD-20260611-ai-qa-evolution-roadmap.md`
- `docs/requirements/REQ-20260611-ai-qa-evolution-roadmap.md`
- `docs/specs/SPEC-20260611-ai-qa-evolution-roadmap.md`
- `docs/plans/PLAN-20260611-ai-qa-evolution-roadmap.md`
- `docs/tasks/TASK-20260611-ai-qa-evolution-roadmap.md`
- `docs/context/CONTEXT-20260611-ai-qa-evolution-roadmap.md`

Can Skip:

- 本轮不写代码，因此不需要依赖评审、DB migration、接口实现 evidence。

Upgrade Trigger:

- 若下一步要求直接实现 V5/V6，必须拆分成更小子任务，并在新增依赖、联网搜索、代码分析或文件访问前补充安全/依赖评审。

## 3. Subagent Decision

Use Subagents: Yes for documented expert analysis gate; no live subagent coding.

Reason:

- 项目规则要求 3+ 模块和 Agent/RAG 任务启用 Multi-Expert Subagent Gate。
- 当前用户没有明确要求实际并行代理改代码，所以本轮采用文档化专家分析，不启动并行实现。

Parallelism Level: L1 / L2 planning analysis.

Selected Subagents:

- Product Analyst
- Frontend Expert
- Backend Expert
- Agent/RAG Expert
- Security & Quality
- Integration Reviewer

Implementation Mode:

- Single Codex documentation planning.

Expert Report:

- `docs/subagents/runs/RUN-20260611-ai-qa-evolution-roadmap.md`

## 4. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | 系统应提供统一 AI 问答入口，支持普通问答请求和响应。 | Must |
| FR-02 | 请求应支持 `answerMode`，枚举为 `FAST`、`THINKING`、`EXPERT`。 | Must |
| FR-03 | 响应应支持 `reasoningSummary`，仅展示安全摘要，不展示原始思维链。 | Must |
| FR-04 | 流式问答应通过后端 SSE/ReadableStream API 输出 `status`、`token`、`done`、`error`。 | Must |
| FR-05 | 工具调用必须由后端 Agent Tool 触发，前端不得直接访问联网搜索、文件检索或代码分析能力。 | Must |
| FR-06 | 工具调用必须记录到 `agent_tool_call` 或等价治理记录中。 | Must |
| FR-07 | 文件检索/RAG 回答必须返回 citations，无来源时明确 no-source 状态。 | Must |
| FR-08 | 多 Agent 工作流必须包含规划、检索、审查、生成角色边界。 | Must |
| FR-09 | 多 Agent 工作流必须有最大轮数、最大工具调用次数、失败降级和取消策略。 | Must |
| FR-10 | 前端必须展示模式、流式状态、思考摘要、引用、工具调用摘要和 traceId。 | Should |
| FR-11 | OpenAI 主模型目标应支持配置为 `gpt-5.5`，后端模型网关负责把业务模式映射到受控模型参数。 | Must |
| FR-12 | `FAST` / `THINKING` / `EXPERT` 应优先映射到 `reasoning.effort` 策略，而不是由前端直接传递模型内部参数。 | Must |
| FR-13 | 后端应优先通过 Responses API 形态承载推理摘要、工具调用和结构化输出；若当前 Spring AI 适配层暂不支持完整 Responses API 能力，必须在 Slice SPEC 中记录兼容路径和限制。 | Should |

## 5. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | 所有 AI 调用由后端持有供应商配置和 API key。 |
| NFR-02 | Prompt 不能作为权限控制，权限必须在后端代码中执行。 |
| NFR-03 | 工具输入、输出和错误必须脱敏后进入 Trace。 |
| NFR-04 | 模型调用和 token/cost 应写入既有治理日志。 |
| NFR-05 | SSE 生产路径不得把问题、KB id 或 token 放入 URL query。 |
| NFR-06 | 新增外部依赖前必须创建 `docs/security/` 依赖评审。 |
| NFR-07 | `gpt-5.5` 作为高质量主模型时，必须保留成本治理、超时、降级模型或降级模式策略。 |
| NFR-08 | 任何 reasoning summary 都必须经过安全摘要边界，不得把原始推理 token、系统提示词、工具原始输出或私有上下文原文暴露给前端。 |

## 6. Open Questions

- V1 问答是否以课程 RAG 为默认，还是支持无资料通用问答作为单独模式。
- 联网搜索是否允许面向学生直接使用，还是只允许教师/admin 或后台审查后使用。
- 代码分析工具的分析对象是上传文件、仓库代码，还是课程作业代码；不同对象权限完全不同。
