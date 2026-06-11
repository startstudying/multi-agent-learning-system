# RUN-20260608 后端 P3 生产化 Security & Quality

## 角色

Security & Quality Expert

## 当前保护面事实

- 当前身份模型仍是 `DevAuthFilter` 读取 `X-User-Id`，`CurrentUserService` 通过字符串判断 `admin` 和 `teacher_*`。
- Learner profile、learning path owner、RAG mixed `kbIds` strict deny、RAG document detail/reindex/index task、resource generation task/trace、Review Gate course scope 已有最小权限收口。
- Assessment answer submit 只校验 `userId == request.learnerId()`。
- `GET /api/courses`、`GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph` 当前未按 current user scope 过滤。
- `POST /api/assessment/grading-evaluations` 当前未校验 teacher/admin。

## 主要缺口

1. 真实 JWT/RBAC 未完成；`X-User-Id` 可伪造。
2. Course read/list/knowledge graph 未做课程范围授权。
3. RAG document upload 可携带任意 `courseId/chapterId` 元数据。
4. Learning path/resource generation 只校验 learner owner，未校验 goal/course/class 范围。
5. Assessment answer submit 未校验 question/course/path ownership。
6. Grading evaluation 接口无权限保护。

## 当前切片建议

P3-4-C 最小可落地范围：

- `GET /api/courses` scoped list。
- `GET /api/courses/{courseId}` scoped detail。
- `GET /api/courses/{courseId}/knowledge-graph` scoped graph。
- `POST /api/assessment/grading-evaluations` teacher/admin only。
- 非 admin missing 与 foreign course detail/graph 返回同类 `FORBIDDEN`，且不返回 `data`。

## 后续切片保留

- 真实 JWT/RBAC。
- class/enrollment 表和矩阵。
- RAG document upload course/chapter scope。
- learning path/resource generation goal/course scope。
- answer submission question scope。
