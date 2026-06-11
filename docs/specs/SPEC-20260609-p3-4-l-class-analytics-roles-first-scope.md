# SPEC-20260609 P3-4-L class analytics roles-first course scope

## 1. Scope

本规格只覆盖 `GET /api/analytics/classes/{courseId}/summary` 的 class/course roles-first 授权切片。

## 2. Current Gap

当前入口：

```text
AnalyticsController.teacherClassSummary(courseId)
-> analyticsService.teacherClassSummary(courseId, currentUserId)
-> AnalyticsService.requireTeacherClassAccess(currentUserId, course)
```

问题：

- Controller 未传递 `CurrentUserService.isAdmin()` / `isTeacherUser()`。
- Service 先查 course missing 并直接返回 `NOT_FOUND`，非 admin 可区分 missing 与 foreign。
- fallback 权限判断仍依赖 `"admin"` / `currentUserId == teacherId`。

## 3. Target Behavior

入口应调整为：

```text
AnalyticsController.teacherClassSummary(courseId)
-> analyticsService.teacherClassSummary(courseId, currentUserId, currentUserAdmin, currentUserTeacher)
```

Service 授权规则：

- `currentUserAdmin == true`：existing course 允许，missing course 返回 `NOT_FOUND`。
- `currentUserTeacher == true` 且 `currentUserId == Course.teacherId`：允许。
- 非 admin missing course：返回 `FORBIDDEN`。
- 非 admin foreign course：返回 `FORBIDDEN`。
- 其他用户：返回 `FORBIDDEN`。

## 4. API Contract

API path、HTTP method、response DTO 不变：

```text
GET /api/analytics/classes/{courseId}/summary
```

成功响应继续返回 `TeacherClassAnalyticsSummary`。

## 5. Files

Production:

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`

Tests:

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

Docs:

- 本切片 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / Evidence / Acceptance / Retro。

## 6. Test Matrix

| Test | Expected |
|---|---|
| Bearer ADMIN with spoofed `X-User-Id: alice` reads existing class summary | 200 |
| Bearer TEACHER subject owns course | 200 |
| Bearer TEACHER subject foreign course | 403 |
| Bearer STUDENT with spoofed `X-User-Id: admin` | 403 |
| non-admin missing course | 403 |
| admin missing course | 404 |

## 7. Architecture Drift Check

| Check | Expected |
|---|---|
| Controller only handles HTTP/current user extraction | PASS |
| Service owns object authorization | PASS |
| Permission in backend code, not Prompt | PASS |
| No frontend change | PASS |
| No new dependency | PASS |
| No schema drift | PASS |

## 8. Out of Scope

- Full `CourseAccessService` role-aware migration.
- Formal OAuth2/JWK/Spring Security.
- Broader class/course domain model.
- PromptVersion / Evaluation / RAG KB full RBAC.
