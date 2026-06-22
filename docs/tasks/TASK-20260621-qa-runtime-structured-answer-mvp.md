# TASK-20260621 QaRuntime structured answer MVP

## 1. 目标

完成 P0-3：把 `/api/ai/qa` 从直接 RAG wrapper 升级为 `QaRuntime` MVP，输出结构化回答 schema。

## 2. 范围

包含：

- `QaRuntime`
- `IntentRouter`
- `ContextOrchestrator`
- `FinalComposer`
- `AiQaResponse` 新增结构化字段
- `AiQaService` 委托运行时
- focused / adjacent tests

不包含：

- 新模型 provider 调用
- DB schema
- 依赖
- 前端消费
- verifier/eval gate 完整实现
- SSE streaming

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/runtime/*.java`
- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/test/java/com/learningos/aiqa/application/runtime/*.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- 本任务 P0-3 docs / evidence / acceptance
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

## 4. 禁止修改文件

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/**`
- AI provider 配置和密钥
- RAG parser/vector/index worker
- ResourceGeneration / ReviewGate / Evaluation 生产代码

## 5. 验收标准

- `QaRuntimeTest` 覆盖 grounded 和 no-source 场景。
- `AiQaControllerTest` 覆盖新增响应字段。
- `sources` 保持兼容，`citations` 与 `sources` 一致。
- `toolCalls` 包含运行时关键步骤。
- 响应不包含 raw chain-of-thought / prompt / provider key / teacher note。
- Evidence / Acceptance 写入完成。

## 6. Evidence

已写入 `docs/evidence/EVIDENCE-20260621-qa-runtime-structured-answer-mvp.md`。

关键验证：

- RED：接续上下文记录中，`mvn --% -Dtest=QaRuntimeTest test` 首次失败于缺少 `QaRuntime` / `IntentRouter` / `ContextOrchestrator` / `FinalComposer` 和新响应字段。
- GREEN：`mvn --% -Dtest=QaRuntimeTest test`，2 run, 0 failures, 0 errors。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,MemoryContextServiceTest,RagQueryServiceTest test`，32 run, 0 failures, 0 errors。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。

调试记录：

- Adjacent 首次运行时，`RagQueryServiceTest.rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates` 出现一次 `ERROR_FALLBACK` / `TIMEOUT_FALLBACK` 抖动。
- 单独复跑该方法通过，随后整组 adjacent 复跑通过；未修改 P0-3 范围外的 RAG 生产/测试文件。

## 7. Acceptance Verdict

Verdict: PASS

已写入 `docs/acceptance/ACCEPT-20260621-qa-runtime-structured-answer-mvp.md`。

验收结论：

- `/api/ai/qa` 已委托 `QaRuntime`，响应包含结构化字段。
- grounded / no-source 两条路径均通过 focused 测试。
- `sources` 与 `citations` 兼容。
- `toolCalls` 包含 `IntentRouter`、`RagQueryService`、`MemoryContextService`、`ContextOrchestrator`、`FinalComposer`。
- 未新增 DB schema、依赖、前端消费、真实模型 provider 或 SSE streaming。
