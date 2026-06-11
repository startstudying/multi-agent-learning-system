# EVIDENCE - P3-4-U Review Gate ResourceReview roles-first RBAC

## 1. Scope

本证据覆盖：

- `GET /api/reviews/resources`
- `POST /api/reviews/resources/{reviewId}/decision`
- Review Gate HTTP 主路径 Bearer roles-first RBAC。

## 2. Code Evidence

| File | Evidence |
|---|---|
| `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java` | `list` / `decide` 读取 `CurrentUserService.currentUser()`，只从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts，并传入 service roles-first overload。 |
| `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java` | 新增 `listResourceReviews(reviewerUserId, reviewerAdmin, reviewerTeacher, status)` 和 `decide(reviewerUserId, reviewerAdmin, reviewerTeacher, reviewId, request)`；roles-first path 不回落 subject-name inference。 |
| `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java` | 新增 Bearer admin spoofed header、Bearer teacher no-prefix、`USER sub=admin`、`USER sub=teacher_1`、teacher missing/foreign anti-enumeration 覆盖。 |

## 3. TDD RED

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceReviewControllerTest test
```

Observed RED:

```text
Tests run: 16, Failures: 3, Errors: 0, Skipped: 0
ResourceReviewControllerTest.bearerAdminCanListAndDecideResourceReviewsDespiteSpoofedHeader: expected 200 but was 403
ResourceReviewControllerTest.bearerUserSubjectAdminCannotListOrDecideResourceReviews: expected 403 but was 200
ResourceReviewControllerTest.bearerUserSubjectTeacherCannotUseCourseTeacherIdForReviews: expected 403 but was 200
```

Meaning: 新测试准确暴露旧逻辑仍从 subject-name 推断 admin/teacher。

## 4. GREEN / Verification

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceReviewControllerTest test
```

Result:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,ResourceGenerationControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

Result:

```text
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 454, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. Expert Review

只读审查 agent `019eac5d-cdea-78f1-8fd6-77c03c4d4636` 结论：目标代码路径 PASS。

审查限制：`D:\多元agent` 缺少 `.git` 元数据，不能用 `git diff/status` 严格证明全局未越界；但基于目标调用点和 Context 允许文件，未发现 API/DB/dependency/frontend 越界。

## 6. Remaining Out Of Scope

- ResourceGeneration / Agent Trace detail roles-first RBAC。
- CourseAccessService legacy overload 收口。
- broader class/course matrix。
- formal OAuth2/JWK/Spring Security。
- P3-2 工业级 PDF/DOCX 页码/章节层级。
