# Review Gate 审核权限加固需求

## 功能需求

| 编号 | 需求 | 优先级 | 验收方式 |
|---|---|---|---|
| FR-01 | `GET /api/reviews/resources` 必须校验当前用户是否可审核资源。 | 必须 | student 返回 403，teacher/admin 返回 200 |
| FR-02 | `POST /api/reviews/resources/{reviewId}/decision` 必须校验当前用户是否可审核资源。 | 必须 | student 返回 403，teacher/admin 返回 200 |
| FR-03 | 未授权时不得查询 review/task/resource 详情。 | 必须 | service guard 在 repository lookup 前执行 |
| FR-04 | 临时权限契约为 `X-User-Id` 等于 `teacher` 或 `admin`。 | 必须 | Controller 测试覆盖 teacher/admin/student |
| FR-05 | 未授权响应不得泄露审核对象详情。 | 必须 | 403 body `data == null` 且不包含 review/task/resource id |

## 非功能需求

- 不新增依赖。
- 不新增数据库字段。
- 保持现有 API path 和响应 envelope。
- 权限检查必须在后端代码中完成，不能依赖 Prompt 或前端隐藏按钮。

## Open Issues

- 当前策略只是开发期临时规则，不是真实 RBAC。
- 后续需要用户角色模型、课程/班级授权关系，并将教师访问范围限制到授权教学数据。
