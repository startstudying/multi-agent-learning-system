# REQ-20260608 P3-4-C 权限矩阵安全前置

| ID | Requirement | Priority |
|---|---|---|
| REQ-RBAC-C-1 | `GET /api/courses` 对 `admin` 返回全部课程。 | P0 |
| REQ-RBAC-C-2 | `GET /api/courses` 对 teacher 只返回 `teacherId == currentUserId` 的课程。 | P0 |
| REQ-RBAC-C-3 | `GET /api/courses` 对普通 student 暂返回空列表，直到 class/enrollment 模型落地。 | P0 |
| REQ-RBAC-C-4 | `GET /api/courses/{courseId}` 对 authorized teacher/admin 返回课程详情。 | P0 |
| REQ-RBAC-C-5 | `GET /api/courses/{courseId}` 对 foreign teacher/student 与 missing course 的非 admin 响应同类 `FORBIDDEN`，且无 `data`。 | P0 |
| REQ-RBAC-C-6 | `GET /api/courses/{courseId}/knowledge-graph` 复用课程读取授权。 | P0 |
| REQ-RBAC-C-7 | `POST /api/assessment/grading-evaluations` 只允许 admin/teacher，普通 student 返回 `FORBIDDEN`。 | P0 |
| REQ-RBAC-C-8 | Controller 只传 current user；权限判断必须在 Service 层。 | P0 |
| REQ-RBAC-C-9 | 不新增依赖、不改 schema、不改前端。 | P0 |
