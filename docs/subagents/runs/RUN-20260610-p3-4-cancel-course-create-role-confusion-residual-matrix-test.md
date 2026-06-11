# RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-test

## 角色

Test Engineer

## 范围

只读测试覆盖分析：`agent task cancel` + `course create role-confusion residual matrix`。

## 现有覆盖

Agent / ResourceGeneration：

- `ResourceGenerationControllerTest.cancelsAgentTaskAndRejectsRepeatedCancellation`
  - 已覆盖 `POST /api/agent/tasks/{taskId}/cancel` owner 可取消、重复取消冲突、foreign user 拒绝。
- `AgentRunRecorderTest.cancelsRunningTaskAndAppendsCancellationTrace`
  - 已覆盖底层 RUNNING -> CANCELLED，并追加 `task_cancelled` trace。
- `AgentRunRecorderTest` 已覆盖 terminal status 不可取消。
- `AgentTraceControllerTest` 已覆盖 trace governance/search/detail 的 Bearer ADMIN、spoofed `X-User-Id`、`sub=admin + USER role` 拒绝。

Course / CourseKnowledge：

- `CourseKnowledgeControllerTest.bearerUserSubjectTeacherPrefixCannotCreateCourseForSelf`
  - 已覆盖 `sub=teacher_1 + USER role` 不能创建课程。
- `CourseKnowledgeControllerTest` 已覆盖 teacher-prefix USER 不能管理课程图谱。
- `CourseKnowledgeControllerTest` 已覆盖 `sub=admin + USER role` 不能获得课程 admin 读权限。
- `CourseAccessServiceTest` 已用反射断言 `KnowledgeCatalogService` 不保留 legacy subject-name overload。

## 缺口

- cancel 缺 Bearer roles-first MockMvc 矩阵：
  - Bearer owner + spoofed `X-User-Id`
  - Bearer `USER sub=admin`
  - Bearer `USER sub=teacher_*`
  - Bearer non-owner + spoofed owner header
- course create 缺 Bearer `USER sub=admin` create 拒绝。
- course create 可补 Bearer `TEACHER` + spoofed admin header 正向钉子。

## 推荐测试名

`ResourceGenerationControllerTest`：

- `bearerOwnerCanCancelTaskDespiteSpoofedUserIdHeader`
- `bearerUserSubjectAdminCannotCancelForeignTaskAndDoesNotMutateTask`
- `bearerUserSubjectTeacherPrefixCannotCancelForeignTaskAndDoesNotMutateTask`
- `bearerNonOwnerWithSpoofedOwnerHeaderCannotCancelTaskAndDoesNotAppendTrace`

`CourseKnowledgeControllerTest`：

- `bearerUserSubjectAdminCannotCreateCourseAndDoesNotPersistCourse`
- `bearerTeacherCreatesOwnCourseDespiteSpoofedAdminHeader`

## 推荐验证命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,AgentRunRecorderTest,CourseKnowledgeControllerTest,CourseAccessServiceTest test
```

Full：

```powershell
cd D:\多元agent\backend
mvn test
```

