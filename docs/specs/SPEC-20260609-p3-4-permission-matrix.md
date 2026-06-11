# SPEC-20260609 P3-4-K 权限渗透测试矩阵补齐

## 1. Scope

本规格只补齐 P3-4 当前 transitional scope 的权限渗透测试矩阵。默认实现方式为新增/扩展测试；只有当测试暴露明确安全缺陷时，才做最小生产代码修复。

## 2. Test Architecture

优先新增轻量跨模块测试类：

```text
backend/src/test/java/com/learningos/security/api/PermissionMatrixControllerTest.java
```

该类从 HTTP Controller 入口验证跨模块不变量，避免仅通过 service/unit test 证明局部行为。

允许在少量已有测试类中补充更贴近模块上下文的测试，例如：

- `DevAuthFilterTest`：staging header-only auth。
- `AnalyticsControllerTest`：Bearer roles 驱动 admin-only endpoint。

## 3. Authorization Matrix

### 3.1 Auth Context

| Case | Expected |
|---|---|
| dev/test 无 Bearer + `X-User-Id` | 使用 header identity |
| dev/test valid Bearer + spoofed header | 使用 token subject / roles |
| dev/test invalid Bearer + spoofed header | `UNAUTHORIZED`，不 fallback |
| staging/prod 无 Bearer + header | `UNAUTHORIZED` |
| staging/prod valid Bearer + spoofed header | 使用 token subject / roles |

### 3.2 Student Write Deny

student 必须无法写入 teacher-managed surface：

- `POST /api/courses`
- `POST /api/courses/{courseId}/chapters`
- `POST /api/knowledge-points`
- `POST /api/knowledge-dependencies`
- `POST /api/knowledge-bases/{kbId}/documents` 带 `courseId`
- `POST /api/resource-reviews/{reviewId}/decision`

### 3.3 Teacher Foreign Course Deny

teacher 访问 foreign course scoped surface 必须 `FORBIDDEN` 且无 `data`：

- `GET /api/courses/{courseId}`
- `GET /api/courses/{courseId}/knowledge-graph`
- `GET /api/analytics/students/{learnerId}/summary?courseId=foreignCourse`
- `POST /api/resource-reviews/{reviewId}/decision`

### 3.4 Student Foreign Learner Deny

student 访问 foreign learner scoped surface 必须 `FORBIDDEN` 且无 `data`：

- `GET /api/assessment/answers/{answerId}`
- `GET /api/assessment/wrong-questions/{wrongQuestionId}`
- `GET /api/analytics/students/{learnerId}/summary`
- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/tasks/{taskId}/trace`

### 3.5 Non-admin Anti-enumeration

同一非 admin 用户访问 foreign object 与 missing object：

- 均返回 `FORBIDDEN`。
- 均无 `data`。
- 响应 body 不包含目标 object id、parent id、traceId、title、markdown content。

### 3.6 Admin Semantics

admin:

- 可访问 existing course、answer、analytics summary、review list/decision、resource task detail。
- missing course / answer / task 语义保持 `NOT_FOUND`。

## 4. Expected Production Code Boundary

默认不修改生产代码。

若出现 RED：

- `AnalyticsController.overview` 等仍使用 `currentUserId == "admin"` 而不使用 `CurrentUserService.isAdmin()` 的入口，应做最小修复。
- 只修复测试证明的安全缺陷，不迁移完整 Spring Security / OAuth2。
- 不新增依赖或 schema。

## 5. Architecture Drift Check

| Check | Expected |
|---|---|
| Controller only handles HTTP and delegates | PASS |
| Service owns object/course authorization | PASS |
| Permission in backend code, not Prompt | PASS |
| No frontend LLM/API key change | PASS |
| No new dependency | PASS |
| No schema drift | PASS |

## 6. Test Commands

Focused：

```powershell
cd backend
mvn --% -Dtest=PermissionMatrixControllerTest,DevAuthFilterTest,AnalyticsControllerTest test
```

Adjacent：

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest test
```

Full：

```powershell
cd backend
mvn test
```

## 7. Out-of-scope Follow-ups

- broader class/course domain model。
- formal OAuth2/JWK/Spring Security migration。
- PromptVersion / Evaluation endpoints full RBAC matrix。
- RAG KB management full permission matrix。
- Repository-level performance optimization for all list scopes。

## 8. Implementation Result

P3-4-K 当前 transitional matrix 已完成：

- `DevAuthFilterTest` 补齐 staging header-only deny。
- `AnalyticsControllerTest` 补齐 Bearer admin role 贯穿 `overview` / `token-budget governance`，以及 spoofed `X-User-Id` 不生效。
- `CourseKnowledgeControllerTest` 补齐 active enrolled student 不能写 course graph，并强化 dropped enrollment course 不出现在 student list。
- `DocumentControllerTest` 补齐 student 拥有 public KB 且 enrolled 时仍不能伪造 course metadata。
- RED 暴露 analytics admin-only 入口仍依赖 `"admin"` 字符串身份判断；已最小修复为 `CurrentUserService.isAdmin()` / `currentUserAdmin` gate。
- Focused / adjacent / full backend Maven verification 均通过。

本结果不改变第 7 节后续项：broader class/course 和 formal OAuth2/JWK/Spring Security 仍未完成。
