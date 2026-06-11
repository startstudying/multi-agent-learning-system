# REQ-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. 背景

P3-4 权限与安全加固已经完成 formal OAuth2/JWK/Spring Security、RAG runtime roles-first RBAC、KB-course binding governance、SSE production auth strategy 等关键切片。HTTP Controller 主路径已逐步改为从 `UserContext.roles()` 提取显式角色事实，再传入 Service 层。

当前仍存在一个服务层遗留面：部分 Application Service 仍公开或保留通过 `currentUserId == "admin"`、`currentUserId == "teacher"`、`currentUserId.startsWith("teacher_")` 推断角色的 legacy overload/helper。虽然当前 HTTP 入口多数已经走 roles-first 签名，但这些兼容入口会给后续内部调用、Agent/Orchestrator 调用或新代码复用留下 role-confusion 风险。

## 2. 目标

清理以下服务中的 subject-name role inference：

- `KnowledgeCatalogService`
- `AssessmentService`
- `GradingEvaluationService`

目标结果：

- 服务层授权入口只接受显式 `currentUserAdmin` / `currentUserTeacher` role facts。
- 不再通过 `currentUserId` 字符串推断 admin/teacher。
- HTTP API 合同、数据库、依赖、前端行为保持不变。
- 用反射守卫测试防止 legacy overload/helper 被重新引入。

## 3. 非目标

- 不修改 REST API path、HTTP method、request DTO、response DTO。
- 不修改数据库 schema 或迁移脚本。
- 不新增依赖。
- 不修改前端。
- 不重构 Course/RAG/Agent/Review Gate 之外的权限体系。
- 不关闭 P3-4 父项；本任务只收口服务层 legacy subject-name 入口。

## 4. 需求清单

### R1. KnowledgeCatalogService legacy 入口清理

必须移除只接收 `currentUserId` 且内部推断角色的 public overload：

- `createCourse(String currentUserId, CreateCourseRequest request)`
- `getCourseForUser(String currentUserId, String courseId)`
- `listCoursesForUser(String currentUserId)`
- `createChapter(String currentUserId, String courseId, CreateChapterRequest request)`
- `createKnowledgePoint(String currentUserId, CreateKnowledgePointRequest request)`
- `createDependency(String currentUserId, CreateKnowledgeDependencyRequest request)`
- `getKnowledgeGraphForUser(String currentUserId, String courseId)`

必须移除 private subject-name helper：

- `resolveCourseTeacherId(String currentUserId, String requestedTeacherId)`
- `requireCourseTeacherOrAdmin(String currentUserId, Course course)`
- `requireCourseManageAccess(String currentUserId, Course course)`
- `requireCourseReadAccess(String currentUserId, Course course)`
- `scopedCourseMissing(String currentUserId)`
- `isAdmin(String currentUserId)`
- `isTeacherUser(String currentUserId)`

### R2. AssessmentService legacy 入口清理

必须移除只接收 `currentUserId` 且内部推断角色的 public overload：

- `listAnswers(String currentUserId, String learnerId, String courseId, int page, int size)`
- `listWrongQuestions(String currentUserId, String learnerId, String courseId, int page, int size)`
- `answerDetail(String currentUserId, String answerId)`
- `wrongQuestionDetail(String currentUserId, String wrongQuestionId)`

必须移除 private subject-name helper：

- `isAdmin(String currentUserId)`
- `isTeacherUser(String currentUserId)`

### R3. GradingEvaluationService legacy 入口清理

必须移除只接收 `currentUserId` 且内部推断角色的 public overload：

- `evaluate(String currentUserId, GradingEvaluationRequest request)`

必须移除 private subject-name helper：

- `isAdmin(String currentUserId)`
- `isTeacherUser(String currentUserId)`

必须保留纯算法入口：

- `evaluate(GradingEvaluationRequest request)`
- `evaluate(List<Double> humanScores, List<Double> aiScores, double agreementThreshold)`

### R4. 测试要求

新增或补齐反射守卫：

- 目标 legacy public overload 不再出现在对应 Service 的 declared methods。
- 目标 private subject-name helper 不再出现在对应 Service 的 declared methods。

保留并运行邻接 RBAC 测试，确认 HTTP roles-first 行为未回退。

## 5. 验收标准

- 三个目标服务不再包含 subject-name role inference helper。
- 三个目标服务不再暴露本需求列出的 legacy public overload。
- Controller 编译通过，仍调用 roles-first 签名。
- 反射守卫测试通过。
- focused、adjacent、full backend Maven 验证完成，或限制被明确记录。
- 文档、Evidence、Acceptance、Changelog、Memory、TODO 均更新。
