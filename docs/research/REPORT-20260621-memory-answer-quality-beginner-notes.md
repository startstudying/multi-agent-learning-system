# REPORT-20260621 记忆系统与回答质量调研报告（左栏小白注释版）

> 阅读方式：左侧是“小白注释”，用大白话解释；右侧是正式调研结论。这样可以一边看懂概念，一边保留工程判断。

| 小白注释 | 调研报告正文 |
|---|---|
| **先看结论：不是换个更强模型就完事。** | 当前项目要接近 ChatGPT 级回答质量，核心不是单点模型能力，而是建立完整 Answer Quality System：上下文构建、记忆检索、RAG/工具执行、答案生成、质量校验、评测反馈。 |
| **现在项目有不少零件，但还没组装成一台车。** | 项目已经具备 RAG、Agent Trace、PromptVersion、Evaluation、Model Gateway、Provider Registry、学习者画像快照等基础能力。主要缺口是缺少统一 `QaRuntime`、`MemoryContextService`、`AnswerVerifier` 和 Eval feedback loop。 |
| **`/api/ai/qa` 现在更像“问知识库”，还不是“会思考的答疑运行时”。** | 当前 AI QA 主要包装 `RagQueryService`，能够保留 RAG 权限、citation 和 traceId，但尚未系统组合长期记忆、近期会话摘要、真实工具调用、reviewer/verifier、模型策略和评测闭环。 |
| **记忆不是“把所有历史都塞进去”。** | 记忆系统应由 Memory Store、Memory Retrieval、Context Orchestration、Memory Write Pipeline、Policy/Governance 组成。关键是读对、写对、裁剪对、治理对，而不是保存越多越好。 |
| **第一步不要急着“记住更多”，先确认哪些东西不能乱记。** | 安全/质量分析显示，RAG query log、citation excerpt、profileSnapshot、trace retention 都可能把敏感学习数据持久化。长期记忆会放大这些风险，所以 P0 第一项应是 Memory/RAG Privacy Guard。 |
| **学生问的问题、老师写的备注、资料原文片段，都可能是敏感信息。** | 原始 question、answer、excerpt、documentName、`teacher_note`、完整 profileSnapshot 不应默认进入长期记忆或长期 trace。建议保存 hash、长度、低敏摘要、citation id、profileRef 等可治理信息。 |
| **MemoryContextService 就像“答题前的资料整理员”。** | `MemoryContextService` 应从学习者画像、学习事件、掌握度、错题、近期会话摘要、RAG citation、用户偏好中挑选必要上下文，并记录每条上下文的来源、分数和注入原因。 |
| **Context Orchestrator 像“排版和优先级调度器”。** | `ContextOrchestrator` 负责把系统策略、用户当前请求、任务约束、RAG 证据、学习状态、偏好记忆按优先级和 token budget 组织给模型。P0/P1/P2 约束不能被历史记忆覆盖。 |
| **`FAST / THINKING / EXPERT` 不该只是三个按钮名字。** | 当前 `answerMode` 只映射到 `low/medium/high` 字符串。后续应让模式真实影响检索深度、上下文预算、模型 reasoning effort、工具调用权限、verifier 强度和成本策略。 |
| **Verifier 就像交卷前检查一遍：有没有瞎编、漏引用、泄露隐私。** | `AnswerVerifier` MVP 应检查 schema 合规、citation 必填、no-source 策略、隐私泄露、prompt injection、答案完整性和下一步建议。它可以先规则化，再逐步加入轻量 LLM reviewer。 |
| **Eval 是“考试题库”，没有它就不知道回答变好了还是变差了。** | 质量提升应由 eval dataset、grader、trace、badcase 驱动。数据集至少覆盖 source-required、no-source、prompt injection、privacy leak、cross-user memory leak、personalized tutoring、multi-turn memory drift。 |
| **工具调用不能让模型自己乱跑数据库。** | 所有工具调用必须走后端 Service 层，不能直接访问 Mapper/Repository。工具输入输出要有严格 schema、权限校验、traceId、citation/warnings、recoverable error。 |
| **OpenAI 的对话状态可以借鉴，但不能替代你自己的业务数据库。** | 外部 response id / conversation state 可作为模型连续调用引用，但项目自己的学习记忆、权限、RAG citation、trace、review 状态必须由项目 DB 主导。 |
| **不要一上来就上 GraphMemory、Neo4j、自进化。** | MVP 应先做 preference / long_term / short_term 三类记忆和 user/project/task 三类 scope。GraphMemory、procedural memory、多 Agent 共享记忆属于 P2 或更后。 |
| **最终体验应该是：回答更懂学生，但也更守规矩。** | 理想 QA 输出应包含 `answer`、`reasoningSummary`、`citations`、`learnerFit`、`nextSteps`、`uncertainty`、`qualityFlags`、`traceId`，并且不暴露 raw chain-of-thought、完整 prompt 或敏感画像。 |

