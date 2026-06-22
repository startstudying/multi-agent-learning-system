# PLAN-20260621 记忆系统与回答质量落地计划

## 1. 一句话目标

把当前“RAG + AI QA + Trace + Evaluation + Model Gateway”的分散能力，收束成一条可治理、可评测、可解释的高质量回答链路。

目标链路：

```text
用户问题
-> 意图识别
-> 记忆与上下文构建
-> RAG / 工具检索
-> 结构化答案生成
-> 回答校验
-> 最终输出
-> Trace / Eval / Feedback 记录
```

## 2. 总体原则

1. 不先堆大 prompt，先把上下文来源、权限、脱敏、评测闭环做稳。
2. 不先扩大长期记忆，先防止原始问题、完整答案、excerpt、教师笔记、画像快照被长期保存。
3. 不让前端接触 LLM API，所有模型、工具、权限、审计都在后端。
4. 不把“回答看起来不错”当作质量，要用 eval dataset、trace、badcase 和验收阈值验证。

## 3. 阶段计划

| 阶段 | 目标 | 主要产物 | 验收重点 |
|---|---|---|---|
| P0-1 | Memory/RAG Privacy Guard | 隐私策略、RAG 日志脱敏、profileSnapshot 最小化 | 新增长期数据不保存 raw question / full excerpt / teacher_note |
| P0-2 | MemoryContextService MVP | 统一 `MemoryContext`、token budget、注入理由 | QA 能拿到低敏画像、学习状态、RAG citation、近期摘要 |
| P0-3 | QaRuntime MVP | `IntentRouter`、`ContextOrchestrator`、结构化答案 schema | `/api/ai/qa` 不再只是 RAG wrapper |
| P0-4 | Basic Verifier / Eval Gate | citation/no-source/privacy/schema 校验、QA eval set | 回答质量可回归，不靠主观感觉 |
| P1 | 流式与工作台 | `/api/ai/qa/stream`、prompt/schema 版本、质量面板 | 教师/管理员能看 trace 与质量结论 |
| P2 | 记忆生命周期 | salience、decay、用户可编辑/删除、session 表扩展 | 记忆可治理、可纠错、可清理 |

## 4. P0-1 详细计划：先做隐私守门

### 为什么先做

如果先做长期记忆，系统会把当前已经存在的敏感字段放大：学生原始提问、课程片段、教师笔记、学习弱点、完整 profileSnapshot 都可能被长期沉淀。这个顺序不对。

### 建议改动范围

- 新增 RAG / memory 隐私策略服务，例如 `RagPrivacySanitizer` 或 `MemoryPrivacyPolicy`。
- RAG query log 默认保存 hash、长度、低敏元数据，不保存完整原始问题。
- citation excerpt 默认只保存短摘要或 citation id。
- `profileSnapshot` 改为 `profileRef + 必要低敏字段`。
- `teacher_note` 默认不进入模型上下文和长期记忆。

### 验收标准

- 测试断言新写入数据不包含原始问题全文。
- 测试断言新写入数据不包含完整 excerpt。
- 测试断言 `teacher_note` 不进入 AI 上下文。
- RAG UI 或 replay 如受影响，需要明确降级展示策略。

## 5. P0-2 详细计划：MemoryContextService

### 目标

提供一个后端统一服务，把“这次回答真正需要的上下文”准备好。

建议结构：

```java
record MemoryContext(
    LearnerSummary learner,
    List<LearningSignal> learningSignals,
    List<RagCitationContext> citations,
    List<RecentSessionSummary> recentSessions,
    List<PreferenceMemory> preferences,
    List<ContextInjectionReason> injectionReasons,
    TokenBudget budget
) {}
```

### 输入来源

- 学习者画像低敏摘要。
- 学习事件、掌握度、错题摘要。
- RAG 检索结果和 citation。
- 最近会话摘要。
- 用户偏好和项目规则。

### 验收标准

- 每条上下文都有来源、分数和注入原因。
- token budget 有上限，不允许无限塞历史。
- 上下文对象不包含 raw prompt、provider key、teacher_note。

## 6. P0-3 详细计划：QaRuntime

### 目标

把 `/api/ai/qa` 从“RAG 包装器”升级为“回答质量运行时”。

推荐链路：

```text
AiQaController
-> QaRuntime
   -> IntentRouter
   -> MemoryContextService
   -> RAG / ToolExecutor
   -> ContextOrchestrator
   -> AiModelGateway
   -> AnswerVerifier
   -> FinalComposer
```

