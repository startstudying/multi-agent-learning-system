# RAG 索引恢复调度与并发锁 Context Pack

## Related Memory And Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/specs/SPEC-20260606-rag-index-active-dedup.md`
- `docs/specs/SPEC-20260606-rag-index-timeout-recovery.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## Selected Skills

- `feature-development-workflow`
- `ai-learning-agent-development`
- `educational-rag-pipeline`
- `test-driven-development`
- `unit-test-generator`
- `verification-before-completion`

## Subagent Plan

不启用新的子代理。本 Worker 是主 Codex 派发的后端切片执行者，按单任务顺序实现。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/java/com/learningos/config/IndexRecoveryProperties.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexTaskRecoveryScheduler.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocumentRepository.java`
- `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskRecoverySchedulerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`（仅必要时）
- `docs/product/PRD-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/requirements/REQ-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/specs/SPEC-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/plans/PLAN-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/tasks/TASK-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/context/CONTEXT-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/evidence/EVIDENCE-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/acceptance/ACCEPT-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/retrospectives/RETRO-20260606-rag-index-recovery-scheduler-lock.md`

## Files Not Allowed To Modify

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/domain/KbDocument.java`
- 文档上传幂等相关实现文件
- 数据库迁移文件
- 前端代码
- `docs/memory/*`
- `docs/changelog/CHANGELOG.md`
- `docs/planning/backend-architecture-todolist.md`

## Test Commands

```powershell
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest,IndexTaskRecoverySchedulerTest" test
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
```

## Current Task Boundary

只处理 RAG 文档索引任务的服务层并发去重锁和自动恢复入口。上传幂等、真实索引 worker、解析/embedding、后台重试队列和 MySQL smoke 不在本切片范围。
