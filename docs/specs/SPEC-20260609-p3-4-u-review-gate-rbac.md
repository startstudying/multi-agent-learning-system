# SPEC - P3-4-U Review Gate ResourceReview roles-first RBAC

## 1. 范围

目标接口：

- `GET /api/reviews/resources`
- `POST /api/reviews/resources/{reviewId}/decision`

## 2. 授权模型

### 2.1 角色事实来源

HTTP 主路径只从 `CurrentUserService.currentUser().roles()` 派生：

- `currentUserAdmin = roles contains ADMIN`
- `currentUserTeacher = roles contains TEACHER`

不得通过 `currentUserId == "admin"` 或 `currentUserId.startsWith("teacher_")` 在 HTTP 主路径推断角色。

### 2.2 对象范围

Teacher 审核范围沿用：

```text
ResourceReview.generationTaskId
-> ResourceGenerationTask.id
-> ResourceGenerationTask.goalId
-> Course.id
-> Course.teacherId == currentUserId
```

Admin 可访问所有 existing review。Ordinary user 无审核权限。

## 3. Service API

新增 roles-first overload：

```java
listResourceReviews(String reviewerUserId, boolean reviewerAdmin, boolean reviewerTeacher, String status)

decide(String reviewerUserId, boolean reviewerAdmin, boolean reviewerTeacher, String reviewId, ReviewDecisionRequest request)
```

旧签名保留兼容：

```java
listResourceReviews(String reviewerUserId, String status)
decide(String reviewerUserId, String reviewId, ReviewDecisionRequest request)
```

旧签名可继续使用 legacy facts；HTTP controller 不得调用旧签名。

## 4. 错误语义

| Case | Response |
|---|---|
| 非 admin/teacher list | `FORBIDDEN`, no `data` |
| 非 admin/teacher decision | `FORBIDDEN`, no `data` |
| teacher foreign review | `FORBIDDEN`, no `data` |
| teacher missing review | `FORBIDDEN`, no `data` |
| admin missing review | `NOT_FOUND` |

越权响应不得包含 reviewId、generationTaskId、resourceId、title、traceId。

## 5. 测试策略

在 `ResourceReviewControllerTest` 新增 Bearer JWT integration tests：

1. Bearer admin + spoofed header 可 list/decision。
2. Bearer teacher no-prefix 可 list/decision own-course review。
3. Bearer `USER sub=admin` 被拒绝。
4. Bearer `USER sub=teacher_1` 被拒绝。
5. Bearer teacher foreign/missing review 返回安全 `FORBIDDEN`。

## 6. Architecture Drift

| Check | Expected |
|---|---|
| Backend layering | Controller 提取 auth facts；Service 执行业务授权。 |
| Security | 权限由后端代码执行，不依赖 prompt 或 subject 字符串。 |
| API / DB | 无 path/DTO/schema 变更。 |
