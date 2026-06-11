# P3-4-W CourseAccessService legacy overload cleanup / caller audit RED 测试矩阵

## 任务边界

- 角色：Test Engineer，只读分析；不改源码、不改测试。
- 目标：为清理 `CourseAccessService` legacy overload 与真实调用残留设计最小 TDD RED 测试集合。
- 相关技能：`object-scope-authorization`、`auth-context-boundary`、`test-generator`。
- GitHub 研究：不需要。该任务是项目内 RBAC 调用链收口与回归测试设计。

## 代码证据

### CourseAccessService 当前状态

`backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java` 仍存在 3 个 public legacy overload：

- `requireCourseRead(String currentUserId, String courseId)`：内部通过 `isAdmin(currentUserId)` / `isTeacherUser(currentUserId)` 推断角色。
- `requireCourseManage(String currentUserId, Course course)`：同样通过 subject-name 推断角色。
- `requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)`：同样通过 subject-name 推断 admin。
- `listCoursesForUser(String currentUserId)`：同样通过 subject-name 推断角色。

roles-first overload 已存在：

- `requireCourseRead(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId)`
- `requireCourseManage(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, Course course)`
- `requireLearnerEnrolledForExistingCourse(String currentUserId, boolean currentUserAdmin, String learnerId, String courseId)`
- `listCoursesForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher)`

### 真实调用方审计

生产代码中直接调用 `CourseAccessService` 的主要路径：

- `KnowledgeCatalogService`
  - HTTP 主路径通过 `CourseController` / `KnowledgePointController` 传入 roles-first facts。
  - 但服务本身仍保留 legacy public overload 和 legacy private helper/fallback，后续 cleanup 删除 overload 时会暴露编译残留。
- `AnalyticsService`
  - student summary course scope 调用 roles-first `requireCourseRead(...)`。
  - class learner scope 使用 `listActiveLearnerIds(...)`，不涉及 legacy role inference。
- `AssessmentService`
  - answer/wrong-question list/detail teacher/admin course scope 均调用 roles-first `requireCourseRead(...)`。
- `GradingEvaluationService`
  - HTTP evaluation path 调用 roles-first `requireCourseRead(...)`。
- `DocumentService`
  - upload course metadata scope 调用 roles-first `requireCourseRead(...)` 和 `requireCourseManage(...)`。
- `LearningWorkflowService`
  - course-bound path create 调用 roles-first `requireLearnerEnrolledForExistingCourse(...)`。
- `ResourceGenerationService`
  - direct / orchestrated resource generation course-bound create 调用 roles-first `requireLearnerEnrolledForExistingCourse(...)`。