### `FAST / THINKING / EXPERT` 的真实含义

| 模式 | 建议策略 |
|---|---|
| FAST | 少量上下文、低检索深度、基础 verifier、低成本模型策略 |
| THINKING | 标准上下文、标准 RAG、结构化答案、基础质量校验 |
| EXPERT | 更深检索、更严格 verifier、必要时 reviewer、更多 trace 证据 |

### 验收标准

- QA 响应输出结构化字段：`answer`、`reasoningSummary`、`citations`、`learnerFit`、`nextSteps`、`uncertainty`、`traceId`。
- 不暴露 raw chain-of-thought。
- 不让前端解析自然语言来猜 citation 或质量状态。

## 7. P0-4 详细计划：Verifier / Eval Gate

### 目标

让回答质量有可回归的标准。

基础 verifier 检查：

- 是否必须引用但没有引用。
- no-source 场景是否胡编。
- 是否泄露隐藏上下文、教师笔记、画像原文。
- 是否符合输出 schema。
- 是否回答了用户真正的问题。

Eval 数据集标签：

- `SOURCE_REQUIRED`
- `NO_SOURCE`
- `PROMPT_INJECTION`
- `PRIVACY_LEAK`
- `CROSS_USER_MEMORY_LEAK`
- `PERSONALIZED_TUTORING`
- `MULTI_TURN_MEMORY_DRIFT`

### 验收标准

- 低样本 evaluation run 不能作为上线依据。
- 缺少关键指标时 gate verdict 为 `FAIL` 或 `INSUFFICIENT_SAMPLE`。
- 每次 prompt/model/tool 策略变更都有 baseline vs candidate。

## 8. 推荐任务拆分

1. `Memory/RAG Privacy Guard`
2. `MemoryContextService MVP`
3. `QaRuntime structured answer MVP`
4. `AnswerVerifier and QA Eval Gate`
5. `QA streaming and trace workbench`
6. `Memory lifecycle governance`

## 9. 主要风险

| 风险 | 处理方式 |
|---|---|
| 长期记忆污染 | 先做写入策略、去重、冲突检测、置信度和重要性评分 |
| 隐私泄露 | 默认不保存 raw sensitive text，敏感字段短 TTL 或 hash |
| 回答质量不可测 | 建 eval dataset 和 gate，而不是只调 prompt |
| 模型状态不可复现 | 项目 DB 保存业务状态，外部 response id 只做引用 |
| 工具越权 | 工具只能走 Service 层，后端权限校验先于工具执行 |

## 10. 下一步建议

下一步直接开一个 M-size 实施任务：`Memory/RAG Privacy Guard`。这个切片完成后，再做 `MemoryContextService` 会稳很多。

## 11. 落地进度

### 2026-06-21 P0-1 Memory/RAG Privacy Guard

状态：已完成。

交付：

- 新增 `MemoryPrivacyPolicy`，统一 question hash/length、citation excerpt hash/length、profileRef、RAG replay redaction 策略。
- RAG query log 新写入不保存 raw question；source citation 新写入不保存 full excerpt。
- RAG requestId replay snapshot 改为 `RAG_REPLAY_REDACTED` 降级响应，保留 trace/retrieval/source identity。
- `profileSnapshot` 改为 `profileRef + 必要低敏字段`，不再保存 raw `learnerId`、`teacher_note` 或 `TEACHER_NOTE` source marker。
- ResourceAgent 模型上下文复用低敏 `profileSnapshot`，测试断言 `teacher_note` 不进入 AI context。

验证：

- `mvn --% -Dtest=RagQueryServiceTest test`：22 run, 0 failures, 0 errors。
- `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test`：54 run, 0 failures, 0 errors。
- `mvn --% -Dtest=OrchestratorWorkflowControllerTest test`：33 run, 0 failures, 0 errors。

下一步建议：继续 P0-2 `MemoryContextService MVP`，基于本切片的低敏画像和 citation metadata 构建可注入上下文。

### 2026-06-21 P0-2 MemoryContextService MVP

状态：已完成。

交付：

