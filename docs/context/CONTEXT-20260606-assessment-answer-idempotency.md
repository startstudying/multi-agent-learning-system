# 答题提交幂等 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/domain/AnswerRecord.java`
- `backend/src/main/java/com/learningos/assessment/dto/AnswerSubmitRequest.java`
- `backend/src/main/java/com/learningos/assessment/repository/AnswerRecordRepository.java`
- `backend/src/main/resources/db/migration/V5__assessment_answer_idempotency.sql`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/subagents/runs/RUN-20260606-assessment-answer-idempotency.md`

## 不允许修改

- 前端代码。
- RAG、agent、analytics 主业务代码。
- 依赖配置。
- 现有迁移文件内容。

## 架构约束

- Controller 不增加业务逻辑。
- 幂等判断、hash、响应快照和重放逻辑在 Service 层。
- 数据库唯一约束作为最终兜底。
- 不新增外部依赖。
