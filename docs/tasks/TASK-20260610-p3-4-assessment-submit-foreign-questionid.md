# TASK-20260610-p3-4-assessment-submit-foreign-questionid

## 1. 任务名称

P3-4 子任务：Assessment submit foreign-questionId

## 2. 目标

修复 `POST /api/assessment/answers` 允许学生提交 foreign course `questionId` 的安全缺陷，确保课程绑定题目提交前执行 ACTIVE enrollment 授权，并保持 legacy/template question id 兼容。

## 3. 当前状态

专家 subagent 已新增 RED 测试：

- `AssessmentControllerTest.submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects`

RED 结果：

```text
Tests run: 49, Failures: 1, Errors: 0, Skipped: 0
Expected: 403 FORBIDDEN
Actual:   200 OK
```

## 4. 允许修改文件

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`（如需要）
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

## 5. 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- 非 assessment submit / 文档闭环相关文件

## 6. 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,OrchestratorWorkflowControllerTest test
```

Full：

```powershell
cd D:\多元agent\backend
mvn test
```

## 7. Acceptance Criteria

- RED 测试转 GREEN。
- 越权 foreign `questionId` 提交返回 `FORBIDDEN` 且无 `data`。
- 越权响应不泄露 foreign `questionId`、course id、知识点标题、requestId。
- 越权请求不新增 answer/grading/mastery/wrong-question/event。
- 既有 legacy/template `q_sql_join` 提交测试仍通过。
- Evidence、Acceptance、Changelog、Memory、TODO 更新完成。
