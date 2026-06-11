# P3-4-M Course API / CourseAccessService roles-first overload 安全边界报告

## 1. 审查范围

- 任务：只读分析下一切片 `P3-4-M`（`Course API / CourseAccessService roles-first overload` 局部迁移）的安全边界。
- 角色：Security & Quality 专家。
- 范围文件：
  - `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
  - `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
  - `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
  - `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
  - `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
  - `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- 已读取上下文：
  - `AGENTS.md`
  - `docs/memory/PROJECT_MEMORY.md`
  - `docs/memory/BACKEND_MEMORY.md`
  - `docs/skills/SKILL_REGISTRY.md`
  - `docs/architecture/ARCHITECTURE_BASELINE.md`
  - `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
  - `docs/skills/project-specific/object-scope-authorization.md`
  - `docs/skills/project-specific/auth-context-boundary.md`

## 2. Skill Selection Gate

1. 任务类型：安全边界分析 / 权限迁移设计审查 / RED 测试建议。
2. 选中技能：
   - `security-review`：覆盖 OWASP A01、认证/授权、secrets、依赖审计。
   - `object-scope-authorization`：课程、教师、学生、enrollment、missing-vs-foreign anti-enumeration。
   - `auth-context-boundary`：Bearer JWT、`UserContext.roles()`、`X-User-Id` fallback、spoofing 防护。
   - `architecture-drift-check`：确认本切片不越界到新依赖、schema、前端、完整 OAuth2/JWK。
3. 选择原因：本切片核心是将 Course API 从 legacy string identity 迁移到 roles-first 授权，属于高风险访问控制面。
4. 缺失技能：无。
5. GitHub research：不需要；项目内已有 auth 和 object scope 规则。
6. 新项目技能：暂不需要；现有 `auth-context-boundary` 与 `object-scope-authorization` 足够。

## 3. 总体风险评估

**风险等级：HIGH**

原因：

- `DevAuthFilter` 与 `CurrentUserService` 已经建立可信 Bearer role 上下文，但 Course API 主链路仍只传 `currentUserId`。
- `CourseAccessService` 与 `KnowledgeCatalogService` 内部仍使用 `"admin"`、`"teacher"`、`teacher_` userId 字符串推断角色。
- 在 Bearer roles 已成为可信身份来源后，继续依赖 legacy string identity 会造成两类风险：
  - Bearer `ADMIN` subject 不是 `"admin"` 时被误判为非管理员，破坏 admin 语义。
  - Bearer `USER/STUDENT` subject 若等于 `"admin"` 或 `teacher_*`，服务层可能把 subject 当角色使用，形成角色混淆。

## 4. 当前仍依赖 legacy string identity 的风险点

### 4.1 `CourseController` 未传递 roles-first 权限事实

位置：

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:36`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:43`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:49`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:61`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:68`

现状：

```java
knowledgeCatalogService.getCourseForUser(currentUserService.currentUserId(), courseId)
```

风险：

- Controller 没有把 `currentUserService.isAdmin()`、`currentUserService.isTeacherUser()` 传入 `KnowledgeCatalogService` / `CourseAccessService`。
- 即使 `DevAuthFilter` 已经用 Bearer token 建立 `UserContext.roles()`，Course 授权服务仍无法看到 roles 事实。

安全影响：

- OWASP A01 Broken Access Control。
- Bearer role 与业务授权脱节，后续迁移容易出现“认证层正确、服务层仍按字符串误判”的局部绕过或拒绝服务。

### 4.2 `CourseAccessService` 角色判断仍是字符串推断

位置：

- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:37`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:59`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:109`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:148`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:152`

现状：

```java
private boolean isAdmin(String currentUserId) {
    return "admin".equals(currentUserId);
}

private boolean isTeacherUser(String currentUserId) {
    return "teacher".equals(currentUserId) || (currentUserId != null && currentUserId.startsWith("teacher_"));
}
```

风险：

- Bearer `ADMIN` role + `sub=ops_admin` 访问课程会被当作普通用户，而不是 admin。
- Bearer `USER/STUDENT` role + `sub=admin` 会被当作 admin。
- Bearer `USER/STUDENT` role + `sub=teacher_1` 会被当作 teacher。
- `scopedCourseMissing(currentUserId)` 使用 `"admin"` 决定 `NOT_FOUND` vs `FORBIDDEN`，会让 Bearer admin 的 missing course 行为错误，也会让伪 admin subject 获得 admin-style existence oracle。

### 4.3 `KnowledgeCatalogService` 仍复制 legacy 角色判断

位置：

- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:220`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:249`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:257`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:277`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:281`

