# PRD-20260608 P3-4-F Assessment Record List RBAC / Pagination

## 1. 背景

P3-4-E 已完成 assessment answer / wrong-question 详情端点的对象级权限矩阵，但 P3-4 TODO 仍保留 answer record list / pagination RBAC 缺口。

如果后续直接开放答题记录列表，容易出现：

- student 通过 `learnerId` 查询他人答题记录。
- teacher 跨课程查看其他教师课程学生记录。
- list 接口返回过多原始答案、幂等快照或内部 payload。
- 无分页或无 scope 查询导致全表枚举。

## 2. 目标

新增最小安全列表能力：

- 学生只能分页查看自己的 answer / wrong question。
- 教师只能分页查看自己课程 active enrollment 学生的课程相关 answer / wrong question。
- 管理员可分页查看全局或按 learner/course 过滤的记录。
- 列表响应使用安全 summary DTO，不返回原始 answer、`requestId`、`requestHash`、`responseJson`、`payloadJson`。
- 所有列表都有 page / size 上限，避免无限列表。

## 3. 非目标

- 不实现真实 JWT/RBAC。
- 不新增 DB migration，不给 assessment 记录冗余 `courseId`。
- 不修改前端。
- 不新增依赖。
- 不改变答题提交、评分、掌握度更新、路径重规划逻辑。
- 不实现 grading evaluation course scope。

## 4. 用户价值

- 学生可安全查看自己的答题历史和错题本列表。
- 教师可查看授权课程下学生的答题/错题列表，用于班级诊断。
- 管理员可进行分页运维排查，不需要全量加载。

## 5. 验收摘要

- 新增 answer list 与 wrong-question list API。
- student / teacher / admin 列表矩阵有自动化测试。
- teacher 读取必须绑定 `courseId`，并依赖 own-course + active enrollment。
- 响应 DTO 不包含敏感内部字段。
- Focused / adjacent / full backend Maven 验证通过并写入 Evidence。