## 关键判断

| 小白注释 | 调研报告正文 |
|---|---|
| **最先做隐私，是为了以后敢做个性化。** | P0-1 必须是 Memory/RAG Privacy Guard。没有它，MemoryContextService 和长期记忆越强，敏感数据扩散越严重。 |
| **第二步做上下文，是为了让模型“拿对资料”。** | P0-2 应实现 `MemoryContextService MVP`，把低敏画像、学习状态、RAG citation、近期摘要统一成可解释上下文。 |
| **第三步做运行时，是为了让 QA 不再只是 RAG 包装。** | P0-3 应实现 `QaRuntime / Answer Quality MVP`，把 intent、context、RAG/tool、model gateway、verifier、final composer 串起来。 |
| **第四步做评测，是为了防止越改越玄学。** | P0-4 应实现 Basic Verifier / Eval Gate，确保 prompt/model/tool 变更有回归数据支撑。 |

## 推荐路线

| 优先级 | 小白注释 | 工程任务 |
|---|---|---|
| P0-1 | 先别乱记，先装隐私阀门。 | Memory/RAG Privacy Guard |
| P0-2 | 给模型准备一份干净、相关、有限的资料包。 | MemoryContextService MVP |
| P0-3 | 把问答流程升级成真正运行时。 | QaRuntime structured answer MVP |
| P0-4 | 给回答质量装检查器和考试题库。 | AnswerVerifier and QA Eval Gate |
| P1 | 让用户看到流式回答和质量证据。 | QA streaming and trace workbench |
| P2 | 让记忆能衰减、合并、删除、纠错。 | Memory lifecycle governance |

## 不建议现在做

| 小白注释 | 调研报告正文 |
|---|---|
| **不要先加一堆新数据库。** | PostgreSQL + pgvector、Neo4j 等需要 ADR 和迁移评审。当前可优先复用 MySQL 与现有 VectorIndexAdapter/Qdrant 边界。 |
| **不要把所有规则写进 prompt。** | Prompt 只负责策略表达，权限、审计、引用、隐私、工具边界必须由后端代码和数据治理保证。 |
| **不要让模型自己决定保存什么记忆。** | 长期记忆写入必须经过候选提取、分类、去重、冲突检测、重要性评分、敏感性评分和 policy check。 |
| **不要用少量样本宣布质量提升。** | 低样本 evaluation run 容易误判。关键指标缺失时 gate verdict 应为 `FAIL` 或 `INSUFFICIENT_SAMPLE`。 |

## 最终建议

| 小白注释 | 调研报告正文 |
|---|---|
| **下一步就做一件事：隐私守门。** | 建议下一张实施任务单聚焦 `Memory/RAG Privacy Guard`。它是后续 MemoryContextService、QaRuntime、Verifier/Eval 的安全前置条件。 |
| **这不是拖慢进度，是给后面提速。** | 隐私守门完成后，后续上下文和长期记忆可以更大胆地扩展，因为敏感字段、TTL、hash、摘要、删除策略已经有边界。 |