实际存在的相关测试文件：

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`

未发现单独的 `CourseControllerTest`、`KnowledgePointControllerTest`、`GradingEvaluationControllerTest` 文件；对应覆盖在 `CourseKnowledgeControllerTest` 和 `AssessmentControllerTest`。

## 最小 RED 测试集合

### P0：必须新增或保留为 focused RED 的测试

| 优先级 | 测试类 | 建议方法名 | 输入身份 | 请求/输入 | 预期 status / errorCode | 目的 |
|---|---|---|---|---|---|---|
| P0 | `CourseKnowledgeControllerTest` | `courseCreateRejectsBearerUserSubjectTeacherPrefixRoleConfusion` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/courses`，`teacherId=teacher_1` | `403 / FORBIDDEN` | 覆盖 `KnowledgeCatalogService.createCourse(...)` legacy subject-name teacher 推断残留；当前已有 manage/detail 反提权，但 create course 入口更直接。 |
| P0 | `CourseKnowledgeControllerTest` | `knowledgePointCreateRejectsBearerUserSubjectTeacherPrefixRoleConfusion` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/knowledge-points`，目标 course.teacherId=`teacher_1` | `403 / FORBIDDEN` | 覆盖 `createKnowledgePoint(...) -> requireCourseManageAccess(...)`，防止 legacy helper 或 fallback 通过 `teacher_` subject 提权。 |
| P0 | `CourseKnowledgeControllerTest` | `knowledgeDependencyCreateRejectsBearerUserSubjectTeacherPrefixRoleConfusion` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/knowledge-dependencies`，同课程两个 KP，course.teacherId=`teacher_1` | `403 / FORBIDDEN` | 覆盖 `createDependency(...) -> courseForDependency(...) -> requireCourseManageAccess(...)`。 |
| P0 | `CourseKnowledgeControllerTest` | `courseListRejectsBearerUserSubjectAdminRoleConfusion` | Bearer `sub=admin`, roles=`USER` | `GET /api/courses`，预置其他教师课程且无 active enrollment | `200 / OK`，`data.length()==0` | 覆盖 `listCoursesForUser(...)` 不得因 subject=`admin` 返回全量课程。列表接口不应用 403，应断言空列表/不含外部课程。 |
| P0 | `LearningWorkflowControllerTest` | `courseBoundLearningPathCreateRejectsBearerUserSubjectTeacherPrefixWithoutEnrollment` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/learning-paths`，`learnerId=teacher_1`，`goalId` 为 existing course，未 enrollment | `403 / FORBIDDEN` | 覆盖 `requireLearnerEnrolledForExistingCourse(...)` legacy teacher/admin 推断不会绕过 enrollment。 |
| P0 | `ResourceGenerationControllerTest` | `courseBoundResourceGenerationCreateRejectsBearerUserSubjectTeacherPrefixBeforeSideEffects` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/resources/generation-tasks`，`learnerId=teacher_1`，existing course，未 enrollment | `403 / FORBIDDEN`，无 task/resource/review/trace/model/token side effects | 与 LearningPath 同一 CourseAccessService overload，但资源生成 blast radius 更高，需保留副作用断言。 |
| P0 | `OrchestratorWorkflowControllerTest` | `resourceGenerationWorkflowRejectsBearerUserSubjectTeacherPrefixBeforeResourceSideEffects` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/orchestrator/workflows`，`RESOURCE_GENERATION` payload existing course，未 enrollment | `403 / FORBIDDEN`，无 ResourceGeneration business side effects；允许安全 failed workflow evidence 的现有约定保持 | 覆盖 orchestrator caller audit，防止 workflow retry/create 重新走 legacy enrollment bypass。 |
| P0 | `DocumentControllerTest` | `uploadRejectsBearerUserSubjectTeacherPrefixCourseMetadataSpoofing` | Bearer `sub=teacher_1`, roles=`USER` | `POST /api/knowledge-bases/{kbId}/documents`，`courseId` 的 `teacherId=teacher_1` | `403 / FORBIDDEN` | 覆盖 `requireCourseRead/Manage` 双调用，不允许普通 USER 通过 subject=`teacher_1` 写课程 metadata。 |

### P1：已有覆盖可作为 cleanup adjacent 回归

| 测试类 | 已有/建议方法 | 输入身份 | 预期 | 说明 |
|---|---|---|---|---|
| `CourseKnowledgeControllerTest` | `courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader` | Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id=alice` | `200 / OK`，返回全量 | 确认 roles-first admin 正向路径。 |
| `CourseKnowledgeControllerTest` | `courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader` | Bearer `ADMIN sub=ops_admin` + spoofed student header | `200 / OK` | 覆盖 course read + graph read。 |
| `CourseKnowledgeControllerTest` | `bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph` | Bearer `ADMIN sub=ops_admin` | `404 / NOT_FOUND` | 覆盖 admin missing semantics。 |
| `CourseKnowledgeControllerTest` | `bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix` | Bearer `TEACHER sub=instructor_1` | `200 / OK` | 确认 teacher 不依赖 `teacher_` prefix。 |
| `CourseKnowledgeControllerTest` | `bearerStudentRoleWithSpoofedAdminHeaderCannotReadOrManageForeignCourse` | Bearer `STUDENT sub=alice` + `X-User-Id=admin` | `403 / FORBIDDEN` | 确认 Bearer 覆盖 spoofed header。 |
| `CourseKnowledgeControllerTest` | `bearerUserSubjectAdminDoesNotGainCourseAdminAccess` | Bearer `USER sub=admin` | `403 / FORBIDDEN` | 已覆盖 read/graph subject-admin confusion。 |
| `CourseKnowledgeControllerTest` | `bearerUserSubjectTeacherPrefixDoesNotGainTeacherManageAccess` | Bearer `USER sub=teacher_1` | `403 / FORBIDDEN` | 已覆盖 chapter manage；P0 仍建议补 createCourse/KP/dependency。 |
| `AnalyticsControllerTest` | `studentSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader` | Bearer `ADMIN sub=ops_admin` + spoofed header | `200 / OK` | 覆盖 analytics course-scoped admin read。 |
| `AnalyticsControllerTest` | `bearerTeacherCanReadCourseScopedStudentSummaryForOwnCourseWithoutTeacherIdPrefix` | Bearer `TEACHER sub=instructor_1` | `200 / OK` | 覆盖 analytics teacher no-prefix。 |
| `AnalyticsControllerTest` | `bearerUserSubjectTeacherPrefixCannotReadCourseScopedStudentSummaryAsTeacher` | Bearer `USER sub=teacher_1` | `403 / FORBIDDEN` | 覆盖 analytics subject-name denial。 |
| `AssessmentControllerTest` | `gradingEvaluationAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse` | Bearer `TEACHER sub=instructor_1` | `200 / OK` | 覆盖 GradingEvaluationService roles-first course read。 |
| `AssessmentControllerTest` | `gradingEvaluationRejectsBearerUserSubjectAdminRoleConfusion` | Bearer `USER sub=admin` | `403 / FORBIDDEN` | 覆盖 grading subject-admin denial。 |
| `AssessmentControllerTest` | `gradingEvaluationRejectsBearerUserSubjectTeacherPrefixRoleConfusion` | Bearer `USER sub=teacher_1` | `403 / FORBIDDEN` | 覆盖 grading subject-teacher denial。 |
| `LearningWorkflowControllerTest` | `courseBoundLearningPathCreateUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader` | Bearer `ADMIN sub=ops_admin` + spoofed student header | `200 / OK` | 覆盖 learning path admin enrollment bypass 的 explicit role。 |
| `LearningWorkflowControllerTest` | `courseBoundLearningPathCreateRejectsBearerUserSubjectAdminRoleConfusionWithoutPersisting` | Bearer `USER sub=admin` | `403 / FORBIDDEN` | 覆盖 enrollment legacy admin bypass denial。 |
| `ResourceGenerationControllerTest` | `courseBoundResourceGenerationCreateRejectsBearerUserSubjectAdminRoleConfusionBeforeSideEffects` | Bearer `USER sub=admin` | `403 / FORBIDDEN` | 覆盖 resource generation direct create admin subject denial。 |
| `OrchestratorWorkflowControllerTest` | `resourceGenerationWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeResourceSideEffects` | Bearer `USER sub=admin` | `403 / FORBIDDEN` | 覆盖 orchestrator create admin subject denial。 |
| `DocumentControllerTest` | `documentUploadRejectsBearerUserSubjectTeacherPrefixRoleConfusion`（实际命名需以文件内现有方法为准） | Bearer `USER sub=teacher_1` | `403 / FORBIDDEN` | 已有相关行命中；作为 RAG document caller audit adjacent。 |