- 新增 `MemoryContextService` 和 `MemoryContext`，统一构造 learner summary、learning signals、RAG citation context、recent session summary、preferences、injection reasons 和 token budget。
- 画像上下文复用 P0-1 `MemoryPrivacyPolicy.profileRef`，不暴露 raw `learnerId`。
- 学习状态从 `mastery_record`、`wrong_question` 和 `learning_event.summary` 构造短摘要；真实 chat session 生命周期留给后续 P1/P2。
- RAG citation 进入上下文前只保留短摘要与引用元数据，不保留完整 excerpt。
- `MemoryContext` 对敏感标记执行过滤和截断，防止 `teacher_note`、provider key、raw prompt 等进入上下文对象。
- `/api/ai/qa` 通过现有 `toolCalls` 返回 `MemoryContextService` 准备摘要，未修改请求 DTO、DB schema、依赖或前端。

验证：

- RED: `mvn --% -Dtest=MemoryContextServiceTest test` 首次失败于缺少 P0-2 服务与仓库查询方法。
- GREEN: `mvn --% -Dtest=MemoryContextServiceTest test`，1 run, 0 failures, 0 errors。
- Adjacent: `mvn --% -Dtest=AiQaControllerTest,RagQueryServiceTest test`，26 run, 0 failures, 0 errors。

下一步建议：继续 P0-3 `QaRuntime structured answer MVP`，把 `/api/ai/qa` 从 RAG wrapper 升级为统一回答运行时。

### 2026-06-21 P0-3 QaRuntime structured answer MVP

状态：已完成。

交付：

- 新增 `QaRuntime`，将 `/api/ai/qa` 从直接 RAG wrapper 升级为显式回答运行时。
- 新增 `IntentRouter`、`ContextOrchestrator`、`FinalComposer`，形成 `IntentRouter -> RagQueryService -> MemoryContextService -> ContextOrchestrator -> FinalComposer` 的最小链路。
- `AiQaService` 改为薄门面，委托 `QaRuntime.run(...)`。
- `AiQaResponse` 保留 `sources`，新增 `citations`、`learnerFit`、`nextSteps`、`uncertainty`、`qualityFlags`、`requiresReview`。
- grounded 场景返回 `COURSE_GROUNDED / COURSE_RAG`、低不确定性和无需 review；no-source 场景返回 `GENERAL_FALLBACK / NO_COURSE_SOURCE_FALLBACK`、空引用、`MEDIUM` 不确定性和 `requiresReview=true`。
- `toolCalls` 返回低敏运行时步骤摘要，不暴露 raw chain-of-thought、prompt、provider key、teacher note 或未脱敏画像。
- 本切片未新增 DB schema、依赖、前端消费、真实模型 provider、SSE streaming 或 P0-4 verifier/eval gate。

验证：

- RED：接续上下文记录中，`mvn --% -Dtest=QaRuntimeTest test` 首次失败于缺少 runtime 组件和响应字段。
- GREEN：`mvn --% -Dtest=QaRuntimeTest test`，2 run, 0 failures, 0 errors。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,MemoryContextServiceTest,RagQueryServiceTest test`，32 run, 0 failures, 0 errors。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。

说明：

- Adjacent 首次运行时，`RagQueryServiceTest.rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates` 出现一次 10ms 异步 timeout 抖动；单测复核和整组复跑均通过，未越界修改 RAG 文件。

下一步建议：继续 P0-4 `Basic Verifier / Eval Gate`，把 citation/no-source/privacy/schema 校验固化为可回归 gate，并建立最小 QA eval set。

### 2026-06-21 P0-4 Basic Verifier / Eval Gate

状态：已完成。

交付：

- 新增 `AnswerVerifier`，对 `/api/ai/qa` 结构化回答执行 schema、citation、no-source、privacy 基础校验。
- 新增 `QaEvalGate`，支持 `PASS` / `FAIL` / `INSUFFICIENT_SAMPLE` 最小 gate verdict。
- `AiQaResponse` 新增 `verification`，返回 verdict、gate policy、check 明细、summary 与 review requirement。
- `QaRuntime` 在 `FinalComposer` 后接入 `AnswerVerifier`，并追加低敏 `AnswerVerifier` tool call。
- `EvaluationSetService` 支持 `AI_QA_ANSWER` 样本类型，要求 question 与非空 quality criteria。
- `EvaluationRunService` 支持 QA gate 指标白名单：`schemaPassRate`、`verificationPassRate`、`privacyLeakRate`。
- 本切片未新增 DB schema、依赖、前端消费、真实模型 reviewer、批量 eval runner、P1 streaming/workbench 或 P2 memory lifecycle。

验证：

- RED：接续上下文记录中，P0-4 测试首次失败于缺少 `AnswerVerifier`、`QaEvalGate`、`verification` DTO 与 runtime verifier tool call。
- GREEN：`mvn --% -Dtest=AnswerVerifierTest,QaEvalGateTest,QaRuntimeTest test`，10 run, 0 failures, 0 errors。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,RagEvaluationServiceTest,RagQueryServiceTest test`，50 run, 0 failures, 0 errors。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。
- Privacy grep：生产命中仅为敏感标记过滤/校验规则，测试命中为故意样本。

