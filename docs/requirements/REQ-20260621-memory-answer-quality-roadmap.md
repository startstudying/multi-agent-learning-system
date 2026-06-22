# REQ-20260621 记忆系统与回答质量需求

## 1. 功能需求

### FR-1 记忆分类与范围

系统应支持最小可行记忆类型：`short_term`、`preference`、`long_term`。后续可扩展 `graph`、`task_buffer`、`procedural`、`constraint`。

系统应支持最小范围：`user`、`project`、`task`。任何跨用户、跨课程、跨班级、跨租户检索必须经过后端权限校验。

### FR-2 MemoryContextService

系统应提供 `MemoryContextService`，面向 AI QA / tutor / resource generation 构造结构化上下文，至少包含：

- 当前请求与任务类型。
- 学习者低敏画像摘要。
- 课程、知识点、学习路径、掌握度、错题摘要。
- RAG 检索片段与 citation。
- 近期会话摘要，而不是无限原始历史。
- 注入原因、分数、来源和 token budget。

### FR-3 Memory Router 与 Retrieval

系统应根据任务类型生成 read plan，决定读取短期记忆、偏好记忆、长期项目记忆、RAG 资料和任务缓冲。

检索应采用 hybrid retrieval + rerank + dedupe + compress，而不是直接把全部历史塞入 prompt。

### FR-4 Memory Write Pipeline

写入链路应包含候选提取、分类、去重、冲突检测、重要性评分、置信度评分、敏感性检测、策略检查和提交。

敏感信息默认不写入长期记忆。任何长期记忆写入都应记录来源、证据、置信度、重要性、scope 和可删除标识。

### FR-5 QaRuntime

`/api/ai/qa` 应升级为统一 QA 运行时，至少经过：

`IntentRouter -> ContextBuilder -> MemoryRetriever -> RAG/ToolExecutor -> DraftGenerator -> Verifier -> FinalComposer -> Feedback/EvalLog`

`FAST / THINKING / EXPERT` 不应只映射字符串，而应影响可观测策略：检索深度、reasoning effort、工具调用权限、reviewer/verifier 强度、成本预算。

### FR-6 结构化答案

QA 答案应支持结构化 schema：

- `answer`
- `reasoningSummary`
- `citations`
- `learnerFit`
- `nextSteps`
- `uncertainty`
- `qualityFlags`
- `requiresReview`
- `traceId`

不得暴露 raw chain-of-thought、provider key、完整 prompt、未脱敏 profileSnapshot。

### FR-7 Verifier / Critic

系统应提供基础 verifier，检查：

- 是否回答用户问题。
- 是否满足 citation / no-source 策略。
- 是否存在隐私泄露。
- 是否存在明显幻觉或无依据结论。
- 是否遵守输出 schema。
- 是否需要人工审核。

### FR-8 Eval 闭环

系统应建立 QA 质量数据集与 gate，覆盖：

- source-required
- no-source
- prompt injection
- privacy leak
- cross-user memory leak
- personalized tutoring
- tool-use correctness
- multi-turn memory drift

低样本或缺少关键指标的 evaluation run 不得作为 prompt/model/tool 策略上线依据。

## 2. 非功能需求

### NFR-1 安全与隐私

- 前端不得直接调用 LLM API。
- 权限检查必须在后端服务层执行。
- RAG、profile、trace、memory 的原始敏感文本必须有 TTL、脱敏、hash 或不落库策略。
- 教师笔记默认不进入模型上下文，除非显式标记可用于 AI。

### NFR-2 可观测性

每次 AI QA 应产生可关联 `traceId`，记录上下文构造摘要、检索结果摘要、工具调用摘要、模型调用元数据、verifier 结果、eval 标签。

### NFR-3 可演进性

Prompt、tool schema、output schema、eval dataset 应版本化，并与模型调用和 trace 关联。

### NFR-4 性能与成本

上下文构造必须有 token budget；复杂模式可增加检索和 verifier，但必须有成本预算和最大循环次数。

## 3. 约束

- 本次任务只产出路线图与设计文档，不实现业务代码。
- 后续实现每个 slice 必须重新走 size-specific workflow。
- 新增依赖必须走 `docs/security/` 依赖评审。