## TDD RED 执行建议

1. 先只添加 P0 测试，不改生产代码，运行 focused 命令。预期：如果仍有 legacy caller 或 fallback 参与 HTTP path，新增测试应失败；如果当前主路径已全部 roles-first，则测试可能已绿，此时 RED 可由下一步“删除 legacy overload 后编译失败”暴露残留调用点。
2. 删除 `CourseAccessService` 的 public legacy overload 前，先运行 `rg` 审计确认真实 production call 不再使用 2/3 参数 legacy 签名。
3. 删除 legacy overload 后先跑 `mvn -q -DskipTests compile`。如果 `KnowledgeCatalogService` 或测试 mock 仍调用 legacy 签名，编译失败就是本 cleanup 的 RED 反馈。
4. 最小实现只应改调用方签名与测试 mock，不改变 API contract、DTO、DB schema 或权限语义。

## Maven 命令

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest#courseCreateRejectsBearerUserSubjectTeacherPrefixRoleConfusion+knowledgePointCreateRejectsBearerUserSubjectTeacherPrefixRoleConfusion+knowledgeDependencyCreateRejectsBearerUserSubjectTeacherPrefixRoleConfusion+courseListRejectsBearerUserSubjectAdminRoleConfusion,LearningWorkflowControllerTest#courseBoundLearningPathCreateRejectsBearerUserSubjectTeacherPrefixWithoutEnrollment,ResourceGenerationControllerTest#courseBoundResourceGenerationCreateRejectsBearerUserSubjectTeacherPrefixBeforeSideEffects,OrchestratorWorkflowControllerTest#resourceGenerationWorkflowRejectsBearerUserSubjectTeacherPrefixBeforeResourceSideEffects,DocumentControllerTest#uploadRejectsBearerUserSubjectTeacherPrefixCourseMetadataSpoofing test
```

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,DocumentControllerTest test
```