下一步建议（历史记录）：继续 P1 `QA streaming and trace workbench`，将 QA streaming、trace、verification 和 prompt/schema 版本做成可观察工作台；P2 `Memory lifecycle governance` 已由后续切片完成。

### 2026-06-21 P1 QA streaming and trace workbench MVP

状态：已完成。

交付：

- 新增 `POST /api/ai/qa/stream`，输出 `status`、`token`、`done`、`error` SSE 事件。
- QA stream 复用 `AiQaService`、`QaRuntime` 和 `AnswerVerifier`，不新增 DB schema 或依赖。
- 新增前端 `streamAiQa` API helper，生产/预发学生问答改用 `/api/ai/qa/stream`。
- 前端请求体携带 question/kbIds/topK/courseId/answerMode，不把 question/kbIds 放入 URL。
- 学生回答卡展示 `verification` verdict、gate policy、source policy、uncertainty、answer mode、reasoning effort、tool call count 和 quality flags。
- 右侧思考流增加「校验回答质量」步骤，并在运行指标中展示 QA verdict / source policy / tool chain count。
- 本切片未实现真实 provider token streaming、独立教师/管理员质量看板、批量 eval runner 或 P2 memory lifecycle。

验证：

- RED backend：`mvn --% -Dtest=AiQaControllerTest test` 首次失败于 `/api/ai/qa/stream` 缺失。
- RED frontend：`pnpm test -- --run App.spec.ts` 首次失败于仍调用 `/api/rag/query/stream` 且没有 `qa-quality-summary`。
- GREEN backend：`mvn --% -Dtest=AiQaControllerTest test`，5 run, 0 failures, 0 errors。
- GREEN frontend：`pnpm test -- --run App.spec.ts`，34 passed。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。
- Build：`pnpm build`，通过。

下一步建议：继续 P2 `Memory lifecycle governance`，补 salience、decay、用户可编辑/删除和 session 生命周期边界。

### 2026-06-21 P2 Memory lifecycle governance MVP

状态：已完成。

交付：

- 新增 `V23__memory_lifecycle_governance.sql`，扩展 `kb_chat_session` 与 `kb_chat_message` 的 learner/course/session/message lifecycle 字段。
- `KbChatSession` / `KbChatMessage` entity 支持 owner、course、status、salience、decay、editable、created/updated/deleted 时间。
- 新增 `KbChatMessageRepository`，扩展 `KbChatSessionRepository`，提供 owner 隔离与 active memory 查询。
- 新增 `MemoryLifecycleService`：QA 回答成功后写入低敏 `AI_QA_SUMMARY`，计算 salience/decay，支持 session 列表、用户编辑和软删除。
- 新增 `AiQaMemoryController`：`GET /api/ai/memory/sessions`、`PATCH /api/ai/memory/messages/{messageId}`、`DELETE /api/ai/memory/messages/{messageId}`。
- `AiQaService` 在 `QaRuntime` 成功返回后记录低敏 session memory。
- `MemoryContextService` 优先读取未删除、未过期 session memory；无 active memory 时保留 `learning_event` fallback。
- `MysqlMigrationSmokeTest` latest version/count 更新到 V23/23，并补充 V23 schema assertion。

隐私边界：

- memory summary 仅保存 `questionHash/questionLength`、`answerLength`、`sourcePolicy`、`verification` 和 `citationCount`。
- 不保存 raw question、完整 answer、prompt、provider key、teacher note 或 raw profileSnapshot。
- 用户编辑 summary 时遇到敏感标记会写入 `[redacted-sensitive-memory]`。

验证：

- RED：P2 focused 测试首次失败于缺少 `KbChatMessageRepository`、`MemoryLifecycleService`、chat lifecycle 字段与 V23 migration。
- GREEN focused：`mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test`，26 run, 0 failures, 0 errors。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,QaRuntimeTest test`，7 run, 0 failures, 0 errors。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。
- Privacy grep：生产命中仅为敏感标记过滤/校验规则。

计划结论：P0-1、P0-2、P0-3、P0-4、P1、P2 均已完成；这份落地计划的 MVP 范围已闭环。
