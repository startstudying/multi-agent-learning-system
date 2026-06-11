# RUN-20260609 P3-4-M Course API / CourseAccessService roles-first overload 最小 RED 测试矩阵

## 任务与边界

- 任务类型：只读测试设计 / 最小 RED 矩阵。
- 目标切片：`CourseController` / `KnowledgeCatalogService` / `CourseAccessService` 的 Course 读路径与 Course graph 写路径，验证从 `currentUserId` 字符串推断角色迁移到 roles-first overload。
- 只读约束：本报告仅设计测试，不修改生产代码或测试代码；未执行 Maven。
- 已读取入口文档：`AGENTS.md`、`docs/memory/PROJECT_MEMORY.md`、`docs/memory/BACKEND_MEMORY.md`、`docs/skills/SKILL_REGISTRY.md`、`docs/harness/TEST_COMMANDS.md`。
- 重点检查文件：`CourseKnowledgeControllerTest`、`DevAuthFilterTest`、`CurrentUserServiceTest`、`CourseController`、`CourseAccessService`、`KnowledgeCatalogService`。

## 当前证据

- `DevAuthFilterTest` 已覆盖 Bearer token 优先于 spoofed `X-User-Id`，invalid Bearer 不 fallback，staging / production header-only 返回 `UNAUTHORIZED`。
- `CurrentUserServiceTest` 已覆盖 `isAdmin()` / `isTeacherUser()` 优先使用 roles，且 production 不再从 `admin` / `teacher_*` 字符串推断角色。
- `AnalyticsControllerTest` 的 P3-4-L 已有相邻模式：`teacherClassSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`、`teacherClassSummaryAllowsBearerTeacherWhenSubjectOwnsCourse`、`teacherClassSummaryRejectsBearerStudentWithSpoofedAdminHeader`。
- `CourseController` 当前只向业务层传 `currentUserService.currentUserId()`，未传 `currentUserService.isAdmin()` / `currentUserService.isTeacherUser()`。
- `CourseAccessService` 当前仍在内部通过 `"admin".equals(currentUserId)` 和 `currentUserId.startsWith("teacher_")` 推断角色。

## 建议新增 / 调整的测试方法名

建议在 `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java` 新增 5 个 focused integration tests，并复用该类现有 `MockMvc`、`createCourseForTeacher(...)`、`seedEnrollment(...)` 风格。为 Bearer token 可从 `AnalyticsControllerTest` / `DevAuthFilterTest` 引入本地 `jwt(...)` helper，secret / issuer 使用当前 test profile 对应固定值。