风险：

- `createCourse(...)` 的 `resolveCourseTeacherId(...)` 仍按 `currentUserId` 判断 admin/teacher。
- `courseAccessService != null` 时，部分读/写走 `CourseAccessService`；但 `resolveCourseTeacherId(...)` 等创建入口仍在本服务内本地判断。
- 若只改 `CourseAccessService` 而不改 `KnowledgeCatalogService` 的 Course API 创建路径，`POST /api/courses` 仍可能保留角色混淆风险。

### 4.4 `CourseKnowledgeControllerTest` 目前主要覆盖 header legacy 路径

位置：

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:50` 等大量 `.header("X-User-Id", ...)`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:488`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:505`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:524`

现状：

- 已覆盖 course list/detail/graph 的 admin、teacher、student/enrollment、foreign teacher、missing-vs-foreign。
- 已覆盖 student 即使 active enrollment 也不能写 course graph。
- 但尚未覆盖 Bearer roles + spoofed `X-User-Id` 的 Course API 矩阵。

风险：

- 现有测试会让 legacy 行为继续“看起来安全”，但无法暴露 Bearer role 与服务层字符串判断之间的断裂。

## 5. 本切片应该收口的最小安全行为

### 5.1 roles-first overload 应作为 Course API 主路径

建议最小边界：

- `CourseController` 对 Course API 调用传入：
  - `currentUserService.currentUserId()`
  - `currentUserService.isAdmin()`
  - `currentUserService.isTeacherUser()`
- `KnowledgeCatalogService` 增加 roles-first overload，并让 Controller 只调用 roles-first overload。
- `CourseAccessService` 增加 roles-first overload，例如：

```java
public Course requireCourseRead(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        String courseId
) {
    Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> scopedCourseMissing(currentUserAdmin));
    if (currentUserAdmin) {
        return course;
    }
    if (currentUserTeacher && hasText(course.getTeacherId()) && course.getTeacherId().equals(currentUserId)) {
        return course;
    }
    if (!currentUserTeacher && courseEnrollmentRepository.existsByCourseIdAndLearnerIdAndStatus(
            courseId,
            currentUserId,
            ENROLLMENT_ACTIVE
    )) {
        return course;
    }
    throw new ApiException(ErrorCode.FORBIDDEN, "Course access denied");
}
```

兼容边界：

- 旧 `requireCourseRead(String currentUserId, String courseId)` 可以暂时保留，但只能作为 legacy overload，并明确委托 legacy 推断路径。
- 本切片不必一次迁移全仓库所有 `CourseAccessService.requireCourseRead(...)` 调用方，但 Course API / Course graph / Course create/manage 入口必须使用 roles-first overload。

### 5.2 admin / teacher / student 行为矩阵

`admin`：

- Bearer `roles=[ADMIN]` 应允许读取任意 existing course，即使 `sub != "admin"`。
- Bearer `ADMIN` 对 missing course 保留 `NOT_FOUND`，便于运维定位。
- Bearer `ADMIN` + spoofed `X-User-Id=student` 必须仍按 token admin 授权。

`teacher`：

- Bearer `roles=[TEACHER]` 只有在 `token.sub == Course.teacherId` 时可读/写 own course。
- Bearer `TEACHER` + spoofed `X-User-Id=other_teacher` 不能扩大权限。
- Bearer `TEACHER` 访问 missing course 与 foreign course 都应返回安全 `FORBIDDEN`，不形成 existence oracle。

`student / ordinary user`：

- Bearer `roles=[USER]` 或无 teacher/admin role 时，只能通过 active enrollment 读取课程。
- 即使 `token.sub == "admin"` 或 `token.sub` 以 `teacher_` 开头，只要 token roles 不是 `ADMIN/TEACHER`，就不能获得 admin/teacher 权限。
- student 即使 active enrollment，也不能创建 course、chapter、knowledge point、dependency。

### 5.3 Bearer role 必须覆盖 spoofed `X-User-Id`

当前 `DevAuthFilter` 已经在 Bearer 存在时优先校验 token，并在有效 token 下建立 `UserContext`。本切片需要验证 Course API 确实继承这个性质：

