# PRD-20260621 记忆系统与 ChatGPT 级回答质量路线图

## 1. 背景

当前项目已经具备 RAG、Agent Trace、Prompt Version、Evaluation、Model Gateway、学习者画像快照、资源审核等基础能力，但这些能力还没有被组合成统一的 AI QA 运行时。`/api/ai/qa` 目前主要包装 `RagQueryService`，还没有形成“意图识别、上下文编排、记忆检索、工具执行、答案生成、质量校验、评测闭环”的完整 Answer Quality System。

用户给出的两个框架明确指出：记忆系统不是单一向量库，而是 Memory Store、Memory Retrieval、Context Orchestration、Memory Write Pipeline、Policy/Governance 的组合；复刻 ChatGPT 的回答质量也不能只依赖“LLM + RAG + 工具调用”，必须建立上下文、记忆、工具、验证和评测闭环。

## 2. 产品目标

1. 让 AI 答疑能够使用项目内可信上下文：学习者画像、课程/RAG 资料、近期会话摘要、学习路径、错题/掌握度、项目级策略。
2. 让记忆系统可解释、可治理、可删除，避免把原始问题、教师笔记、课程片段和敏感画像无差别沉淀为长期记忆。
3. 让 AI 答案具备结构化质量标准：正确性、完整性、引用可信度、个性化适配、简洁性、工具使用质量、指令遵循。
4. 让质量提升由 eval dataset、grader、trace、badcase 驱动，而不是反复堆 prompt。

## 3. 目标用户

- 学生：获得更贴合自身水平、能引用课程来源、能承认不确定性的学习答疑。
- 教师：查看 AI 答案依据、质量评估和资源审核状态，避免错误资源直接发布。
- 管理员/运营：观察模型调用、成本、质量指标、隐私治理和回归风险。
- 开发团队：在可追踪、可评测的框架下迭代 prompt、模型、工具和记忆策略。

## 4. MVP 范围

P0 MVP 不直接追求 GraphMemory 或复杂多 Agent 自进化，优先把现有基础能力串成受控链路：

1. Memory/RAG Privacy Guard：先阻断原始问题、答案、excerpt、教师笔记、完整 profileSnapshot 进入长期记忆或长期 trace。
2. MemoryContextService MVP：从学习者画像、学习事件、掌握度/错题、近期 QA/session 摘要、RAG 检索结果构造有 token budget 的 `MemoryContext`。
3. QaRuntime / Answer Quality MVP：让 `/api/ai/qa` 进入统一运行时，经过 Context Builder、RAG、`AiModelGateway`、结构化 QA schema。
4. Basic Verifier/Eval Gate：校验 citation、no-source 策略、隐私泄露、指令遵循、完整性，并记录 eval/trace。

## 5. 非目标

- 不在本路线图阶段直接引入 Neo4j、复杂组织级共享记忆或无限 Agent 自我进化。
- 不让前端直接调用 LLM/API key。
- 不把 OpenAI/外部 conversation state 当作项目业务记忆的唯一来源。
- 不在未完成依赖评审前引入 Python/TypeScript Agents SDK 作为 Java 后端运行时依赖。

## 6. 成功指标

| 指标 | 目标 |
|---|---|
| Citation Accuracy | source-required 样本 >= 95% |
| No-source Refusal / Safe Fallback | no-source 样本 >= 95% |
| Prompt/context leak rate | 0 |
| Cross-user memory leak rate | 0 |
| Groundedness | >= 0.90 |
| Personalization helpfulness | >= 4.2/5 |
| Trace coverage | 100% AI QA workflow 有 traceId |
| Raw sensitive persistence | 新增长期记忆默认不保存原始问题、完整 excerpt、teacher_note |

## 7. 路线图优先级

- P0：隐私守门、记忆上下文、统一 QA Runtime、基础 Verifier/Eval。
- P1：QA streaming、prompt/schema 版本、真实工具调用 trace、质量工作台。
- P2：记忆生命周期治理、会话表扩展、GraphMemory/Procedural Memory 探索。
