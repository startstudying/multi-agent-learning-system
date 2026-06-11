# EVIDENCE-20260610 P3-2 子任务：parser/OCR TODO 状态对账

## Scope

本证据只记录 P3-2 parser/OCR TODO 状态对账，不新增后端功能代码。

## Reconciled TODO Item

| TODO item | Status | Evidence |
|---|---|---|
| 补齐复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别 | Done for minimum productionized parser/OCR capability | P3-2-E/F/G/H/I 已完成 parser provider boundary、PDFBox/POI provider、best-effort `pageNum` / `headingPath`、configurable OCR fallback、process OCR provider，并有 focused/adjacent/full backend 验收证据。 |

## Evidence Read

- `docs/acceptance/ACCEPT-20260609-rag-parser-layout-hierarchy.md`
  - P3-2-E 通过 PDF simple multi-page、DOCX page break、DOCX tab/line break、chunk page metadata 验收。
  - 明确 OCR fallback、真实 PDF/DOCX parser SDK、复杂 PDF 页码/章节层级仍是后续；这些后续已由 P3-2-G/H/I 部分推进。
- `docs/evidence/EVIDENCE-20260609-p3-2-f-parser-provider-boundary.md`
  - `DocumentParserService` 已形成 provider registry。
  - `OcrFallbackService` / `NoopOcrFallbackService` 已建立 disabled/noop OCR contract。
- `docs/acceptance/ACCEPT-20260609-p3-2-g-real-parser-sdk-provider.md`
  - Apache PDFBox provider 已按页抽取真实 PDF 文本并保留 `pageNum`。
  - Apache POI DOCX provider 已抽取 Heading1-6、page break、tab/line break，并保留 heading/page metadata。
  - 明确工业级 PDF layout/table/TOC、完整 DOCX 样式体系和真实 OCR 不在该切片内。
- `docs/acceptance/ACCEPT-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
  - OCR fallback selection 已通过 `learning-os.rag.parser.ocr.enabled/provider` 配置。
  - 默认 disabled，missing provider / provider exception 返回安全状态码。
- `docs/acceptance/ACCEPT-20260609-p3-2-i-real-ocr-provider.md`
  - `ProcessOcrFallbackProvider` 已通过显式 runtime command、stdin bytes、stdout text、timeout/output limit 完成最小 process OCR provider。
  - 明确 OCR SDK/native/cloud provider、OCR confidence、layout/table/TOC/reading-order 仍是独立后续。
- `docs/skills/project-specific/rag-parser-boundary.md`
  - 明确真实 SDK provider 完成后不能宣称 OCR 或工业级 layout 完成；OCR、表格、目录、复杂阅读顺序恢复必须作为独立切片验收。

## Subagent Review

Agent/RAG Architect verdict: CONDITIONAL.

Key conclusion:

- 可以把 P3-2 TODO 勾选为“最小生产化能力已完成”。
- 不能写成工业级复杂 PDF/DOCX、native/cloud OCR、OCR confidence、真实渲染页码已经完成。
- 应新增或保留后续 TODO：工业级 PDF/DOCX layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 与真实渲染页码增强。

Full report: `docs/subagents/runs/RUN-20260610-p3-2-parser-ocr-todo-status-reconciliation.md`.

## Verification

Focused verification:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ConfigurableOcrFallbackServiceTest,ProcessOcrFallbackProviderTest test
```

Result:

```text
Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent verification:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
```

Result:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Accepted Follow-up

- 工业级 PDF/DOCX layout/table/TOC/reading-order。
- Native/cloud OCR provider and privacy/runtime/security review.
- OCR confidence、页级质量、OCR metadata。
- DOCX true rendered page numbers / complete Word layout semantics.
- Real VectorDB adapter remains separate P3-2/P3-3 production work.

## Acceptance Verdict

Verdict: PASS.

- TASK acceptance criteria 1: PASS. `backend-architecture-todolist.md` 已将 parser/OCR TODO 标记为最小生产化完成，并保留边界说明。
- TASK acceptance criteria 2: PASS. 已新增独立未勾选后续项覆盖工业级 layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 和真实渲染页码。
- TASK acceptance criteria 3: PASS. 本对账任务未修改后端代码、schema、dependency、API、frontend 或 secret 文件。
