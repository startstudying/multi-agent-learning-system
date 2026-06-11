# PLAN - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 追踪

- PRD：`docs/product/PRD-20260609-p3-2-g-real-parser-sdk-provider.md`
- REQ：`docs/requirements/REQ-20260609-p3-2-g-real-parser-sdk-provider.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-g-real-parser-sdk-provider.md`
- TASK：`docs/tasks/TASK-20260609-p3-2-g-real-parser-sdk-provider.md`
- Context：`docs/context/CONTEXT-20260609-p3-2-g-real-parser-sdk-provider.md`
- Dependency Review：`docs/security/DEPENDENCY-REVIEW-20260609-p3-2-g-real-parser-sdk-provider.md`

## 2. Skill Selection Report

### Task Type

RAG / retrieval parser productionization。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制 spec-first 流程 |
| `educational-rag-pipeline` | parser 是 RAG 索引链路入口 |
| `rag-parser-boundary` | 约束 provider、safe error、无 raw fallback |
| `dependency-review` | 新增 PDFBox/POI 依赖 |
| `security-review` | 不可信文件解析与资源限制 |
| `test-driven-development` | provider 行为需 RED/GREEN |
| `architecture-drift-check` | 防止解析逻辑进入 `IndexService` |
| `verification-before-completion` | 完成声明前 fresh verification |

### Missing Skills

无。

### GitHub Research Needed

No。采用 Apache 官方成熟库，不需要 GitHub Reference Gate。

### New Project-Specific Skill To Create

暂不创建；完成后如有可复用规则，扩展 `rag-parser-boundary`。

## 3. Multi-Expert Subagent Gate

| Item | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | RAG + dependency + security |
| Parallelism Level | L1/L2 analysis/design |
| Selected Subagents | Architect Parfit（gpt-5.5）；dependency/test/security 由主线补充，第二专家因 thread limit 未启动 |
| Implementation Mode | Main Codex 单任务 TDD |

## 4. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 当前无 PDFBox/POI/Tika/docx4j provider |
| Architecture compliance | PASS | 继续走 `rag/parser` provider |
| Official docs verified | PASS | 已核对 PDFBox/POI 官网与 Maven metadata |
| OSS implementation referenced | PASS | Apache PDFBox / Apache POI 官方 OSS |
| Root cause identified | PASS | lightweight parser 无法覆盖真实 PDF/DOCX |

Confidence：0.95。

## 5. 实施阶段

| Phase | Description | Status |
|---|---|---|
| 1 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/Dependency Review | Completed |
| 2 | RED：新增真实 parser provider tests | Completed |
| 3 | GREEN：新增依赖与 provider 实现 | Completed |
| 4 | Focused / adjacent / dependency tree / full verification | Completed |
| 5 | Evidence / Acceptance / Changelog / Memory / TODO / Retro | Completed |

## 6. Verification Summary

- Focused parser tests: `26 run, 0 failures, 0 errors, 0 skipped`
- Adjacent index tests: `15 run, 0 failures, 0 errors, 0 skipped`
- Dependency tree: `pdfbox:3.0.7`, `poi-ooxml:5.5.1`, and no `commons-logging` tree entry
- Full backend: `378 run, 0 failures, 0 errors, 1 skipped`

## 7. 风险与缓解

| Risk | Mitigation |
|---|---|
| 不可信文件导致 CPU/内存压力 | 文件大小、页数、段落数、输出字符数限制 |
| PDFBox 3.0.7 examples CVE | 不引入 examples/tools，不复制相关示例；只用 core text extraction |
| POI 解析异常类型复杂 | broad catch 由 `DocumentParserService` 映射 safe code |
| `IndexService` 边界漂移 | Context Pack 禁止修改 `IndexService` |
| OCR 范围膨胀 | 明确 OCR 非目标 |

## 8. 依赖审批

本 PLAN 批准新增：

- `org.apache.pdfbox:pdfbox:3.0.7`
- `org.apache.poi:poi-ooxml:5.5.1`

条件：

1. 只在 `rag/parser` provider 内使用。
2. 不引入 parser 以外的业务调用点。
3. 不持久化 raw parser error。
4. 不新增 OCR/native/外部服务依赖。
