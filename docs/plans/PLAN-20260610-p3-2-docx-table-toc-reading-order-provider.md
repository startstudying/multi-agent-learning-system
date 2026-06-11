# P3-2 子任务：DOCX table/TOC reading-order provider 实施计划

## Skill Selection Report

### Task Type

RAG / parser 生产化增强。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 S/M/L、文档、测试、证据、验收闭环 |
| ai-learning-agent-development | 保持 AI learning system 架构边界 |
| educational-rag-pipeline | RAG parsing / chunking / indexing 规则 |
| rag-parser-boundary | 项目专属 parser boundary 与 metadata 安全规则 |
| dispatching-parallel-agents | 用户要求专家 subagent 并行开发 |
| test-driven-development | 实现前先写 RED 测试 |

### Missing Skills

无。

### GitHub Research Needed

否。本切片使用已引入的 Apache POI，并通过本地已安装 jar API 验证 `getBodyElements()`、`XWPFTable#getRows()`、`XWPFTableRow#getTableCells()`、`XWPFTableCell#getParagraphs()`。

### New Project-Specific Skill To Create

不创建新 skill；完成后更新 `rag-parser-boundary.md`。

## Size Classification

Size: M

Reason:

- 影响 RAG parser 行为与 index metadata 验证。
- 不改 API/DB/frontend/dependency。
- 需要 focused + adjacent + full backend 验证。

Required Documents:

- REQ
- SPEC
- PLAN
- TASK
- CONTEXT
- Evidence
- Acceptance

Can Skip:

- PRD：无产品交互或用户流程变更。
- Dependency review：不新增依赖。

Upgrade Trigger:

- 如果新增依赖、改 API/DB、引入真实渲染页码或 OCR provider，则升级为 L 或新 M/L 子任务。

## Subagent Decision

Use Subagents: Yes

Reason:

- 任务涉及 RAG/parser 与测试验收，且用户明确要求专家 subagent 并行开发。

Parallelism Level: L1

Selected Subagents:

- RAG/parser 架构专家
- 测试/验收专家

Implementation Mode:

- 专家并行只读分析。
- 主 Codex 单线程集成实现，避免多个 agent 修改同一 parser 文件。

## Architecture Drift Check - Before

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser provider 内聚在 `rag/parser` |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | RAG parser boundary 内增强 |
| Security | PASS | 不新增依赖，不写 secret/raw XML |
| API / Database | PASS | 不改 API/DB |

## 实施步骤

1. 创建 RED 测试：
   - POI provider table body order / TOC skip / `TABLE_TEXT`
   - lightweight parser XML table body order / TOC skip
   - IndexService metadata propagation
2. 运行 focused 测试确认 RED。
3. 修改 `PoiDocxDocumentFormatParser`：
   - body elements 遍历
   - table text section
   - TOC skip
4. 修改 `DocumentParserService` lightweight DOCX fallback：
   - paragraph/table XML body order
   - table text section
   - TOC skip
5. 运行 focused 测试确认 GREEN。
6. 运行 adjacent 测试。
7. 运行 full backend `mvn test`。
8. 更新 Evidence / Acceptance / Changelog / Memory / TODO / project-specific skill。

## 风险与控制

- 风险：TOC 识别不完整。控制：只承诺 style `TOC*` 和 field instruction `TOC` 的最小识别。
- 风险：table 过度结构化。控制：只输出纯文本，不输出坐标/样式/XML。
- 风险：误称工业级 layout 完成。控制：文档明确本切片不关闭 P3-2 父项。
