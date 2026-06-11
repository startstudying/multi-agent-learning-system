# Context Pack - P3-2-C RAG 无依赖 Parser 加固

## 当前任务

实现 P3-2-C no-dependency parser hardening：加固 PDF/DOCX/TXT/Markdown parser，不新增 dependency/schema/API/frontend，确保不可解析或二进制垃圾内容不进入 RAG chunk。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 关联文档

- PRD：`docs/product/PRD-20260608-rag-parser-hardening.md`
- REQ：`docs/requirements/REQ-20260608-rag-parser-hardening.md`
- SPEC：`docs/specs/SPEC-20260608-rag-parser-hardening.md`
- PLAN：`docs/plans/PLAN-20260608-rag-parser-hardening.md`
- TASK：`docs/tasks/TASK-20260608-rag-parser-hardening.md`
- 既有 parser minimal SPEC：`docs/specs/SPEC-20260607-rag-parser-adapter-minimal.md`
- Parser skill：`docs/skills/project-specific/rag-parser-boundary.md`
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 漂移检查：`docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 已选 Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `spring-boot-architecture`
- `test-generator`
- `test-driven-development`
- `security-review`
- `architecture-drift-check`
- `verification-before-completion`

## Subagent 计划

### 是否启用 Subagent

是。

### 原因

任务涉及 RAG parser 与安全/质量边界，按项目规则必须启用 Agent/RAG Expert 与 Security & Quality。

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 2 | 是 | 是 |

### 选中的专家

- Agent/RAG 专家：`docs/subagents/runs/RUN-20260608-rag-parser-hardening-agent-rag.md`
- 安全与质量：`docs/subagents/runs/RUN-20260608-rag-parser-hardening-security-quality.md`
- 集成评审员：`docs/subagents/runs/RUN-20260608-rag-parser-hardening-integration-review.md`

### 并行级别

- [x] L1 - 仅并行分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

单 Codex 实施。

### 文件归属

| 领域 | 负责人 | 允许修改的文件 |
|---|---|---|
| RAG Parser | Main Codex | `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java` |
| Parser Tests | Main Codex | `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java` |
| Index Tests | Main Codex | `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`, `IndexServiceParserFailureTest.java` |
| Workflow Docs | Main Codex | 本 Context Pack 中列出的 P3-2-C docs |

## 关联代码区域

- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`

## 允许修改的文件

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

## 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`

## 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test
mvn --% dependency:tree -Dscope=compile
mvn test
```

## 任务边界

本次只完成 TASK-20260608-rag-parser-hardening。不得切换到 P3-4-C 权限收口、P3-3 真实模型接入、真实 OCR、真实 parser SDK、真实 VectorDB 或前端任务。

