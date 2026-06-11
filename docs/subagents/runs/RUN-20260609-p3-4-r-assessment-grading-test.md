# RUN-20260609 P3-4-R Assessment / GradingEvaluation Test Design

## 范围

只读测试设计，未修改代码，未运行测试。

## 主测试文件

`backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`

需要补充 Bearer JWT fixture：

- `learning-os.auth.jwt-secret=unit-test-secret`
- `learning-os.auth.issuer=learning-os`
- `jwt(sub, name, roles)` HS256 helper

## 推荐 RED 矩阵

### Assessment answer / wrong-question read paths

- `answerDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `answerDetailRejectsBearerUserSubjectAdminRoleConfusion`
- `wrongQuestionDetailAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse`
- `wrongQuestionDetailRejectsBearerUserSubjectTeacherPrefixRoleConfusion`
- `answerListAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse`
- `answerListRejectsBearerUserSubjectAdminRoleConfusion`

### GradingEvaluation HTTP path

- `gradingEvaluationUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `gradingEvaluationAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse`
- `gradingEvaluationRejectsBearerUserSubjectAdminRoleConfusion`
- `gradingEvaluationRejectsBearerUserSubjectTeacherPrefixRoleConfusion`

## 预期 RED

当前代码使用 `currentUserId` 字符串推断角色：

- Bearer admin/teacher no-prefix 正常授权场景会失败为 `403`。
- `USER sub=admin` / `USER sub=teacher_1` 会错误通过，测试期望 `403`。

## 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```