- `Authorization: Bearer <ADMIN token sub=ops_admin>` + `X-User-Id: alice`：按 admin。
- `Authorization: Bearer <USER token sub=alice>` + `X-User-Id: admin`：按 student/user，不能按 admin。
- `Authorization: Bearer <TEACHER token sub=teacher_1>` + `X-User-Id: teacher_2`：只允许 `teacher_1` 自有课程。

### 5.4 missing-vs-foreign anti-enumeration

本切片应保持并扩展现有语义：

- admin missing course：`NOT_FOUND`，`data` 不存在。
- non-admin missing course：`FORBIDDEN`，`data` 不存在。
- non-admin foreign course：`FORBIDDEN`，`data` 不存在。
- non-admin missing 与 foreign 的响应不应包含 course id、teacher id、title、chapter id、knowledge point id。

需要覆盖：

- `GET /api/courses/{courseId}`
- `GET /api/courses/{courseId}/knowledge-graph`
- `POST /api/courses/{courseId}/chapters`
- `POST /api/knowledge-points`
- `POST /api/knowledge-dependencies`

## 6. 不应该纳入本切片的范围

本切片应保持小而安全，不建议纳入：

1. 完整 OAuth2 Resource Server / JWK / Spring Security 替换。
2. 用户角色表、组织/班级/租户 RBAC 模型设计。
3. 数据库 schema 变更。
4. 前端改造。
5. 新依赖引入。
6. RAG KB 管理、PromptVersion、EvaluationSet、Assessment、ResourceReview 的全量 roles-first 迁移。
7. 删除 dev/test `X-User-Id` 兼容路径。
8. 重写 `KnowledgeCatalogService` 大模块或抽象全局 `AuthorizationService`。
9. 改变 API path、DTO 字段、错误码契约。
10. 改变 course enrollment 业务语义。

## 7. 建议 RED 测试清单

建议优先在 `CourseKnowledgeControllerTest` 中新增 Bearer helper，并补以下 RED 用例。测试命名可按项目现有风格调整。

### 7.1 Bearer admin role 不依赖 `sub == "admin"`

```java
@Test
void courseDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader() throws Exception {
    String courseId = createCourseForTeacher(TEACHER_ID, "Bearer Admin Course");

    mockMvc.perform(get("/api/courses/{courseId}", courseId)
                    .header("Authorization", bearerToken("ops_admin", "ADMIN"))
                    .header("X-User-Id", STUDENT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(courseId));
}
```

当前预期 RED：服务层只认 `"admin"`，`ops_admin` 可能被当普通用户。

### 7.2 Bearer student 不能通过 `X-User-Id: admin` 升权

