# REQ-20260608 P3-4-G Grading Evaluation Course Scope

## 1. Functional Requirements

### REQ-1 Course-scoped request

`POST /api/assessment/grading-evaluations` 请求体新增并要求：

| Field | Required | Semantics |
|---|---:|---|
| `courseId` | Yes | 本次 grading evaluation 的课程授权锚点。 |
| `samples` | No | 新版结构化样本；若提供 `knowledgePointId`，必须属于 `courseId`。 |
| `humanScores` / `aiScores` | No | 旧版分数数组仍可计算，但也必须绑定 `courseId`。 |

### REQ-2 Actor authorization

| Actor | Behavior |
|---|---|
| student | 始终 `FORBIDDEN`，无 `data`。 |
| teacher / `teacher_*` | 必须提供 `courseId`，且只能访问自己课程。 |
| admin | 必须提供 `courseId`，可访问任意存在课程。 |

### REQ-3 Error semantics

| Scenario | ErrorCode |
|---|---|
| teacher/admin 未提供 `courseId` | `VALIDATION_ERROR` |
| student 调用，不论 `courseId` 是否存在 | `FORBIDDEN` |
| teacher foreign existing course | `FORBIDDEN` |
| teacher non-existent course | `FORBIDDEN` |
| admin non-existent course | `NOT_FOUND` |
| sample `knowledgePointId` 不属于请求 course | `VALIDATION_ERROR` |

错误响应不得返回 `data`，不得在 message 中包含 foreign course id、teacher id 或 sample 详情。

### REQ-4 Sample course consistency

- 当请求包含 `samples` 时：
  - 空白 `knowledgePointId` 可继续按 `UNKNOWN` 分组，不作为课程过滤依据。
  - 非空 `knowledgePointId` 必须存在于请求 `courseId` 的知识点集合中。
  - 混入其他课程或不存在的 `knowledgePointId` 返回 `VALIDATION_ERROR`。
- legacy score array 模式没有 sample knowledge point，但必须先通过 `courseId` 授权。

### REQ-5 Compatibility boundary

- 保留 `GradingEvaluationService.evaluate(GradingEvaluationRequest request)` 和分数数组计算方法，用于纯指标计算和既有单元测试。
- HTTP 入口 `evaluate(currentUserId, request)` 强制 course scope。

## 2. Non-functional Requirements

- 不新增依赖。
- 不新增 DB migration。
- 不修改 frontend。
- Controller 只传 current user 和 request，权限在 Service 层。
- 使用 `CourseAccessService` 复用现有 course read 语义。
- 需要 TDD RED、focused、adjacent、full backend Maven 验证。

## 3. Out of Scope

- JWT/RBAC claim。
- Evaluation set 自动运行。
- 样本从数据库加载。
- 教师班级成员矩阵扩展。
