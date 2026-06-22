# PRD - AI 问答能力六阶段演进路线

## 1. 背景

用户提出从普通问答逐步演进到多 Agent 问答系统的六个版本：

1. Vue + Spring Boot 普通问答。
2. 加入 `Thinking` / `Fast` / `Expert` 三档模式。
3. 加入 `reasoning summary` 思考摘要。
4. 加入 SSE 流式输出。
5. 加入工具调用：联网搜索、文件检索、代码分析。
6. 加入多 Agent：规划 Agent、检索 Agent、审查 Agent、生成 Agent。

当前项目已经具备课程 RAG、SSE/POST streaming、Agent Trace、模型网关、RAG 引用、工具调用记录表和部分 Orchestrator 工作流基础。本 PRD 将用户版本目标整理为产品路线，而不是一次性直接改代码。

## 2. 产品目标

- 为学生提供从基础问答到可解释、多工具、多 Agent 协作问答的连续体验。
- 保留教育场景的引用、权限、Trace、审核和成本治理边界。
- 每个版本都可以独立演示、验证、回滚，不要求一次性交付所有能力。
- 前端只调用后端 API，不直接连接模型、联网搜索、文件系统或代码分析工具。
- 目标效果不是复刻会员权益，而是复刻 GPT Web 端“效果好”的工程机制：`gpt-5.5` 主模型、受控 `reasoning.effort`、干净上下文、工具检索、多轮自检、结构化输出和评测迭代。

## 3. 非目标

- 本轮不直接实现六个版本的代码。
- 本轮不引入新模型供应商、新依赖、WebSocket、浏览器端 LLM API key 或未审查的外部搜索 SDK。
- 本轮不绕过既有 RAG 权限过滤、Agent Trace、Review Gate、token/cost 记录。
- 本轮不把多 Agent 设计成无边界循环或自由工具执行。
- 本轮不承诺复刻 ChatGPT / GPT Web 端账号、会员、私有产品策略或不可公开能力；只在本系统后端可治理范围内复刻可实现的问答机制。

## 4. 用户价值

| 用户 | 价值 |
|---|---|
| 学生 | 在同一个问答入口中选择响应速度、答案深度和可解释程度。 |
| 教师 | 能看到回答来源、思考摘要、工具调用和审查结论，便于教学监督。 |
| 管理员 | 能治理 Agent 运行、工具调用、模型消耗、失败和安全风险。 |

## 5. 版本路线

| 版本 | 目标体验 | 交付边界 |
|---|---|---|
| V1 普通问答 | 输入问题，返回文本答案和基础 traceId | 复用后端模型网关或 RAG query，不暴露模型密钥 |
| V2 三档模式 | 用户选择 `FAST` / `THINKING` / `EXPERT` | 后端统一解释模式为 prompt/runtime policy，不由前端控制模型秘密参数 |
| V3 思考摘要 | 返回面向用户的短思考摘要 | 只返回安全的 `reasoningSummary`，不返回原始 chain-of-thought |
| V4 SSE 流式输出 | 答案逐步出现，状态清晰 | 复用 `POST /api/rag/query/stream` 或新增统一 QA stream endpoint |
| V5 工具调用 | 支持联网搜索、文件检索、代码分析 | 工具必须走后端 Service/Tool 边界，记录 `agent_tool_call` |
| V6 多 Agent | 规划、检索、审查、生成协作 | Orchestrator 有状态编排，限制轮数，记录完整 Trace |

## 6. 成功指标

- 每个 AI 问答请求都有 `traceId`。
- RAG/文件检索类回答有 citations，无来源时明确拒答或降级。
- 工具调用有脱敏输入/输出摘要、耗时、状态和错误码。
- 多 Agent 工作流有最大轮数和失败降级策略。
- 前端支持 loading、streaming、error、empty、done 状态。
- 后端测试覆盖模式选择、思考摘要安全、SSE 事件、工具权限、多 Agent 状态迁移。
- `FAST` / `THINKING` / `EXPERT` 的实际效果可通过模型日志、token/cost、reasoning effort 映射、引用质量、审查通过率和用户可见摘要质量进行对比评估。

## 7. 里程碑建议

优先从 V1-V3 形成一个 M 级可交付切片：统一问答 API、模式枚举和思考摘要字段。V4 可复用既有正式 production streaming 能力作为第二个 M 级切片。V5/V6 涉及工具、外部联网、代码分析和多 Agent 编排，应拆成 L 级或多个 M 级子任务。
