# RAG 查询重放与响应快照任务

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260606-rag-query-replay-snapshot.md`
- SPEC：`docs/specs/SPEC-20260606-rag-query-replay-snapshot.md`
- 任务编号：TASK-20260606-rag-query-replay-snapshot

## 2. 目标

补齐 P0-3 中 `RAG_QA` 的 `requestId` 幂等、响应快照和冲突检测，保证重复 RAG query 不产生重复业务结果。

## 3. 范围

### 纳入范围

- `kb_query_log` replay 字段和唯一索引。
- `RagQueryService` 幂等查询入口。
- `/api/rag/query` 可选 `requestId`。
- Orchestrator `RAG_QA` 创建前 replay / conflict 检查。
- 服务层、Orchestrator 和迁移测试。

### 排除范围

- 文档上传幂等。
- 索引任务数据库锁。
- 后台恢复任务。
- 真实 VectorDB / LLM 接入。
- 前端页面。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
- `backend/src/main/java/com/learningos/rag/repository/KbQueryLogRepository.java`
- `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java`
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/resources/db/migration/V7__rag_query_replay_snapshot.sql`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- 本任务对应 `docs/**`、memory、changelog、TODO。

## 5. 完成标准

- [x] RED 测试证明当前 RAG query replay 缺失。
- [x] 首次 RAG 查询写入 `requestId/requestHash/responseJson`。
- [x] 重复相同请求重放第一次响应，表计数不变。
- [x] 不同 payload 复用 `requestId` 返回 409。
- [x] Orchestrator `RAG_QA` 重放返回首次 workflow，不新增 task/trace/query/citation。
- [x] 无来源响应可重放且不写 citation。
- [x] 聚焦、交叉回归、全量测试完成。
- [x] Evidence / Acceptance / Memory / Changelog / TODO 更新。

## 6. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 2026-06-06 |
