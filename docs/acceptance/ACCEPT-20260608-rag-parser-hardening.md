# Acceptance - P3-2-C RAG 无依赖 Parser 加固

## 1. 验收结论

状态：Accepted。

本切片已按 PRD / REQ / SPEC / PLAN / TASK 完成 no-dependency parser hardening：PDF 不再索引 raw binary fallback，DOCX 只读取 `word/document.xml` 且具备 entry/XML 资源边界，TXT/Markdown 明显二进制输入安全失败，parser failure 只持久化 safe code。不新增 dependency、schema、公开 API 或 frontend 变更。

## 2. Done Definition

| 项目 | 状态 | 证据 |
|---|---|---|
| PRD 存在 | PASS | `docs/product/PRD-20260608-rag-parser-hardening.md` |
| REQ 存在 | PASS | `docs/requirements/REQ-20260608-rag-parser-hardening.md` |
| SPEC 存在并更新状态 | PASS | `docs/specs/SPEC-20260608-rag-parser-hardening.md` |
| PLAN / TASK 存在并关闭 | PASS | `docs/plans/PLAN-20260608-rag-parser-hardening.md`、`docs/tasks/TASK-20260608-rag-parser-hardening.md` |
| Context Pack 存在 | PASS | `docs/context/CONTEXT-20260608-rag-parser-hardening.md` |
| Evidence 存在 | PASS | `docs/evidence/EVIDENCE-20260608-rag-parser-hardening.md` |
| Changelog / Memory / TODO 更新 | PASS | `docs/changelog/CHANGELOG.md`、`docs/memory/*.md`、`docs/planning/backend-architecture-todolist.md` |
| Retrospective / Skill Extraction | PASS | `docs/retrospectives/RETRO-20260608-rag-parser-hardening.md`、`docs/skills/project-specific/rag-parser-boundary.md` |
| Code Review | PASS | Feynman reviewer 最终 `APPROVE`，无 Critical/Important/Minor 遗留 |

## 3. 功能验收

| 验收项 | 结果 | 说明 |
|---|---|---|
| PDF raw fallback 移除 | PASS | 无可抽取文本 PDF 返回空 sections，由索引层走空文档失败，不生成 `%PDF` 垃圾 chunk。 |
| PDF `Tj` / `TJ` | PASS | `Tj` 与简单 `TJ` array 均可抽取文本；最小 escape 解码通过。 |
| DOCX entry/XML limit | PASS | 超过 entry count、超大 `word/document.xml`、截断/损坏 zip 均映射 `DOCUMENT_PARSE_FAILED`。 |
| DOCX 不解压非目标 entry | PASS | 先读 central directory，再只解压 `word/document.xml`；损坏非目标 entry body 不影响目标解析。 |
| DOCX local header 校验 | PASS | central directory 与 local header entry name 不一致时安全失败。 |
| DOCX heading/page metadata | PASS | `Heading1`-`Heading6` 更新 heading stack；正文 section 继承 heading metadata；page break 递增 best-effort `pageNum`。 |
| TXT/Markdown binary 拒绝 | PASS | malformed UTF-8、NUL、明显 control garbage 映射 `DOCUMENT_PARSE_FAILED`。 |
| Parser safe error | PASS | `IndexServiceParserFailureTest` 验证 raw cause/path/secret/text 不持久化。 |
| Index metadata 回归 | PASS | DOCX parser/page/heading metadata 继续写入 chunk metadata。 |

## 4. 验证结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=DocumentParserServiceTest test` | BUILD SUCCESS，12 tests，0 failures，0 errors |
| `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test` | BUILD SUCCESS，14 tests，0 failures，0 errors |
| `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test` | BUILD SUCCESS，45 tests，0 failures，0 errors |
| `mvn --% dependency:tree -Dscope=compile` | BUILD SUCCESS，未新增 parser/OCR dependency |
| `mvn test` | BUILD SUCCESS，298 tests，0 failures，0 errors，1 skipped |

## 5. 非目标确认

- 未实现真实 OCR。
- 未接入 Apache PDFBox / POI / Tika / docx4j / iText / Tesseract / 云 OCR。
- 未新增 Maven dependency。
- 未新增 Flyway migration。
- 未修改 frontend。
- 未修改 public API。
- 未宣称复杂 PDF/DOCX 工业级解析完成。

## 6. 验收备注

本验收关闭 P3-2-C no-dependency parser hardening。后续若要处理复杂 PDF/DOCX、OCR、真实页码、完整章节识别或引入 parser SDK，必须重新创建 PRD/REQ/SPEC/PLAN/TASK/Context Pack，并完成 dependency/security review。
