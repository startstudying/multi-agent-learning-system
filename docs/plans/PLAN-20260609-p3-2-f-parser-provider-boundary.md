# PLAN - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. 追踪

- PRD：`docs/product/PRD-20260609-p3-2-f-parser-provider-boundary.md`
- REQ：`docs/requirements/REQ-20260609-p3-2-f-parser-provider-boundary.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- TASK：`docs/tasks/TASK-20260609-p3-2-f-parser-provider-boundary.md`
- Evidence：`docs/evidence/EVIDENCE-20260609-p3-2-f-parser-provider-boundary.md`
- Acceptance：`docs/acceptance/ACCEPT-20260609-p3-2-f-parser-provider-boundary.md`

## 2. Skill Selection Report

### Task Type

RAG / retrieval parser refactor。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 Spec-first 工作流推进 |
| `educational-rag-pipeline` | parser / chunk / citation 属于 RAG 索引链路 |
| `rag-parser-boundary` | 约束 parser 边界、安全错误码、无 raw fallback |
| `test-driven-development` | provider/OCR contract 需要 RED/GREEN 验证 |
| `security-review` | parser 输入是不可信文件，需保持错误和依赖边界 |
| `dependency-review` | 本切片评估但不新增 PDF/OCR 依赖 |
| `verification-before-completion` | 完成前必须运行 fresh verification |

### Missing Skills

无。

### GitHub Research Needed

No。项目已有 `rag-parser-boundary` 技能，本切片不新增外部 SDK，不需要 GitHub Reference Gate。

### New Project-Specific Skill To Create

不创建。复用现有 `rag-parser-boundary`。

## 3. Multi-Expert Subagent Gate

| 项 | 决策 |
|---|---|
| Use Subagents | Yes |
| Reason | RAG parser + dependency/OCR/security 边界，符合 RAG/安全专家前置审查 |
| Parallelism Level | L1 并行分析 |
| Selected Subagents | Architecture、Test、Dependency、Integration |
| Implementation Mode | Main Codex 单任务串行 TDD |

专家报告：

- `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-architecture.md`
- `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-test.md`
- `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-dependency.md`
- `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-integration.md`

## 4. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 进入实现前没有 `DocumentFormatParser` / `OcrFallbackService` |
| Architecture compliance | PASS | parser 保持在 `rag/parser`，`IndexService` 不解析格式 |
| Official docs verified | N/A | 本切片不新增 SDK/API 依赖 |
| OSS reference | N/A | 不复制 GitHub 代码，不引入外部实现 |
| Root cause identified | PASS | 单体 `DocumentParserService` 会阻塞后续真实 parser/OCR 接入 |

Confidence：0.95。

## 5. 实施阶段

| 阶段 | 说明 | 状态 |
|---|---|---|
| 1 | 创建 workflow docs 与 Context Pack | 完成 |
| 2 | RED：新增 provider/OCR contract 测试 | 完成 |
| 3 | GREEN：最小重构 parser provider boundary | 完成 |
| 4 | focused/adjacent/full verification | 完成 |
| 5 | Evidence/Acceptance/Memory/TODO/Retro | 完成 |

## 6. 文件变更

| 文件 | 操作 | 说明 |
|---|---|---|
| `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java` | 修改 | 改为 provider registry；轻量 provider 作为内部默认 provider |
| `backend/src/main/java/com/learningos/rag/parser/ParseInput.java` | 新增 | provider 输入，不携带 storage path/key |
| `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java` | 新增 | provider contract |
| `backend/src/main/java/com/learningos/rag/parser/OcrFallbackService.java` | 新增 | OCR fallback contract |
| `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java` | 新增 | OCR fallback result |
| `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java` | 新增 | disabled/noop OCR |
| `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java` | 修改 | provider/OCR/parser 回归 |
| `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java` | 新增 | noop OCR 测试 |
| workflow / evidence / acceptance / memory / changelog / TODO | 新增/修改 | 工作流闭环 |

## 7. 禁止修改确认

- 未修改 `backend/pom.xml`。
- 未修改 `backend/src/main/resources/db/migration/**`。
- 未修改 `frontend/**`。
- 未修改 `docs/superpowers/**`。
- 未修改 `EmbeddingService` / `VectorIndexAdapter` / `RagQueryService`。

## 8. 依赖

新增依赖：无。

依赖审查记录：`docs/security/DEPENDENCY-REVIEW-20260609-p3-2-f-parser-provider-boundary.md`。

## 9. 风险与处理

| 风险 | 处理 |
|---|---|
| provider 拆分改变 parser selection | focused + adjacent parser/index 测试覆盖 |
| OCR noop 被误认为真实 OCR | Evidence/Acceptance 明确真实 OCR 非目标 |
| provider 异常绕过 safe code | service 统一 catch/mapping |
| Spring 构造方式影响启动 | 使用 `ObjectProvider` 可选注入并通过 full backend 验证 |

## 10. 验证结果

- RED：`mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test` 初次失败于 `testCompile`，缺少 contract/provider/noop OCR，符合预期。
- Focused：`mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test`，`19 run, 0 failures, 0 errors`。
- Adjacent：`mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`，`15 run, 0 failures, 0 errors`。
- Extended adjacent：`mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test`，`49 run, 0 failures, 0 errors`。
- Full backend：`mvn test`，`371 run, 0 failures, 0 errors, 1 skipped`。

## 11. 完成状态

状态：完成。

完成日期：2026-06-09。
