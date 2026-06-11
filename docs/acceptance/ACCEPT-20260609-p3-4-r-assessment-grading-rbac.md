# ACCEPT - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. 验收结论

通过。

P3-4-R 已完成 Assessment read paths 与 GradingEvaluation HTTP path 的 roles-first RBAC 迁移，并通过 focused、adjacent、full backend Maven 验证。

## 2. 验收项

| ID | Acceptance | Status | Evidence |
|---|---|---|---|
| AC-P3-4-R-01 | `AssessmentController` 从 `UserContext.roles()` 派生 admin/teacher facts | PASS | `AssessmentController.java` |
| AC-P3-4-R-02 | Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 可读 answer detail | PASS | `answerDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader` |
| AC-P3-4-R-03 | Bearer `TEACHER sub=instructor_1` 可读 own-course answer/wrong-question/list | PASS | `wrongQuestionDetailAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse`, `answerListAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse` |
| AC-P3-4-R-04 | Bearer `USER sub=admin` 不获得 admin Assessment/Grading 权限 | PASS | `answerDetailRejectsBearerUserSubjectAdminRoleConfusion`, `answerListRejectsBearerUserSubjectAdminRoleConfusion`, `gradingEvaluationRejectsBearerUserSubjectAdminRoleConfusion` |
| AC-P3-4-R-05 | Bearer `USER sub=teacher_1` 不获得 teacher Assessment/Grading 权限 | PASS | `wrongQuestionDetailRejectsBearerUserSubjectTeacherPrefixRoleConfusion`, `gradingEvaluationRejectsBearerUserSubjectTeacherPrefixRoleConfusion` |
| AC-P3-4-R-06 | Bearer admin existing/missing grading course 语义正确 | PASS | `gradingEvaluationUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`, `gradingEvaluationBearerAdminMissingCourseReturnsNotFound` |
| AC-P3-4-R-07 | Bearer teacher no-prefix 可运行 own-course grading evaluation | PASS | `gradingEvaluationAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse` |
| AC-P3-4-R-08 | 无 API/DB/frontend/dependency drift | PASS | 文件变更审查 + full backend verification |
| AC-P3-4-R-09 | Integration review 无阻断项 | PASS | `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-integration-review.md` |

## 3. 测试结果

```text
RED: mvn --% -Dtest=AssessmentControllerTest test
Tests run: 37, Failures: 11, Errors: 0, Skipped: 0

Focused GREEN: mvn --% -Dtest=AssessmentControllerTest test
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0

Adjacent: mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
Tests run: 123, Failures: 0, Errors: 0, Skipped: 0

Full backend: mvn test
Tests run: 442, Failures: 0, Errors: 0, Skipped: 1
```

Integration review: Hegel / code-reviewer PASS.

## 4. 非目标确认

- 未修改 frontend。
- 未修改 DB migration。
- 未修改 API contract。
- 未新增依赖。
- 未改 `POST /api/assessment/answers` 提交答题写入语义。
- 未引入 formal OAuth2/JWK/Spring Security。
- 未声明 P3-4 整体完成。

## 5. 后续建议

下一切片建议处理 LearningPath / ResourceGeneration course-bound create role facts，或继续扩展 broader class/course 权限渗透矩阵。
