# REQ-20260608 P3-4-D Course Enrollment Scope

## 功能需求

| ID | Requirement |
|---|---|
| REQ-1 | 系统必须新增 `course_enrollment` 作为 learner 与 course 的 active enrollment 授权关系。 |
| REQ-2 | `GET /api/courses` 对 student 只返回 active enrolled courses。 |
| REQ-3 | `GET /api/courses/{courseId}` 对 student 只允许读取 active enrolled course。 |
| REQ-4 | `GET /api/courses/{courseId}/knowledge-graph` 对 student 复用同一 course read scope。 |
| REQ-5 | `POST /api/learning-paths` 当 `goalId` 是存在的 courseId 时，非 admin learner 必须 active enrolled。 |
| REQ-6 | `POST /api/resources/generation-tasks` 当 `goalId` 是存在的 courseId 时，learner 必须 active enrolled。 |
| REQ-7 | `GET /api/analytics/classes/{courseId}/summary` 的 learner set 必须来自 active enrollment，不再从 learning path 推断。 |
| REQ-8 | 非 admin missing/foreign course detail/graph 继续返回安全 `FORBIDDEN` 且无 `data`。 |

## 安全需求

- 权限判断必须在 Service 层集中执行。
- Controller 只读取 current user 和请求参数。
- 不泄露 foreign course id、path id、resource task id。
- 不新增依赖。
- 不记录密钥或敏感学习原始数据到 memory/changelog。

## 兼容需求

- 非 course goal（如 `goal_spring_boot`）的学习路径与资源生成保持兼容。
- admin 和 course owner teacher 课程读取能力保持兼容。
- H2 test profile 继续 `ddl-auto=create-drop`，Flyway 默认关闭。