### Compile guard after legacy overload removal

```powershell
cd D:\多元agent\backend
mvn --% -DskipTests compile
```

### Full

```powershell
cd D:\多元agent\backend
mvn test
```

## 避免脆弱测试和过度 mock 的建议

- 优先复用现有 `@SpringBootTest + MockMvc + H2 repository fixture` 风格，不新增只验证 mock 调用次数的 controller 单测。
- 不要断言内部 service 方法被调用哪个 overload；通过 HTTP 身份、真实课程归属、enrollment、持久化副作用来验证行为。
- 对 forbidden response 断言 `status`、`$.code`、`$.data` 不存在，以及 body 不包含 courseId / learnerId / requestId；不要断言完整 message 文案。
- 对资源生成和 orchestrator forbidden create，保留已有副作用断言：无 `ResourceGenerationTask`、无 generated resource、无 review、无 model/token/citation；orchestrator 已约定可留下安全 failed workflow evidence 时，不要误断言完全无 `agent_task`。
- 列表类接口不要把“拒绝提权”写成 403；例如 `GET /api/courses` 对普通 USER 应是 `200 / OK` 且不含外部课程。
- 不为 `CourseAccessService` legacy overload 本身补单元测试；cleanup 目标是删除 legacy API，单测会固化待移除接口。
- 对 JWT 使用测试内现有 `jwt(sub, name, roles)` helper，避免手写 token 或依赖真实 secret。
- 保持每个测试只验证一个行为：一个身份、一个入口、一个预期授权结果；不要把多个 controller path 塞进同一个 mega-test。

## 覆盖缺口与风险

- 高风险：`KnowledgeCatalogService` 仍存在多个 legacy public overload/private helper/fallback。即使 HTTP path 已 roles-first，删除 `CourseAccessService` legacy overload 后仍可能需要同步删除或迁移这些兼容入口。
- 中风险：`GradingEvaluationServiceTest` 当前通过 mock `CourseAccessService` 构造服务；cleanup 后若签名调整，mock stubbing 需跟随 roles-first 参数，避免宽松 mock 掩盖调用错误。
- 中风险：`ResourceGenerationService` 的 `allowAdminEnrollmentBypass` 布尔语义来自上游角色/业务规则，测试应覆盖 direct 与 orchestrator 两条入口，避免后续误把 admin/teacher role 或 subject-name 混为一谈。
- 低风险：`AnalyticsService` 和 `AssessmentService` 已有较完整 roles-first HTTP 覆盖；本切片可作为 adjacent regression，不建议再新增大量重复矩阵。

## 建议验收标准

- P0 focused 测试通过。
- 删除 `CourseAccessService` public legacy overload 后 `mvn --% -DskipTests compile` 通过。
- Adjacent controller tests 通过。
- Full backend `mvn test` 通过。
- `rg "requireCourseRead\\(String currentUserId, String courseId\\)|requireCourseManage\\(String currentUserId, Course course\\)|requireLearnerEnrolledForExistingCourse\\(String currentUserId, String learnerId, String courseId\\)|listCoursesForUser\\(String currentUserId\\)" backend/src/main/java` 不再命中 `CourseAccessService` legacy API 定义。