1. `courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
2. `courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader`
3. `bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph`
4. `bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix`
5. `bearerStudentRoleWithSpoofedAdminHeaderCannotReadOrManageForeignCourse`

## 最小 RED 测试矩阵

| 测试方法 | 请求 | 身份 | 预期 HTTP | 预期 `code` | 当前是否 RED | RED 原因 |
|---|---|---|---:|---|---|---|
| `courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader` | `GET /api/courses` | `Authorization: Bearer(sub=ops_admin, roles=[ADMIN])` + `X-User-Id: alice` | `200` | `OK` | 是 | Bearer 已建立 admin role，但 Course list 只传 `currentUserId=ops_admin`；`CourseAccessService.listCoursesForUser` 不识别 roles，按普通 learner 查 active enrollment，返回空列表而不是全部课程。 |
| `courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader` | `GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph` | `Authorization: Bearer(sub=ops_admin, roles=[ADMIN])` + `X-User-Id: alice` | `200` | `OK` | 是 | `ops_admin` 不等于 legacy `"admin"`，`requireCourseRead` 不识别 admin role，会进入 teacher/student 分支并返回 `FORBIDDEN`。 |
| `bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph` | `GET /api/courses/course_missing_roles_admin`、`GET /api/courses/course_missing_roles_admin/knowledge-graph` | `Authorization: Bearer(sub=ops_admin, roles=[ADMIN])` + `X-User-Id: alice` | `404` | `NOT_FOUND` | 是 | `scopedCourseMissing(currentUserId)` 只认 legacy `"admin"`；Bearer admin subject `ops_admin` 被当作非 admin，缺失课程会返回安全 `FORBIDDEN`，不符合 admin 运维可见 `NOT_FOUND` 语义。 |
| `bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix` | `GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph`、`POST /api/courses/{courseId}/chapters` | `Authorization: Bearer(sub=instructor_1, roles=[TEACHER])`，课程 `teacherId=instructor_1` | `200` | `OK` | 是 | 当前 teacher 判断依赖 `"teacher"` 或 `teacher_` 前缀；真实 Bearer subject 不一定符合 legacy 命名。`instructor_1` 拥有课程且有 `TEACHER` role，当前仍会被当作 student 并拒绝读/写。 |
| `bearerStudentRoleWithSpoofedAdminHeaderCannotReadOrManageForeignCourse` | `GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph`、`POST /api/courses/{courseId}/chapters` | `Authorization: Bearer(sub=alice, roles=[STUDENT])` + `X-User-Id: admin`，无 active enrollment | `403` | `FORBIDDEN` | 否，作为防回归 GREEN | 该用例确认 Course API 不因 spoofed `X-User-Id: admin` 放权。当前 Bearer 优先生效且 `alice` 无 enrollment，预期应已通过；保留它是为了锁定迁移后的负向边界。 |

## 单个测试的断言细节

### `courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`

- 准备：创建两个课程，例如 legacy admin seed `teacherId=teacher_1` 和 `teacherId=teacher_2`。
- 请求：`GET /api/courses`。
- 身份：Bearer `sub=ops_admin`、`roles=[ADMIN]`，同时带 `X-User-Id: alice`。
- 预期：HTTP `200`，`$.code == "OK"`，`$.data.length() == 2`，响应不受 `alice` enrollment 影响。
- 当前 RED：预计返回 `200 OK` 但 `data.length() == 0`。

### `courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader`

- 准备：创建课程、章节、知识点。
- 请求：`GET /api/courses/{courseId}` 与 `GET /api/courses/{courseId}/knowledge-graph`。
- 身份：Bearer `sub=ops_admin`、`roles=[ADMIN]`，同时带 `X-User-Id: alice`。
- 预期：两次请求均 HTTP `200`，`$.code == "OK"`，detail 的 `$.data.id == courseId`，graph 的 `$.data.course.id == courseId`。
- 当前 RED：预计 HTTP `403`，`code == "FORBIDDEN"`。

### `bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph`

- 请求：`GET /api/courses/course_missing_roles_admin` 与 `GET /api/courses/course_missing_roles_admin/knowledge-graph`。
- 身份：Bearer `sub=ops_admin`、`roles=[ADMIN]`，同时带 `X-User-Id: alice`。
- 预期：两次请求均 HTTP `404`，`$.code == "NOT_FOUND"`，`$.data` 不存在。
- 当前 RED：预计 HTTP `403`，`code == "FORBIDDEN"`。

### `bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix`

- 准备：用 legacy admin seed 一个课程，`teacherId=instructor_1`；创建基础 graph 数据。
- 请求：`GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph`、`POST /api/courses/{courseId}/chapters`。
- 身份：Bearer `sub=instructor_1`、`roles=[TEACHER]`，不依赖 `X-User-Id`。
- 预期：三次请求均 HTTP `200`，`$.code == "OK"`；chapter 创建返回 `$.data.courseId == courseId`。
- 当前 RED：预计 HTTP `403`，因为 `instructor_1` 不满足 legacy `isTeacherUser(currentUserId)`。

### `bearerStudentRoleWithSpoofedAdminHeaderCannotReadOrManageForeignCourse`

- 准备：创建 foreign course，不给 `alice` active enrollment。
- 请求：`GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph`、`POST /api/courses/{courseId}/chapters`。
- 身份：Bearer `sub=alice`、`roles=[STUDENT]`，同时带 `X-User-Id: admin`。
- 预期：三次请求均 HTTP `403`，`$.code == "FORBIDDEN"`，`$.data` 不存在，响应 body 不包含 `courseId`。
- 当前状态：预计 GREEN；用于证明迁移后仍是 roles-first 且不回退 spoofed header。

## 测试当前应 RED 的原因汇总

- RED 核心不是 `DevAuthFilter` 或 `CurrentUserService`，它们已经能建立 roles-first `UserContext`。
- RED 核心在 Course 局部迁移尚未完成：`CourseController` 没有把 role booleans 传入 service；`KnowledgeCatalogService` / `CourseAccessService` 暴露的 Course read/list/manage 入口也尚无 roles-first overload。
- 现有 Course 测试几乎都走 `X-User-Id: admin` / `teacher_1` legacy identity，因此无法暴露 Bearer subject 与 legacy userId 命名不一致时的权限错误。
- P3-4-M 的最小 RED 应只打 Course API / CourseAccessService 局部，不扩散到 Spring Security、OAuth2/JWK、前端或全量 RBAC。

## Maven 命令建议

Focused：

```bash
cd backend && mvn --% -Dtest=CourseKnowledgeControllerTest test
```

Adjacent：

```bash
cd backend && mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
```

Full：

```bash
cd backend && mvn test
```

可选编译预检：

```bash
cd backend && mvn compile -q
```

## 测试范围不该覆盖的内容

- 不覆盖 Spring Security / OAuth2 Resource Server / JWK 引入；这是后续正式认证架构任务。
- 不新增依赖、不做 dependency review。
- 不改数据库 schema、course enrollment schema 或 repository 查询结构。
- 不测试前端页面、路由、状态管理。
- 不覆盖 RAG KB 管理、PromptVersion、Evaluation Set、Assessment 全链路 RBAC；本轮只覆盖 Course API / `CourseAccessService` 局部迁移。
- 不把 Course create 的 teacherId 合同扩大为完整用户目录校验；本轮最多验证 Course read/list/manage 对 Bearer roles 的判权。
- 不做性能、分页、排序、N+1 优化测试。
- 不测试生产环境 header-only `UNAUTHORIZED`，该职责已由 `DevAuthFilterTest` 覆盖。

## 结论

最小 RED 矩阵建议以 4 个应 RED 的 roles-first Course API 用例加 1 个 spoofed header 负向防回归用例组成。它们能精确暴露 P3-4-M 当前缺口：认证层已经提供 role，但 Course 局部仍只按 `currentUserId` 字符串推断 admin/teacher，导致 Bearer admin subject `ops_admin` 和 Bearer teacher subject `instructor_1` 被错误降级为普通 learner。
