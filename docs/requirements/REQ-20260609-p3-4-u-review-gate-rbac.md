# REQ - P3-4-U Review Gate ResourceReview roles-first RBAC

## 功能需求

| ID | Requirement |
|---|---|
| R1 | `GET /api/reviews/resources` 必须从 Bearer `UserContext.roles()` 判断 admin/teacher，而不是从 `userId` 字符串推断。 |
| R2 | `POST /api/reviews/resources/{reviewId}/decision` 必须从 Bearer `UserContext.roles()` 判断 admin/teacher。 |
| R3 | Bearer `ADMIN` 可全局 list/decision，即使请求带 spoofed `X-User-Id`。 |
| R4 | Bearer `TEACHER` 只能 list/decision `ResourceGenerationTask.goalId -> Course.teacherId == token.sub` 的 review，且不要求 `teacher_` 前缀。 |
| R5 | Bearer `USER sub=admin` 不得获得 admin 审核权限。 |
| R6 | Bearer `USER sub=teacher_1` 不得获得 teacher 审核权限，即使存在 `Course.teacherId=teacher_1`。 |
| R7 | 非管理员 missing review 与 foreign review 均返回安全 `FORBIDDEN`，响应不包含 review/task/resource id。 |
| R8 | legacy service 签名可保留兼容，但 HTTP 主路径必须走 roles-first overload。 |

## 质量需求

- 不新增依赖。
- 不修改 API path、request DTO、response DTO。
- 不修改数据库 schema。
- 必须完成 focused、adjacent、full backend Maven 验证或记录限制。
