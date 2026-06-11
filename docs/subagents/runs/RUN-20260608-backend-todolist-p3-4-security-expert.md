# RUN-20260608 backend TODO P3-4 Security Expert

## 结论

P3-4 最大缺口是 class/enrollment 数据域缺失和 answer record 读取矩阵缺失。当前身份仍是 dev header + string role，已有多个对象级权限切片，但还不是生产级 RBAC/JWT。

## 关键证据

- `CurrentUserService` 仍通过 `X-User-Id` 和硬编码 `admin/teacher_*` 识别角色。
- Course/Knowledge 当前有 teacher own-course scope，student list/detail/graph 仍是保守拒绝或空列表。
- Learning Path 和 Resource Generation 只检查 learner owner，未检查 `goalId` 对应 course enrollment。
- Teacher class summary 当前从 `LearningPath.goalId == courseId` 推断班级成员，不是真实 enrollment。
- Assessment submit 已有 owner 检查，但 answer record 没有独立读 API 和 teacher/student/admin 矩阵。

## 推荐

优先实施 P3-4-D：最小 `course_enrollment` 授权域。

本切片先做：

- `course_enrollment` schema/entity/repository。
- `CourseAccessService` 集中处理 course read/manage/enrollment 判断。
- Student course list/detail/graph 只允许 active enrollment。
- Learning Path / Resource Generation 对真实 course goal 要求 learner active enrollment。
- Teacher class summary learner set 改为 active enrollment，不再从 learning path 推断。

Answer record 读矩阵作为下一 P3-4 切片，避免同一任务同时新增 enrollment 域与 assessment 读 API。
