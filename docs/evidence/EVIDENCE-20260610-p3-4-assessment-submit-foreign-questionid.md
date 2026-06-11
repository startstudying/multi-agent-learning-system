# EVIDENCE-20260610-p3-4-assessment-submit-foreign-questionid

## 1. 任务

P3-4 子任务：Assessment submit foreign-questionId

## 2. 变更摘要

修复 `POST /api/assessment/answers` 的课程绑定题目提交授权缺陷。

专家 subagent 先新增 RED 测试：

- `AssessmentControllerTest.submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects`

RED 证明 Bearer `USER sub=alice` 可提交 foreign course 的 `questionId` 并触发答题闭环副作用。任务由 S Fast Lane 升级为 M。

生产修复：

- `AssessmentService.submitAnswerWithTraceId(...)` 在 `requestId` 基础校验之后、idempotency replay 与任何业务副作用之前调用 `requireSubmitQuestionScope(...)`。
- `requireSubmitQuestionScope(...)` 使用 `questionId -> AssessmentFeedbackService.resolveKnowledgePointId(...) -> KnowledgePointRepository.findById(...) -> KnowledgePoint.courseId -> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)` 执行 ACTIVE enrollment 校验。
- 无法解析到现有 `KnowledgePoint` 的 legacy/template `questionId` 保持兼容，不在本切片强制拒绝。

## 3. 修改文件

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/requirements/REQ-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/specs/SPEC-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/plans/PLAN-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/tasks/TASK-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/tasks/TASK-20260610-p3-4-assessment-submit-foreign-questionid-matrix.md`
- `docs/context/CONTEXT-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/subagents/runs/RUN-20260610-p3-4-assessment-submit-foreign-questionid-matrix.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### RED

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

- `Tests run: 49, Failures: 1, Errors: 0, Skipped: 0`
- 失败点：新增测试期望 foreign course `questionId` 提交返回 `403 FORBIDDEN`，实际返回 `200 OK`。

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest test
```

结果：

- `Tests run: 60, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,OrchestratorWorkflowControllerTest test
```

结果：

- `Tests run: 92, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 578, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

说明：此前一次 full backend 失败由并行 Maven 进程互踩 `backend/target` 引发 `ClassNotFoundException`；停止并行 Maven 后重跑通过，不判定为代码缺陷。

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仍委托 Service；授权前置在 `AssessmentService`。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG contract；Orchestrator 复用同一 Service 并由 adjacent test 覆盖。 |
| Security | PASS | 权限检查在后端代码中，且早于 replay / content safety / transaction / 持久化副作用。 |
| API / Database | PASS | 未改 API/DTO/schema/dependency。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| RED 测试证明真实 foreign-questionId 缺陷 | PASS |
| foreign course `questionId` 提交返回 `FORBIDDEN` 且无 `data` | PASS |
| 越权响应不泄露 foreign question/course/knowledge/request 信号 | PASS |
| 越权请求不新增 answer/grading/mastery/wrong-question/event | PASS |
| `q_sql_join` 等 legacy/template question id 兼容路径保持通过 | PASS |
| scope 校验早于 idempotency replay 和任何副作用 | PASS |
| focused / adjacent / full backend 验证完成 | PASS |

最终结论：PASS。P3-4 父项仍保持 open。
