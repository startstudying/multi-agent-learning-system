# RUN - P3-4-R Assessment / GradingEvaluation Integration Review

## 1. Reviewer

- Agent: Hegel
- Role: code-reviewer / integration reviewer
- Date: 2026-06-09

## 2. Conclusion

PASS。

未发现阻断项。基于当前工作区文件内容，P3-4-R 的 roles-first RBAC 迁移符合目标。

## 3. Key Evidence

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
  - `list/detail/grading` 主路径均读取 `currentUserService.currentUser()`。
  - admin / teacher facts 仅从 `UserContext.roles()` 派生。
  - 未调用 `currentUserService.isAdmin()` / `isTeacherUser()`。
  - `submit` 仍用 `currentUserId()`，不属于本切片 read/grading 迁移范围。

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
  - `listAnswers/listWrongQuestions/answerDetail/wrongQuestionDetail` 已新增 explicit role facts overload。
  - HTTP 主路径可传入 `currentUserAdmin/currentUserTeacher`。
  - legacy overload 仍保留并委托旧 subject 推断。
  - list/detail 授权路径调用 role-aware `CourseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)`。

- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
  - HTTP overload 使用 explicit role facts。
  - 普通用户先 `FORBIDDEN`，再校验 `courseId`。
  - 调用 role-aware `CourseAccessService.requireCourseRead(...)`。
  - legacy `evaluate(String currentUserId, request)` 仍兼容保留。

- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
  - 覆盖 Bearer admin spoof、`USER sub=admin`、teacher no-prefix、`USER sub=teacher_1` role-confusion。
  - 覆盖 admin/teacher missing/foreign 语义。
  - `surefire-reports` 显示 `AssessmentControllerTest`: 37 run, 0 failures。

## 4. Drift Check

- 未看到 API path / DTO / DB migration / frontend / dependency drift。
- `backend/src/main/resources/db/migration` 当前到 `V19`，未见本切片新增迁移。
- `backend/pom.xml`、`frontend/**` 不在本切片允许修改范围内。

## 5. Risks / Notes

- 工作区没有 `.git` 元数据，`git diff` 无法使用；本次审查基于当前态文件和测试报告，不是基于精确 diff。
- 当前可用工具里没有 `lsp_diagnostics` / `ast_grep_search`，评审使用窄范围 `rg` 替代扫描。
- 未发现调试输出或生产密钥。唯一命中是测试属性 `learning-os.auth.jwt-secret=unit-test-secret`，属于测试 JWT fixture。

