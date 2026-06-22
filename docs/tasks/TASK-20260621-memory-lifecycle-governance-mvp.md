# TASK-20260621 Memory lifecycle governance MVP

## 1. 目标

完成 P2 最小切片：为 AI QA 会话记忆增加 salience、decay、用户编辑/删除和 session 表扩展，并保持 P0 隐私边界。

## 2. 范围

包含：

- V23 migration
- `KbChatSession` / `KbChatMessage` entity 扩展
- `MemoryLifecycleService`
- `AiQaMemoryController`
- `AiQaService` 写入低敏 QA memory summary
- `MemoryContextService` 消费 active session memory
- focused / adjacent tests

不包含：

- 前端记忆治理 UI
- 长期向量记忆
- 复杂冲突合并
- 新依赖
- 真实模型 token streaming

## 3. 允许修改文件

- `backend/src/main/resources/db/migration/V23__memory_lifecycle_governance.sql`
- `backend/src/main/java/com/learningos/rag/domain/KbChatSession.java`
- `backend/src/main/java/com/learningos/rag/domain/KbChatMessage.java`
- `backend/src/main/java/com/learningos/rag/repository/KbChatSessionRepository.java`
- `backend/src/main/java/com/learningos/rag/repository/KbChatMessageRepository.java`
- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/memory/MemoryLifecycleService.java`
- `backend/src/main/java/com/learningos/aiqa/application/memory/MemoryContextService.java`
- `backend/src/main/java/com/learningos/aiqa/api/AiQaMemoryController.java`
- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/test/java/com/learningos/aiqa/application/memory/MemoryLifecycleServiceTest.java`
- `backend/src/test/java/com/learningos/aiqa/application/memory/MemoryContextServiceTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- P2 workflow docs / evidence / acceptance
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`

## 4. 禁止修改文件

- `frontend/**`
- `backend/pom.xml`
- provider credentials / env files
- RAG parser/vector/index worker
- ResourceGeneration / ReviewGate 生产代码

## 5. 验收标准

- V23 migration 扩展 session/message lifecycle columns。
- QA 成功后写入低敏 memory summary。
- summary 不包含 raw question、完整 answer、teacher_note、provider key、profileSnapshot。
- edit/delete API owner-only。
- MemoryContext 排除 deleted/expired message。
- focused/adjacent/compile 通过。

## 6. Evidence

Evidence:

- RED：`mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test` 首次失败于缺少 `KbChatMessageRepository`、`MemoryLifecycleService`、chat message lifecycle 字段/getter/setter、`MemoryContextService` 新依赖与 V23 migration。
- GREEN focused：`mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test`，26 run, 0 failures, 0 errors, Build SUCCESS。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,QaRuntimeTest test`，7 run, 0 failures, 0 errors, Build SUCCESS。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。
- Privacy grep：生产命中仅为敏感标记过滤/校验规则，未发现 raw question/full answer 样例进入生产持久化实现。
- Standalone evidence：`docs/evidence/EVIDENCE-20260621-memory-lifecycle-governance-mvp.md`。

## 7. Acceptance Verdict

Verdict: PASS

- V23 扩展 `kb_chat_session` / `kb_chat_message` lifecycle columns。
- QA 回答成功后写入低敏 `AI_QA_SUMMARY` memory message。
- `contentSummary` 只保存 question hash/length、answer length、sourcePolicy、verification、citationCount。
- 用户编辑 summary 走敏感标记过滤，删除为 soft delete。
- `MemoryContextService` 优先读取未删除、未过期 session memory；无 active memory 时保留 `learning_event` fallback。
- 未新增依赖、未改前端、未保存 raw question 或完整 answer。
- Acceptance：`docs/acceptance/ACCEPT-20260621-memory-lifecycle-governance-mvp.md`。
