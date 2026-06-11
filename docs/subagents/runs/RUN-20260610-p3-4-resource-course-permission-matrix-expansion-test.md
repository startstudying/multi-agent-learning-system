# RUN-20260610-p3-4-resource-course-permission-matrix-expansion-test

## 角色

Test Engineer

## 范围

只读测试覆盖分析。未修改文件，未运行测试。

## 现有覆盖清单

- `CourseKnowledgeControllerTest`：已覆盖课程创建/章节/知识点/依赖、学生禁止写、外部教师禁止写、管理员可写、课程列表按角色 scope、学生读/图谱需 ACTIVE enrollment、非管理员 missing/foreign 统一 `FORBIDDEN`、Bearer ADMIN/TEACHER roles-first、伪造 `X-User-Id` 无效、`sub=admin` / `sub=teacher_*` role-confusion 拒绝。
- `AnalyticsControllerTest`：已覆盖 overview/token-budget admin-only、student summary roles-first、class summary Bearer admin/teacher/student/subject-confusion、active enrollment only class analytics。
- `ResourceGenerationControllerTest`：已覆盖 task/detail/trace owner scope、missing/foreign anti-enumeration、Bearer admin detail、`sub=admin` detail 拒绝、course-bound create 需 ACTIVE enrollment、template goal 兼容、Bearer admin 不能给其他 learner 创建、`sub=admin` create 无副作用拒绝。
- `ResourceReviewControllerTest`：已覆盖 student 不能 list/decide、admin list/decide、Bearer admin spoof header、Bearer teacher own-course、`sub=admin` / `sub=teacher_*` 拒绝、teacher foreign/missing review anti-enumeration、teacher own-course list/decision。
- `AgentTraceControllerTest`：已覆盖 governance list admin-only、Bearer admin spoof header、`sub=admin` 拒绝、trace detail owner/admin、missing admin `NOT_FOUND`。
- `LearningWorkflowControllerTest`：已覆盖 learning path owner/admin、missing/foreign anti-enumeration、course-bound path 需 ACTIVE enrollment、Bearer admin role、`sub=admin` create 拒绝且不持久化。
- `CourseAccessServiceTest`：已覆盖移除 Course/KnowledgeCatalog legacy subject-name overload/helper，并验证 `admin`/`teacher_*` subject 名不会被当作角色。

## 最小缺口

- Resource 入口仍缺少若干 MockMvc 回归测试锁死“HTTP 不走 legacy subject 推断”。
- `learner-resources` 未明确覆盖 Bearer ADMIN 是否能绕过 learner-only release 视图。
- course-bound resource create 未明确覆盖 Bearer TEACHER 对 own-course 学生创建资源是否被拒绝且无副作用。

## 推荐测试名和断言

1. `learnerResourcesRejectsBearerAdminForForeignLearnerEvenAfterRelease`
   - Alice 创建并发布 task。
   - Bearer ADMIN `ops_admin` + spoofed `X-User-Id: alice` 请求 learner-resources。
   - 断言 `403 FORBIDDEN`，无 `data`，body 不含 `markdownContent` / `traceId`。

2. `learnerResourcesUsesBearerOwnerAndIgnoresSpoofedUserIdHeaderAfterRelease`
   - Alice task 已发布。
   - Bearer USER `sub=alice` + `X-User-Id: bob` 请求 learner-resources。
   - 断言 `200`，返回 learner-safe DTO，不返回 `traceId`、`agentTaskId`、`reviewStatus`。

3. `courseBoundResourceGenerationCreateRejectsBearerTeacherForStudentInOwnCourseWithoutSideEffects`
   - course teacher=`teacher_owner`，alice ACTIVE enrolled。
   - Bearer TEACHER `teacher_owner` 创建 learnerId=`alice` resource task。
   - 断言 `403 FORBIDDEN`，无 task/resource/review/trace/model/token 副作用。

4. `courseBoundResourceGenerationCreateAllowsBearerOwnerWithActiveEnrollmentDespiteSpoofedHeader`
   - course + alice ACTIVE enrolled。
   - Bearer USER `alice` + `X-User-Id: admin` 创建 learnerId=`alice`。
   - 断言 `200 WAITING_REVIEW`，task learnerId=`alice`、goalId=courseId。

## 推荐命令

Focused：

```powershell
cd backend
mvn --% -Dtest=ResourceGenerationControllerTest test
```

Adjacent：

```powershell
cd backend
mvn --% -Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,AgentTraceControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,CourseAccessServiceTest test
```

Full：

```powershell
cd backend
mvn test
```
