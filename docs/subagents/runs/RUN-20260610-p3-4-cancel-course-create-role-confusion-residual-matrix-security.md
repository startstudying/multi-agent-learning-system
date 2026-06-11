# RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-security

## 角色

Security & Quality

## 范围

只读分析 P3-4 残余权限矩阵：`agent task cancel` + `course create role-confusion`。

## 结论

生产代码未见明显 role-confusion 提权路径，但测试矩阵仍有缺口。建议作为 S 切片补测试优先；若 RED 暴露再升级修生产代码。

## Agent Task Cancel 授权路径

- HTTP endpoint：`backend/src/main/java/com/learningos/agent/api/AgentTraceController.java`
- `POST /api/agent/tasks/{taskId}/cancel`
- Controller 当前传 `currentUserService.currentUserId()`。
- Service 路径：
  - `ResourceGenerationService.cancelAgentTask(userId, taskId, reason)`
  - `AgentRunRecorder.cancelTask(ownerUserId, agentTaskId, reason)`
- 授权判断：`AgentRunRecorder.cancelTask` 只允许 `ownerUserId.equals(agentTask.ownerUserId())`。
- 当前语义是 owner-only cancel，没有 admin / teacher cancel 特权。

现有覆盖：

- `ResourceGenerationControllerTest.cancelsAgentTaskAndRejectsRepeatedCancellation`
- 已覆盖 owner cancel、重复 cancel `409`、foreign `X-User-Id=bob` 拒绝。

缺口：

- Bearer owner + spoofed `X-User-Id`
- Bearer `USER sub=admin`
- Bearer `USER sub=teacher_*`
- Bearer non-owner + spoofed owner header
- 拒绝后 task status / trace 数量不变

## Course Create 授权路径

- HTTP endpoint：`backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `POST /api/courses`
- Controller 传入 `currentUser.userId()`、`hasRole(ADMIN)`、`hasRole(TEACHER)`。
- Service 路径：`KnowledgeCatalogService.createCourse(...)` -> `resolveCourseTeacherId(...)`。
- explicit `ADMIN` 可指定 `teacherId`。
- explicit `TEACHER` 只能创建自己的课程。
- 非 admin/teacher 在 save 前拒绝。

现有覆盖：

- `CourseKnowledgeControllerTest.bearerUserSubjectTeacherPrefixCannotCreateCourseForSelf`
- 已覆盖 Bearer `USER sub=teacher_1` 不能通过 subject 前缀创建课程。

缺口：

- Bearer `USER sub=admin` 不能创建课程。
- Bearer `TEACHER` + spoofed `X-User-Id=admin` 创建时 `teacherId` 仍为 token subject。
- 拒绝后 course 未新增的显式副作用断言。

## 推荐最小测试

`ResourceGenerationControllerTest`：

- `bearerOwnerCanCancelTaskDespiteSpoofedUserIdHeader`
- `bearerUserSubjectAdminCannotCancelForeignTaskAndDoesNotMutateTask`
- `bearerUserSubjectTeacherPrefixCannotCancelForeignTaskAndDoesNotMutateTask`
- `bearerNonOwnerWithSpoofedOwnerHeaderCannotCancelTaskAndDoesNotAppendTrace`

`CourseKnowledgeControllerTest`：

- `bearerUserSubjectAdminCannotCreateCourseAndDoesNotPersistCourse`
- `bearerTeacherCreatesOwnCourseDespiteSpoofedAdminHeader`

## RED 升级条件

- `USER sub=admin` 或 `USER sub=teacher_*` 获得 create/cancel 能力。
- Bearer 存在时 `X-User-Id` 覆盖 token subject。
- cancel 拒绝后任务状态变为 `CANCELLED` 或追加 `task_cancelled` trace。
- 如果产品要求 cancel missing/foreign 也防枚举，则另起 M 切片明确语义。

