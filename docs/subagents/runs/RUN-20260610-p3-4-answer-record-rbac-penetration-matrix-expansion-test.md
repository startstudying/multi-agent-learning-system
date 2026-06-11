# RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-test

## 专家角色

Test Engineer

## 范围

- `AssessmentControllerTest`
- `AssessmentServiceTest`
- `CourseAccessServiceTest`
- `AssessmentService`
- `AssessmentController`

## 结论

现有 detail/list 基础 RBAC 覆盖已经较厚；本切片应优先补 wrong-question list 的 Bearer/角色混淆矩阵，以及学生 Bearer + spoofed header 的代表路径。

## 现有覆盖摘要

已有测试覆盖：

- Student answer detail owner-only，foreign/missing 合并为 `FORBIDDEN`。
- Teacher answer detail own-course active learner 成功，foreign course 拒绝。
- Admin answer detail existing 成功，missing `NOT_FOUND`。
- Answer detail Bearer admin + spoofed header 成功。
- Answer detail Bearer `USER sub=admin` role-confusion 拒绝。
- Wrong-question detail 复用 assessment scope。
- Wrong-question detail Bearer teacher no-prefix 成功。
- Wrong-question detail Bearer `USER sub=teacher_*` role-confusion 拒绝。
- Student answer/wrong-question list owner-only。
- Teacher answer/wrong-question list own-course active enrollment scope。
- Answer list Bearer teacher no-prefix 成功。
- Answer list Bearer `USER sub=admin` role-confusion 拒绝。
- Admin answer/wrong-question list 全局/filter 与 missing course 语义。
- Pagination bounds。
- Service legacy overload/helper 反射守卫。

## 推荐新增测试

1. `wrongQuestionListAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse`
2. `wrongQuestionListRejectsBearerUserSubjectTeacherPrefixRoleConfusion`
3. `answerListAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader`
4. `answerDetailAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader`
5. `wrongQuestionDetailAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader`

结合 Security Reviewer 建议，主线实现可额外加入：

- answer list Bearer admin spoofing。
- answer detail Bearer `USER sub=teacher_*` role-confusion。
- wrong-question detail Bearer admin spoofing。
- wrong-question detail Bearer `USER sub=admin` role-confusion。
- teacher detail inactive enrollment 拒绝。

## 推荐验证

Focused：

```powershell
cd backend
mvn -Dtest=AssessmentControllerTest test
```

Adjacent：

```powershell
cd backend
mvn -Dtest=AssessmentControllerTest,AssessmentServiceTest,CourseAccessServiceTest test
```

Full：

```powershell
cd backend
mvn test
```
