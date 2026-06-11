# TASK - P3-2-C RAG 无依赖 Parser 加固

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260608-rag-parser-hardening.md`
- SPEC：`docs/specs/SPEC-20260608-rag-parser-hardening.md`
- 任务编号：TASK-20260608-rag-parser-hardening

## 2. 目标

在不新增依赖、schema、API、frontend 的前提下，加固 RAG parser，使不可可靠解析内容不污染 chunk，并为 DOCX 输出 best-effort heading/page metadata。

## 3. 范围

### 纳入范围

- PDF `Tj` / `TJ` 轻量文本抽取。
- PDF 无可抽取文本时不 fallback raw bytes。
- DOCX zip/XML 资源上限。
- DOCX paragraph / heading style / page break best-effort。
- TXT/Markdown binary garbage 拒绝。
- parser failure safe code 回归。
- index chunk metadata 回写回归。

### 排除范围

- 真实 OCR。
- PDFBox/POI/Tika/docx4j/iText/Tesseract/云 OCR。
- Maven dependency、DB schema、API、frontend。
- 完整复杂 PDF/DOCX 工业级解析。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`
- `docs/product/PRD-20260608-rag-parser-hardening.md`
- `docs/requirements/REQ-20260608-rag-parser-hardening.md`
- `docs/specs/SPEC-20260608-rag-parser-hardening.md`
- `docs/plans/PLAN-20260608-rag-parser-hardening.md`
- `docs/tasks/TASK-20260608-rag-parser-hardening.md`
- `docs/context/CONTEXT-20260608-rag-parser-hardening.md`
- `docs/subagents/runs/RUN-20260608-rag-parser-hardening-*.md`
- `docs/evidence/EVIDENCE-20260608-rag-parser-hardening.md`
- `docs/acceptance/ACCEPT-20260608-rag-parser-hardening.md`
- `docs/retrospectives/RETRO-20260608-rag-parser-hardening.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG query/citation/embedding/vector adapter 生产代码
- unrelated agent/resource/assessment modules

## 6. 实施步骤

1. 补齐 workflow docs、subagent integration review、Context Pack。
2. 写 RED tests：PDF raw fallback、PDF `TJ`、DOCX heading/page break、DOCX zip limit、TXT/Markdown binary、parser safe error、IndexService metadata。
3. 运行 focused RED：`DocumentParserServiceTest` 和必要 index tests，确认因缺失能力失败。
4. 修改 `DocumentParserService`，只做最小实现。
5. 运行 focused GREEN。
6. 运行 adjacent regression 和 full backend tests。
7. 生成 Evidence / Acceptance / Retrospective。
8. 更新 Changelog / Memory / Skill / TODO。

## 7. 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test
mvn --% dependency:tree -Dscope=compile
mvn test
```

## 8. 完成标准

- [x] PRD/REQ/SPEC/PLAN/TASK/Context 已存在。
- [x] RED tests 已验证失败原因正确。
- [x] PDF 无文本不索引 raw bytes。
- [x] PDF `Tj` / `TJ` 抽取通过。
- [x] DOCX heading/page break 输出通过。
- [x] DOCX zip/XML 上限通过。
- [x] DOCX 不解压非目标 entry body，central directory / local header 名称不一致时安全失败。
- [x] TXT/Markdown binary 拒绝通过。
- [x] parser failure 不泄露 raw cause。
- [x] IndexService chunk/page/heading metadata 回归通过。
- [x] 不新增 dependency/schema/API/frontend。
- [x] Evidence/Acceptance/Changelog/Memory/Retro/Skill 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-08 |
| 完成日期 | 2026-06-08 |

## 10. 完成证据

- Evidence：`docs/evidence/EVIDENCE-20260608-rag-parser-hardening.md`
- Acceptance：`docs/acceptance/ACCEPT-20260608-rag-parser-hardening.md`
- Code Review：Feynman reviewer 最终 `APPROVE`
- 最终验证：
  - `mvn --% -Dtest=DocumentParserServiceTest test`：12 tests, 0 failures, 0 errors
  - `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`：14 tests, 0 failures, 0 errors
  - `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test`：45 tests, 0 failures, 0 errors
  - `mvn --% dependency:tree -Dscope=compile`：BUILD SUCCESS，未新增 parser/OCR 依赖
  - `mvn test`：298 tests, 0 failures, 0 errors, 1 skipped
