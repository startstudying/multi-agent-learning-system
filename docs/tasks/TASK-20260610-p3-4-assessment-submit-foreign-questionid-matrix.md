# TASK-20260610 P3-4 子任务：Assessment submit foreign-questionId matrix

## 目标

为 `POST /api/assessment/answers` 补一个最小 HTTP 权限回归，验证学生提交答题时不能通过伪造 foreign course 的 `questionId` 为自己生成答题、评分、掌握度、错题或学习事件记录。

## Size Classification

S Fast Lane。

- 单一后端 HTTP 测试切片。
- 不改 REST API path / DTO / DB schema / dependency / frontend / Evaluation / Review / SSE。
- 先以测试验证边界；若 RED 暴露生产缺陷，则停止并升级 M。

## Context Pack

### 相关记忆与规则

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`

### 允许修改

- `backend/src/test/java/com/learningos/assessment/api/AssessmentAnswerControllerTest.java`，若存在。
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`，作为当前最贴近 assessment HTTP 测试文件。
- `docs/tasks/TASK-20260610-p3-4-assessment-submit-foreign-questionid-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-assessment-submit-foreign-questionid-matrix.md`

### 禁止修改

- 生产代码。
- `docs/changelog/`、`docs/memory/`、`docs/planning/`、`docs/evidence/`。
- Evaluation / Review、dev/test fallback、frontend SSE 相关文件。

## 测试设计

新增 `submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects`：

- 准备 `alice` ACTIVE enrollment 的 own course。
- 准备 `bob` ACTIVE enrollment 的 foreign course，并取得该 course 下的 `questionId`。
- 使用 Bearer `USER sub=alice`，同时伪造 `X-User-Id: teacher_b`。
- 请求体 `learnerId=alice`，`questionId=<foreignQuestionId>`。
- 期望 `FORBIDDEN`、无 `data`、响应不泄露 foreign `questionId` / course id / title / requestId。
- 期望 `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event` 均无新增。

## 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

## Acceptance

- 若测试通过：本切片可作为 test-only S 子任务完成。
- 若测试 RED：说明 `POST /api/assessment/answers` 当前未对 course-bound `questionId` 执行 active enrollment 授权，应停止本 S 切片并升级 M，补 service 层生产代码与 evidence。