```java
@Test
void courseDetailRejectsBearerStudentWithSpoofedAdminHeader() throws Exception {
    String courseId = createCourseForTeacher(TEACHER_ID, "Spoofed Admin Course");

    mockMvc.perform(get("/api/courses/{courseId}", courseId)
                    .header("Authorization", bearerToken(STUDENT_ID, "USER"))
                    .header("X-User-Id", ADMIN_ID))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

安全目标：证明 Course API 只信 Bearer user/role，不信 spoofed header。

### 7.3 Bearer `USER sub=admin` 不应被字符串推断为 admin

```java
@Test
void courseDetailDoesNotInferAdminFromBearerSubjectWithoutAdminRole() throws Exception {
    String courseId = createCourseForTeacher(TEACHER_ID, "Subject Admin Without Role");

    mockMvc.perform(get("/api/courses/{courseId}", courseId)
                    .header("Authorization", bearerToken("admin", "USER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

当前高价值 RED：`CourseAccessService.isAdmin("admin")` 会把 subject 当 admin。

### 7.4 Bearer `USER sub=teacher_1` 不应被字符串推断为 teacher

```java
@Test
void courseWriteDoesNotInferTeacherFromBearerSubjectWithoutTeacherRole() throws Exception {
    String courseId = createCourseForTeacher(TEACHER_ID, "Subject Teacher Without Role");

    mockMvc.perform(post("/api/courses/{courseId}/chapters", courseId)
                    .header("Authorization", bearerToken(TEACHER_ID, "USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title": "Forbidden Chapter",
                              "sequenceNo": 9
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

当前高价值 RED：`CourseAccessService.isTeacherUser("teacher_1")` 会把 subject 当 teacher。

### 7.5 Bearer teacher role 只能管理 own course

```java
@Test
void courseWriteAllowsBearerTeacherOnlyForOwnedCourse() throws Exception {
    String ownCourseId = createCourseForTeacher(TEACHER_ID, "Own Course");
    String foreignCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Foreign Course");

    mockMvc.perform(post("/api/courses/{courseId}/chapters", ownCourseId)
                    .header("Authorization", bearerToken(TEACHER_ID, "TEACHER"))
                    .header("X-User-Id", OTHER_TEACHER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title": "Own Chapter",
                              "sequenceNo": 1
                            }
                            """))
            .andExpect(status().isOk());

    mockMvc.perform(post("/api/courses/{courseId}/chapters", foreignCourseId)
                    .header("Authorization", bearerToken(TEACHER_ID, "TEACHER"))
                    .header("X-User-Id", OTHER_TEACHER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title": "Foreign Chapter",
                              "sequenceNo": 2
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

### 7.6 Bearer admin missing course 保留 `NOT_FOUND`

```java
@Test
void bearerAdminSeesMissingCourseAsNotFound() throws Exception {
    mockMvc.perform(get("/api/courses/{courseId}", "missing_course")
                    .header("Authorization", bearerToken("ops_admin", "ADMIN"))
                    .header("X-User-Id", STUDENT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

当前预期 RED：若 admin 由 `"admin"` 判断，`ops_admin` missing 可能返回 `FORBIDDEN`。

### 7.7 Bearer non-admin missing 与 foreign 统一 `FORBIDDEN`

```java
@Test
void bearerTeacherCannotDistinguishMissingCourseFromForeignCourse() throws Exception {
    String foreignCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Foreign Course");

    String foreignBody = mockMvc.perform(get("/api/courses/{courseId}", foreignCourseId)
                    .header("Authorization", bearerToken(TEACHER_ID, "TEACHER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String missingBody = mockMvc.perform(get("/api/courses/{courseId}", "missing_course")
                    .header("Authorization", bearerToken(TEACHER_ID, "TEACHER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(foreignBody).doesNotContain(foreignCourseId, OTHER_TEACHER_ID, "Foreign Course");
    assertThat(missingBody).doesNotContain("missing_course");
}
```

### 7.8 Bearer roles 覆盖 list 语义

```java
@Test
void courseListUsesBearerRolesAndIgnoresSpoofedUserIdHeader() throws Exception {
    String teacherCourseId = createCourseForTeacher(TEACHER_ID, "Teacher One Course");
    String otherTeacherCourseId = createCourseForTeacher(OTHER_TEACHER_ID, "Teacher Two Course");

    mockMvc.perform(get("/api/courses")
                    .header("Authorization", bearerToken("ops_admin", "ADMIN"))
                    .header("X-User-Id", STUDENT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

    mockMvc.perform(get("/api/courses")
                    .header("Authorization", bearerToken(TEACHER_ID, "TEACHER"))
                    .header("X-User-Id", OTHER_TEACHER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(teacherCourseId));
}
```

## 8. 风险与缓解

| 风险 | 严重度 | 利用条件 | Blast Radius | 缓解 |
|---|---|---|---|---|
| Course 服务层继续从 `currentUserId` 推断 admin/teacher | HIGH | 有效 Bearer token 的 `sub` 与 legacy 角色字符串重合，或 admin subject 不是 `"admin"` | 课程读写、知识图谱写入、课程列表泄露/拒绝 | 增加 roles-first overload，Controller 传 `isAdmin/isTeacherUser` |
| Bearer `USER sub=admin` 被当作 admin | HIGH | token subject 可为 `"admin"`，但 roles 非 admin | 任意课程读取/写入，missing course oracle | roles 优先，服务层禁止从 Bearer subject 推断角色 |
| Bearer `USER sub=teacher_1` 被当作 teacher | HIGH | token subject 以 `teacher_` 开头，roles 非 teacher | 自有 teacherId 相同课程的写入绕过 | teacher 权限必须来自 `currentUserTeacher` 且仍校验 `sub == Course.teacherId` |
| `CourseController` 只传 userId | MEDIUM-HIGH | 任意 Course API 调用 | 认证层与授权层事实不一致 | Controller 改为 roles-first service overload |
| missing-vs-foreign 响应被 admin 判断错误影响 | MEDIUM | Bearer admin subject 不是 `"admin"` 或伪 admin subject | 对象存在性探测或 admin 运维行为错误 | `scopedCourseMissing(boolean currentUserAdmin)` |
| 测试仍以 `X-User-Id` 为主 | MEDIUM | 测试未覆盖 Bearer roles | 回归无法暴露 roles-first 断裂 | 新增 Bearer + spoofed header RED 矩阵 |

## 9. OWASP Top 10 对照

- A01 Broken Access Control：本切片核心风险；必须优先修复。
- A02 Cryptographic Failures：未发现本切片生产代码新增加密逻辑；JWT 校验已在 `DevAuthFilter`，本切片不改。
- A03 Injection：Course API 本切片无 SQL 字符串拼接；JPA repository 路径未见注入面。
- A04 Insecure Design：legacy role inference 与 roles-first auth 并存，是当前设计过渡风险。
- A05 Security Misconfiguration：dev/test `X-User-Id` fallback 是受环境约束的兼容路径；本切片不删除，但业务测试必须证明 Bearer 存在时不会被 spoofed header 覆盖。
- A06 Vulnerable and Outdated Components：尝试运行 `mvn org.owasp:dependency-check-maven:check -DskipTests -Dformat=JSON -DfailBuildOnCVSS=11`，NVD 更新成功，但 RetireJS 数据源因 `raw.githubusercontent.com` DNS 失败中断，依赖审计未闭环。
- A07 Identification and Authentication Failures：`CurrentUserService` / `DevAuthFilter` 已建立 Bearer-first 基础，本切片要防止服务层绕开 roles。
- A08 Software and Data Integrity Failures：无新增构建/插件/依赖变更。
- A09 Security Logging and Monitoring Failures：本切片不改日志；注意 RED 测试响应不得包含 course/teacher/knowledge ids。
- A10 SSRF：本切片无外部 URL 获取。

## 10. Secrets / Dependency / Git History 检查

- Secrets 关键字扫描命令：`rg -n "password|secret|token|api[_-]?key|jwtSecret|Authorization|X-User-Id|Bearer" AGENTS.md docs backend/src/main/java backend/src/test/java`
- 结果：命中主要为文档安全规则、认证字段名、测试中的假 `sk-live-secret` / `raw-token` 脱敏断言；在本切片生产代码范围未发现真实硬编码 API key、password、token、private key。
- 依赖审计命令：`mvn org.owasp:dependency-check-maven:check -DskipTests -Dformat=JSON -DfailBuildOnCVSS=11`
- 结果：未完成。失败原因是 RetireJS 数据源下载 `https://raw.githubusercontent.com/Retirejs/retire.js/master/repository/jsrepository.json` 时 DNS 失败；不能声称依赖安全通过。
- Git 历史 secret 扫描：无法完成。`git -C D:\多元agent status --short` 返回 `fatal: not a git repository`。

## 11. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 建议仍保持 Controller 只读取 `CurrentUserService`，授权在 Service / `CourseAccessService`。 |
| Frontend rules | N/A | 本切片不涉及前端。 |
| Agent / RAG rules | N/A | 本切片不涉及 Agent/RAG 执行。 |
| Security | FAIL until migrated | 当前 Course API 授权仍有 legacy string identity 风险。 |
| API / Database | PASS if scoped | 不应改 API path/DTO/schema。 |

## 12. 结论

P3-4-M 应被定义为一个小范围、强安全收益的局部迁移：

1. Course API 入口必须把 `currentUserId + isAdmin + isTeacherUser` 传入服务层。
2. `CourseAccessService` 必须提供 roles-first overload，并将 Course detail/list/graph/manage 的主路径切到 roles-first。
3. `KnowledgeCatalogService.createCourse(...)` / `resolveCourseTeacherId(...)` 也必须收口到 roles-first，否则 `POST /api/courses` 仍保留 legacy string role inference。
4. 保留 legacy overload 仅用于尚未迁移的调用方，不在本切片删除或全仓库重构。
5. RED 测试必须优先证明：Bearer roles 覆盖 spoofed `X-User-Id`、不从 Bearer subject 推断角色、non-admin missing/foreign 统一 `FORBIDDEN`、admin missing 保留 `NOT_FOUND`。

建议下一步由实现切片只修改 Course API 相关 Controller/Service/Test 文件，不新增依赖、不改 schema、不扩展到全局 RBAC。
