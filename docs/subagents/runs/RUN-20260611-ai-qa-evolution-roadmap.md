# RUN - AI 问答能力六阶段演进路线专家分析

## 1. Run Scope

本报告为 L 级 Multi-Expert Subagent Gate 的文档化分析。由于用户未明确要求并行代理实现，本轮不启动实际代码子代理，仅整合专家视角形成路线结论。

## 2. Product Analyst

六个版本不应一次性交付。V1-V3 可以形成一个最小闭环：用户可提问、选择深度、看到安全思考摘要。V4 是体验升级，项目已有正式 production streaming 可复用。V5/V6 是能力边界升级，涉及工具和多 Agent 安全，必须单独拆分。

## 3. Frontend Expert

前端应把“模式选择、流式回答、思考摘要、引用、工具调用摘要、Trace 入口”组合成一个问答工作台，而不是新增多个分散入口。生产流式必须沿用 `fetch` / `ReadableStream` 的 POST body 方式，不回退到携带敏感 query 的 GET SSE。

## 4. Backend Expert

后端应新增或收敛统一 QA application service。`answerMode` 是后端策略输入，映射到 prompt version、retrieval topK、工具上限、审查强度等策略。Controller 只做 HTTP adapter，模型和工具仍经 `AiModelGateway`、RAG Service、Tool Service、Orchestrator。

## 5. Agent/RAG Expert

V5 文件检索应优先复用 RAG 管线和 citations。联网搜索和代码分析不能直接进入模型 prompt，必须先通过受控 Tool Gateway，输出短摘要和可信来源。V6 多 Agent 应明确 Planner / Retrieval / Reviewer / Generator，不允许自由递归和无上限工具循环。

## 6. Security & Quality

主要风险：

- 原始 chain-of-thought 泄漏。
- 前端或工具绕过后端权限。
- 联网搜索把私有问题或课程资料泄漏给第三方。
- 代码分析读取未授权路径或敏感文件。
- 多 Agent 循环导致成本失控。

硬性建议：

- 只返回安全 `reasoningSummary`。
- 工具默认关闭，显式配置开启。
- 每个工具有 allowlist、timeout、max result、redaction。
- 每个 Agent workflow 有 max rounds 和 max tool calls。

## 7. Integration Reviewer

推荐路线：

1. 先做 Slice 1：V1-V3 统一问答合同。
2. 再做 Slice 2：V4 统一流式问答。
3. 再做 Slice 3：文件检索工具。
4. 联网搜索和代码分析分别走独立安全/依赖评审。
5. 最后做 V6 多 Agent 编排。

结论：本轮只完成 L 级路线文档是正确边界，后续实现必须拆 M/L 子任务。
