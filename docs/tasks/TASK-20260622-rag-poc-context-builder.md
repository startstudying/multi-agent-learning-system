# TASK - RAG POC Context Builder

## 1. 目标

吸收 `wiki-rag` 的 POC 检索后上下文优化思想，在现有 Java/Spring RAG 后端中增加结构化上下文构造能力，提升命中 chunk 周边语义完整度，同时保持 citation、权限、隐私和 API 合同稳定。

## 2. 任务类型

RAG / retrieval 优化。

## 3. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 用户提出“完善项目”的功能开发请求，必须走项目分级流程 |
| `educational-rag-pipeline` | 涉及 RAG retrieval、citation grounding、permission 和 no-source 规则 |
| `test-driven-development` | 新增行为必须先写失败测试再实现 |
| `verification-before-completion` | 完成声明前必须提供新鲜测试证据 |

Missing skills：`Confidence Check` 索引路径不存在，已记录为非阻塞。

GitHub Research Needed：No。本任务直接消费已有 `GITHUB-20260621-wiki-rag-reference.md` 调研报告，不再重复外部搜索。

New Project-Specific Skill：No。现有 `rag-hybrid-retrieval` / `rag-embedding-vector-adapter` 足够覆盖边界。

## 4. Size Classification

Size：M

Reason：

- 影响 RAG 后端回答上下文组织，属于可见行为优化。
- 不改 API、DTO、DB schema、依赖、frontend-backend contract。
- 修改范围集中在 RAG application 层与测试，但有 citation / permission / query log 集成风险。

Required Documents：

- `docs/specs/SPEC-20260622-rag-poc-context-builder.md`
- `docs/tasks/TASK-20260622-rag-poc-context-builder.md`
- `docs/context/CONTEXT-20260622-rag-poc-context-builder.md`

Can Skip：

- PRD：不新增产品流程。
- REQ：需求边界已由调研报告和本 SPEC 固化。
- PLAN：单一实现切片，不需要额外多阶段计划。

Upgrade Trigger：

- 若需要改 API / DB schema / frontend / HyDE / query rewrite / MCP，则升级为 L。

## 5. Subagent Decision

Use Subagents：No

Reason：项目规则提示 RAG 可用专家分析，但当前 Codex 工具策略要求只有用户明确要求 subagent 才能 spawn。该切片边界清晰，主 Codex 直接完成设计、实现与验证。

Implementation Mode：Single Codex。

## 6. Context Pack

见 `docs/context/CONTEXT-20260622-rag-poc-context-builder.md`。

## 7. Implementation Checklist

- [x] 写 `PocContextBuilderTest` 红灯测试。
- [x] 写 `RagQueryServiceTest` 红灯测试。
- [x] 实现 `PocContextBuilder` / `PocContextResult`。
- [x] 将 POC 上下文接入 `RagQueryService` answer 构造与 `sourcesJson` metadata。
- [x] 跑 focused tests。
- [x] 跑 adjacent tests。
- [x] 跑 backend compile。
- [x] 创建 Evidence / Acceptance。
- [x] 更新 changelog 与 RAG/project memory。

## 8. Evidence

- 红灯：`mvn test "-Dtest=PocContextBuilderTest,RagQueryServiceTest"` 首次失败，原因为 `PocContextBuilder` / `PocContextResult` 尚不存在，证明测试覆盖新能力缺口。
- Focused：`mvn test "-Dtest=PocContextBuilderTest,RagQueryServiceTest"` 通过，25 tests, 0 failures, 0 errors。
- Adjacent：`mvn test "-Dtest=AiQaControllerTest,QaRuntimeTest,MemoryContextServiceTest,RagEvaluationServiceTest,ChunkServiceVectorRetrievalTest"` 通过，16 tests, 0 failures, 0 errors。
- Compile：`mvn compile -q` 通过。
- Full backend `mvn test` 未跑；原因：本切片不改 API / DB schema / dependency / frontend，已覆盖 RAG 查询服务、POC builder、AI QA runtime/controller、memory context、RAG evaluation 和 chunk vector adjacent tests。

## 9. Acceptance

- Verdict：PASS。
- POC context 可从命中源 chunk 扩展父级链、相邻 chunk、子 chunk。
- `answer` 使用扩展上下文；`sources` 与 `source_citation` 仍只锚定原始命中源 chunk。
- `sourcesJson.pocContext` 只保存安全计数 metadata，不保存扩展正文。
- permission-first、no-source refusal、requestId replay、reranker fallback 和 AI QA adjacent tests 保持通过。
