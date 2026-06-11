# PLAN - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 追踪

- PRD：`docs/product/PRD-20260609-rag-parser-layout-hierarchy.md`
- REQ：`docs/requirements/REQ-20260609-rag-parser-layout-hierarchy.md`
- SPEC：`docs/specs/SPEC-20260609-rag-parser-layout-hierarchy.md`

## 2. Skill Selection Report

### Task Type

RAG / retrieval parser enhancement。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 Spec-first 工作流推进 |
| `educational-rag-pipeline` | parser / chunk / citation 属 RAG 管线 |
| `rag-parser-boundary` | 约束 parser 边界、安全错误码和无 raw fallback |
| `test-driven-development` | 本切片需要 RED/GREEN 覆盖行为变化 |
| `security-review` | parser 输入是不可信文件，需保持资源和错误边界 |
| `verification-before-completion` | 完成前必须运行验证命令 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片不新增依赖，不引入外部 SDK。

### New Project-Specific Skill To Create

暂不创建；复用并在必要时补充 `rag-parser-boundary`。

## 3. Subagent Decision

| 项 | 决策 |
|---|---|
| Use Subagents | Yes |
| Reason | 涉及 RAG parser、安全边界、后续 VectorDB/OCR 取舍 |
| Parallelism Level | L1 并行分析 |
| Selected Subagents | Agent/RAG Expert、Dependency/VectorDB Expert、Security & Quality |
| Implementation Mode | Main Codex 串行 TDD |

## 4. 实施阶段

| 阶段 | 说明 | 状态 |
|---|---|---|
| 1 | 创建 workflow docs 与 Context Pack | 完成 |
| 2 | RED：补 parser/index 测试 | 完成 |
| 3 | GREEN：最小实现 PDF/DOCX layout/page 增强 | 完成 |
| 4 | focused/adjacent/full verification | 完成 |
| 5 | Evidence/Acceptance/Memory/TODO/Retro | 完成 |

## 5. 文件变更清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java` | 修改 | PDF best-effort page boundary；DOCX tab/line break 分隔 |
| `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java` | 修改 | RED/GREEN parser tests |
| `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java` | 修改 | chunk page metadata 回归 |
| 本任务 workflow / evidence / acceptance / memory / changelog / TODO 文档 | 新增/修改 | 工作流闭环 |

## 6. 依赖

- 新增依赖：无。
- `backend/pom.xml` 禁止修改。

## 7. 风险

| 风险 | 影响 | 缓解 |
|---|---|---|
| PDF best-effort 页码误判 | citation 页码可能不准确 | 明确非真实 PDF parser；只覆盖简单 marker |
| DOCX regex 解析有限 | 复杂 WordML 仍丢失结构 | 本轮只做低风险 tab/line break |
| 误把本切片当 OCR 完成 | TODO 状态误导 | Evidence/Acceptance 明确 OCR 非目标 |

## 8. 测试策略

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 9. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | APPROVED |

## 10. 完成记录

- 完成时间：2026-06-09
- 实现方式：Main Codex 串行 TDD；专家子代理在前置分析阶段已产出报告。本轮尝试再启动首个 gpt-5.5 代码审查子代理时命中 agent thread limit，未新增运行。
- 验证：
  - `mvn --% -Dtest=DocumentParserServiceTest test`：15 run, 0 failures, 0 errors。
  - `mvn --% -Dtest=IndexServiceTest#processSimpleMultiPagePdfPreservesPageNumbersInChunks test`：1 run, 0 failures, 0 errors。
  - `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`：15 run, 0 failures, 0 errors。
  - `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test`：45 run, 0 failures, 0 errors。
  - `mvn test`：361 run, 0 failures, 0 errors, 1 skipped。
