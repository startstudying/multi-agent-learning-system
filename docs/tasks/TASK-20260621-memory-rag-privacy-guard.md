# TASK-20260621 Memory/RAG Privacy Guard

## 1. Goal

完成 `PLAN-20260621-memory-answer-quality-execution-readable.md` 的 P0-1：先对 RAG 与画像快照的新写入数据做隐私守门，防止 raw question、full excerpt、`teacher_note` 和完整画像快照沉淀到长期数据或模型上下文。

## 2. Task Type

安全/隐私增强 + RAG 持久化策略调整 + 画像上下文最小化。

## 3. Skill Selection

- `feature-development-workflow`：项目强制开发流程。
- `educational-rag-pipeline`：RAG query log、citation 与检索链路。
- `agent-trace-governance`：durable trace/replay 数据必须脱敏。
- `spring-ai-agent-backend`：资源生成模型上下文由后端 Service 负责。
- `security-review`：隐私字段、prompt/context 敏感数据边界。
- `test-driven-development`：先补 RED 回归，再实现。
- `verification-before-completion`：完成前必须记录实际测试证据。

GitHub research：不需要。本切片使用项目既有 Spring Boot/Jackson/JPA 能力，不新增依赖。

## 4. Size Classification

M。

理由：跨 `common`、`rag`、`learning`、`agent` 四个后端包，触及隐私和 RAG replay 行为；但不改 REST 合同、不改 DB schema、不加依赖、不引入新 Agent/RAG 编排。

## 5. Subagent Decision

不启用 subagent。当前要求是执行已有落地计划的第一个切片，范围可由主线程直接集成；且本会话未收到用户显式要求创建子代理。

## 6. Implementation Checklist

- [x] 添加 `MemoryPrivacyPolicy`。
- [x] 给 RAG 写入路径补 RED 测试：query log、responseJson、source citation 均不含 raw question/full excerpt。
- [x] 给 profile snapshot 补 RED 测试：学习路径与资源生成快照不含 `learnerId` / `teacher_note`。
- [x] 实现 RAG 持久化脱敏和 replay snapshot 降级。
- [x] 实现 profile snapshot 最小化，并确保 ResourceAgent 模型上下文复用低敏快照。
- [x] 运行 focused/adjacent tests。
- [x] 创建 Evidence/Acceptance。
- [x] 更新 memory/changelog/落地计划进度。

## 7. Allowed Files

- `backend/src/main/java/com/learningos/common/privacy/MemoryPrivacyPolicy.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `docs/specs/SPEC-20260621-memory-rag-privacy-guard.md`
- `docs/tasks/TASK-20260621-memory-rag-privacy-guard.md`
- `docs/context/CONTEXT-20260621-memory-rag-privacy-guard.md`
- `docs/evidence/EVIDENCE-20260621-memory-rag-privacy-guard.md`
- `docs/acceptance/ACCEPT-20260621-memory-rag-privacy-guard.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`

## 8. Disallowed Files

- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `pom.xml`
- 环境变量、密钥、依赖锁文件。
- 与 P0-1 无关的历史脏工作区文件。

## 9. Test Commands

```powershell
cd backend; mvn --% -Dtest=RagQueryServiceTest test
cd backend; mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
```

如 focused tests 发现相邻测试风险，再扩大到：

```powershell
cd backend; mvn --% -Dtest=RagQueryServiceTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest test
```

## 10. Acceptance Criteria

- RAG 新 query log 不保存 raw question。
- RAG 新 response snapshot 不保存 raw question 或完整 source excerpt。
- 新 source citation record 不保存完整 excerpt。
- 新 `profileSnapshot` 不保存 `learnerId` 或 `teacher_note`，保存 `profileRef`。
- `ResourceAgent` 模型上下文中的 `profileSnapshot` 不含 `teacher_note`。
- replay 降级行为已记录在 SPEC/Evidence/Acceptance。

## 11. Evidence

RED：

- `mvn --% -Dtest=RagQueryServiceTest#durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt,noSourceReplaySnapshotDoesNotStoreRawQuestion test` 失败，证明 `kb_query_log.question` 仍保存 raw question。
- `mvn --% -Dtest=LearningWorkflowControllerTest#extractsProfileDraftAndGeneratesTraceableLearningPath,ResourceGenerationControllerTest#resourceGenerationSnapshotUsesPersistedLearnerProfile test` 失败，证明 `profileSnapshot` 仍保存 `learnerId` 和 `teacher_note`。

GREEN：

- `mvn --% -Dtest=RagQueryServiceTest test`：22 run, 0 failures, 0 errors。
- `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test`：54 run, 0 failures, 0 errors。
- `mvn --% -Dtest=OrchestratorWorkflowControllerTest test`：33 run, 0 failures, 0 errors。

未运行完整 `mvn test`；本 M-size 切片按风险运行 focused + adjacent backend tests。

## 12. Acceptance Verdict

PASS。

验收结论：

- RAG 新 query log 保存 `questionHash/questionLength`，不保存 raw question。
- RAG requestId replay snapshot 保存 `RAG_REPLAY_REDACTED` 降级响应，不保存完整 answer/source excerpt。
- `source_citation.excerpt` 保存 citation reference/hash/length，不保存完整 excerpt。
- 学习路径与资源生成 `profileSnapshot` 保存 `profileRef`，不保存 raw `learnerId` 或 `teacher_note`。
- `ResourceAgent` 模型请求参数中的 `profileSnapshot` 已由测试断言不含 `teacher_note`。
