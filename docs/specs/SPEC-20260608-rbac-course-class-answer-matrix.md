# SPEC-20260608 P3-4-C 权限矩阵安全前置

## 1. 权限模型

沿用当前过渡身份模型：

- admin：`currentUserId == "admin"`。
- teacher：`currentUserId == "teacher"` 或以 `teacher_` 开头。
- student：其他用户。

本切片不声明真实 RBAC 完成。

## 2. Course 读取规则

### API

```text
GET /api/courses
GET /api/courses/{courseId}
GET /api/courses/{courseId}/knowledge-graph
```

### 规则

| User | List | Detail / Graph |
|---|---|---|
| admin | 全部课程 | 任意存在课程 200；missing 为 `NOT_FOUND` |
| own teacher | 自己课程 | 自己课程 200 |
| foreign teacher | 不含 foreign course | foreign 与 missing 均 `FORBIDDEN` |
| student | 空列表 | foreign 与 missing 均 `FORBIDDEN` |

## 3. Grading evaluation 规则

```text
POST /api/assessment/grading-evaluations
```

- admin：允许；P3-4-G 后必须提供 `courseId`，可评估任意 existing course。
- teacher：允许；P3-4-G 后必须提供 `courseId`，且只能评估 own course。
- student：`FORBIDDEN`。

本切片 P3-4-C 只完成 teacher/admin 角色门禁。P3-4-G 已在 `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md` 中继续收口：

- `GradingEvaluationRequest` 新增 `courseId`。
- HTTP 请求缺少 `courseId` 返回 `VALIDATION_ERROR`。
- teacher missing/foreign course 返回安全 `FORBIDDEN`。
- admin missing course 返回 `NOT_FOUND`。
- 非空 `samples[].knowledgePointId` 必须属于请求 `courseId`。

## 4. Service API 变更

`KnowledgeCatalogService`：

```java
Course getCourseForUser(String currentUserId, String courseId)
List<Course> listCoursesForUser(String currentUserId)
KnowledgeGraphResponse getKnowledgeGraphForUser(String currentUserId, String courseId)
```

`GradingEvaluationService`：

```java
GradingEvaluationSummary evaluate(String currentUserId, GradingEvaluationRequest request)
```

## 5. 错误语义

| 场景 | ErrorCode |
|---|---|
| admin missing course | `NOT_FOUND` |
| 非 admin missing/foreign course detail | `FORBIDDEN` |
| student grading evaluation | `FORBIDDEN` |
| teacher/admin grading evaluation missing `courseId` | `VALIDATION_ERROR` |
| teacher grading evaluation missing/foreign course | `FORBIDDEN` |
| admin grading evaluation missing course | `NOT_FOUND` |
| grading sample knowledge point outside request course | `VALIDATION_ERROR` |

错误响应不得返回 `data`。

## 6. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 读取 current user；Service 做授权。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG。 |
| Security | PASS | 权限在后端代码。 |
| API / Database | PASS | 不改 path，不改 schema。 |
