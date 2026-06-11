# ACCEPT - P3-4-Q Analytics student summary roles-first RBAC

## 1. 验收结论

通过。

P3-4-Q 已完成 Analytics student summary course scope 的 roles-first `CourseAccessService` 补口，并通过 focused、adjacent、full backend Maven 验证。

## 2. 验收项

| ID | Acceptance | Status | Evidence |
|---|---|---|---|
| AC-P3-4-Q-01 | Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 可读取已有课程下学生摘要 | PASS | `AnalyticsControllerTest` focused 34/34 |
| AC-P3-4-Q-02 | Bearer admin missing course 保持 `NOT_FOUND` | PASS | `studentSummaryBearerAdminMissingCourseRemainsNotFound` |
| AC-P3-4-Q-03 | Bearer `TEACHER sub=instructor_1` 可读取 own-course active learner summary，不依赖 `teacher_` 前缀 | PASS | `bearerTeacherCanReadCourseScopedStudentSummaryForOwnCourseWithoutTeacherIdPrefix` |
| AC-P3-4-Q-04 | Bearer `USER sub=teacher_1` 不通过 subject 前缀获得 teacher 权限 | PASS | `bearerUserSubjectTeacherPrefixCannotReadCourseScopedStudentSummaryAsTeacher` |
| AC-P3-4-Q-05 | Teacher missing/foreign course 均为 safe `FORBIDDEN` 且无 `data` | PASS | `bearerTeacherCannotDistinguishMissingCourseFromForbiddenStudentSummaryCourse` |
| AC-P3-4-Q-06 | 无 API/DB/frontend/dependency drift | PASS | 文件变更仅限 Context Pack 允许范围 |
| AC-P3-4-Q-07 | Full backend verification 通过 | PASS | `mvn test`: 431 run, 0 failures, 0 errors, 1 skipped |

## 3. 测试结果

```text
RED: mvn --% -Dtest=AnalyticsControllerTest test
Tests run: 34, Failures: 3, Errors: 0, Skipped: 0

Focused GREEN: mvn --% -Dtest=AnalyticsControllerTest test
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0

Adjacent: mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
Tests run: 68, Failures: 0, Errors: 0, Skipped: 0

Full backend: mvn test
Tests run: 431, Failures: 0, Errors: 0, Skipped: 1
```

## 4. 非目标确认

- 未修改 frontend。
- 未修改 DB migration。
- 未修改 API contract。
- 未新增依赖。
- 未处理 Assessment / GradingEvaluation / LearningPath / ResourceGeneration legacy caller。
- 未声明 P3-4 整体完成。

## 5. 后续建议

下一切片建议 P3-4-R：Assessment / GradingEvaluation legacy role inference 迁移到 roles-first 权限矩阵。
