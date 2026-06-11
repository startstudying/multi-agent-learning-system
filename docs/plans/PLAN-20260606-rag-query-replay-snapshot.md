# RAG 查询重放与响应快照计划

## 1. 追踪

- PRD：`docs/product/PRD-20260606-rag-query-replay-snapshot.md`
- REQ：`docs/requirements/REQ-20260606-rag-query-replay-snapshot.md`
- SPEC：`docs/specs/SPEC-20260606-rag-query-replay-snapshot.md`

## 2. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 本任务是 P0 后端功能切片，必须走文档、测试、证据、验收和记忆更新。 |
| `ai-learning-agent-development` | 任务属于 AI 学习系统的 RAG + Orchestrator 闭环。 |
| `educational-rag-pipeline` | 涉及 RAG query log、citation、no-source 和权限过滤。 |
| `spring-ai-agent-backend` | 涉及 Spring Boot Service、Repository、DTO 和事务边界。 |
| `agent-trace-governance` | 涉及 workflow、traceId、query log、citation 的审计一致性。 |
| `test-driven-development` | 行为变更必须先写 RED 测试。 |
| `verification-before-completion` | 完成声明前必须运行聚焦和全量验证。 |

Missing skills：无。

GitHub Research Needed：No。本切片复用项目内已完成的答题提交幂等模式，不新增外部依赖或框架。

New Project-Specific Skill To Create：No。

## 3. Subagent Decision

Use Subagents：Yes。

Reason：用户明确要求多 subagent 并行开发，且本任务涉及 RAG、Orchestrator、数据库、权限和测试。

Parallelism Level：L1。

Selected Subagents：

- 架构审查：确认服务边界、数据流和非目标。
- 测试审查：设计 RED 测试和回归命令。
- 安全审查：确认 replay 权限、payload hash 和快照隐私边界。

Implementation Mode：Single Codex implementation with parallel analysis。

## 4. Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementation | PASS | RAG query log 已存在，但没有 `requestId/requestHash/responseJson`。 |
| Architecture compliance | PASS | 复用 `AssessmentService` 幂等模式，修改集中在 RAG Service、Orchestrator Service、迁移和测试。 |
| Official docs verified | N/A | 不新增 SDK/API/依赖。 |
| OSS references | N/A | 不需要 GitHub research。 |
| Root cause identified | PASS | 重复 RAG_QA 当前会重新创建 task/query/citation，缺少 replay 和唯一索引兜底。 |

Confidence：0.93。可进入 TDD。

## 5. 实施阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 创建中文流程文档和 Context Pack | 已完成 |
| 2 | 编写 RED 测试并确认失败 | 已完成 |
| 3 | 增加 V7 迁移、实体字段、Repository 方法 | 已完成 |
| 4 | 实现 `RagQueryService` 幂等查询和快照 | 已完成 |
| 5 | 实现 Orchestrator RAG_QA replay / conflict | 已完成 |
| 6 | 聚焦回归和全量测试 | 已完成 |
| 7 | 更新 Evidence / Acceptance / Memory / Changelog / TODO | 已完成 |

## 6. 文件变更清单

| 文件 | 操作 |
|---|---|
| `backend/src/main/java/com/learningos/rag/application/RagQueryService.java` | 修改 |
| `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java` | 修改 |
| `backend/src/main/java/com/learningos/rag/repository/KbQueryLogRepository.java` | 修改 |
| `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java` | 修改 |
| `backend/src/main/java/com/learningos/rag/api/ChatController.java` | 修改 |
| `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` | 修改 |
| `backend/src/main/resources/db/migration/V7__rag_query_replay_snapshot.sql` | 新增 |
| `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java` | 修改 |
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 修改 |
| `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java` | 修改 |
| 本任务对应 `docs/**` | 新增/更新 |

## 7. 测试命令

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest" test
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test
mvn test
```
