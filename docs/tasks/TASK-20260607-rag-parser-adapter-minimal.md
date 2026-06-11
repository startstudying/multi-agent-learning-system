# TASK - RAG Parser Adapter 最小生产切片

## 1. 追踪

- PRD: `docs/product/PRD-20260607-rag-parser-adapter-minimal.md`
- REQ: `docs/requirements/REQ-20260607-rag-parser-adapter-minimal.md`
- SPEC: `docs/specs/SPEC-20260607-rag-parser-adapter-minimal.md`
- PLAN: `docs/plans/PLAN-20260607-rag-parser-adapter-minimal.md`
- 任务编号: `TASK-20260607-rag-parser-adapter-minimal`

## 2. 目标

把 RAG 文档解析从 `IndexService` 中抽出统一 parser 边界，覆盖 Markdown / TXT / PDF / DOCX，保持当前 chunk 行为和 worker/manual 一致。

## 3. 范围内工作

- 新增 `backend/src/main/java/com/learningos/rag/parser/**`
- 调整 `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- 新增 parser 边界单测
- 保留现有 chunk/hash/worker 测试
- 更新 Evidence / Acceptance / Retro / Changelog / Memory / TODO

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskWorkerSchedulerTest.java`
- `backend/src/test/java/com/learningos/rag/parser/**`
- `docs/product/PRD-20260607-rag-parser-adapter-minimal.md`
- `docs/requirements/REQ-20260607-rag-parser-adapter-minimal.md`
- `docs/specs/SPEC-20260607-rag-parser-adapter-minimal.md`
- `docs/plans/PLAN-20260607-rag-parser-adapter-minimal.md`
- `docs/tasks/TASK-20260607-rag-parser-adapter-minimal.md`
- `docs/context/CONTEXT-20260607-rag-parser-adapter-minimal.md`
- `docs/evidence/EVIDENCE-20260607-rag-parser-adapter-minimal.md`
- `docs/acceptance/ACCEPT-20260607-rag-parser-adapter-minimal.md`
- `docs/retrospectives/RETRO-20260607-rag-parser-adapter-minimal.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-parser-boundary.md`

## 5. 禁止修改的文件

- `frontend/**`
- `docs/superpowers/**`
- 与 parser 边界无关的 Agent / Orchestrator / VectorDB / model 接入模块
- 不需要的新增依赖与构建脚本

## 6. 验证命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
mvn test
```

## 7. 完成标准

- parser 边界已抽出，IndexService 不再内嵌全部解析逻辑。
- Markdown / TXT / PDF / DOCX 解析测试通过。
- worker/manual 路径一致性保持。
- Evidence / Acceptance / Retro / Changelog / Memory / TODO 更新完成。

## 8. 完成记录

- 状态：已完成。
- 实现：新增 `rag/parser` 边界，`IndexService` 改为消费 `DocumentParserService.parse(...)` 的统一 section 输出。
- 测试：`DocumentParserServiceTest`、`IndexServiceParserFailureTest`、`IndexServiceTest`、`IndexTaskWorkerSchedulerTest`、`SchemaConvergenceMigrationTest`、`MysqlMigrationSmokeTest`、`mvn test` 已按 Evidence 记录执行。
- 交付：Evidence、Acceptance、Retrospective、Changelog、Memory、TODO、项目 skill 提取已更新。
