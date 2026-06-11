# RAG 质量评估 Context Pack

## Current Task

实现 TODO P2-2「RAG 质量评估」的最小可验收后端切片：在现有 `RagEvaluationService` / Controller 基础上补齐课程 benchmark、`Recall@K`、`Citation Accuracy`、`Groundedness`、`No-source Refusal Rate` 和可归档报告输出。

## Related Memory and Docs

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-evaluation-set-management.md`
- `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md`
- `docs/specs/SPEC-20260606-rag-query-replay-snapshot.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

## Selected Skills

- `feature-development-workflow`
- `rag-project-review`
- `spring-boot-architecture`
- `api-contract-design`
- `test-generator`
- `architecture-drift-check`
- local Codex skills: `test-driven-development`、`educational-rag-pipeline`、`spring-ai-agent-backend`

## Subagent Plan

- 当前会话是并行后端 worker。
- 不启动额外并行实现者。
- 产出 `docs/subagents/runs/RUN-20260606-rag-quality-evaluation-worker.md` 作为本 worker 的分析和实现记录。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/rag/**`
- `backend/src/test/java/com/learningos/rag/**`
- `docs/product/PRD-20260606-rag-quality-evaluation.md`
- `docs/requirements/REQ-20260606-rag-quality-evaluation.md`
- `docs/specs/SPEC-20260606-rag-quality-evaluation.md`
- `docs/plans/PLAN-20260606-rag-quality-evaluation.md`
- `docs/tasks/TASK-20260606-rag-quality-evaluation.md`
- `docs/context/CONTEXT-20260606-rag-quality-evaluation.md`
- `docs/evidence/EVIDENCE-20260606-rag-quality-evaluation.md`
- `docs/acceptance/ACCEPT-20260606-rag-quality-evaluation.md`
- `docs/subagents/runs/RUN-20260606-rag-quality-evaluation-worker.md`

## Files Not Allowed To Modify

- `docs/changelog/CHANGELOG.md`
- `docs/memory/*`
- `docs/planning/backend-architecture-todolist.md`
- `backend/src/main/java/com/learningos/evaluation/**`
- `backend/src/test/java/com/learningos/evaluation/**`
- `backend/src/main/resources/db/migration/V14__evaluation_run_quality_metrics.sql`
- 其他未列入允许范围的前端、后端、数据库迁移文件

## Test Commands

```bash
cd backend; mvn "-Dtest=RagEvaluationServiceTest,RagEvaluationControllerTest" test
```

## Task Boundary

## Worker A 补充：RAG 评估归档自动化

### Current Task

实现 TODO P2-2 剩余项：增加可周期执行的 RAG 质量评估脚本或测试，输出可归档报告。

### Files Allowed To Modify

- `scripts/run-rag-evaluation-archive.ps1`
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportCli.java`
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportTest.java`
- `docs/context/CONTEXT-20260606-rag-quality-evaluation.md`
- `docs/evidence/EVIDENCE-20260606-rag-quality-evaluation.md`
- `docs/acceptance/ACCEPT-20260606-rag-quality-evaluation.md`

### Test / Script Commands

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-evaluation-archive.ps1
```

健康测试树恢复后可运行等价 JUnit 合约：

```powershell
cd backend; mvn "-Dtest=RagEvaluationArchiveReportTest" "-Drag.evaluation.archiveDir=target/rag-evaluation-archive/latest" test
```

### Boundary

- 不修改生产 RAG 服务、API 合同、数据库迁移、token budget、analytics 或 index 生产服务。
- 脚本使用现有 `RagEvaluationService` 和确定性 benchmark fixture 生成 Markdown 报告。
- 当前仓库存在与本任务无关的 Maven compile/testCompile 错误，脚本避免编译无关模块，只编译 RAG evaluation 归档所需的最小源码集合。

- 只评估请求中提供的 benchmark 输出，不触发真实检索或模型生成。
- 不做 evaluation run 持久化。
- 不做定时任务。
- 不新增依赖。
- 保持旧请求兼容。
