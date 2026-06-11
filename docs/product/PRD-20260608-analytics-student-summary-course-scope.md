# PRD-20260608 学生分析摘要课程范围权限收口

## 1. 背景

`GET /api/analytics/students/{learnerId}/summary` 当前只允许学生本人读取自己的全局学习摘要。这满足最小 owner-only 防护，但没有支持教师在授权课程内查看已报名学生的课程内学习摘要，也没有定义可选 `courseId` 时的课程范围过滤。

P3-4 仍要求继续扩展 broader class/course 权限矩阵。本切片聚焦 analytics student summary，补齐“教师端只能访问授权课程数据”和“学生端只能访问自己的课程数据”的一个小而可验收的后端切片。

## 2. 目标

- 学生本人继续可以读取自己的全局学习摘要。
- 学生本人提供 `courseId` 时，只能读取自己 active enrolled course 的课程内摘要。
- 教师读取学生摘要时必须提供 `courseId`，且只能读取自己课程内 active enrolled learner 的课程内摘要。
- 管理员可读取任意学生摘要；提供 `courseId` 时读取课程内摘要。
- 课程内摘要不得混入其他课程的 path node、mastery、wrong question 信号。

## 3. 非目标

- 不新增数据库迁移。
- 不新增前端页面。
- 不接入正式 OAuth2/JWK/Spring Security。
- 不重写全部 analytics 聚合查询为 repository scoped query；本切片允许沿用现有内存过滤，但必须记录后续优化边界。
- 不改变 teacher class summary 现有接口。

## 4. 用户价值

- 教师可以在课程范围内查看单个学生学习摘要，不再只能看班级聚合。
- 学生和教师都不会通过 analytics summary 越权读取其他课程或其他学生数据。
- P3-4 权限矩阵向 class/course 生产化继续推进。

## 5. 验收摘要

- teacher 无 `courseId` 读取学生摘要返回 `VALIDATION_ERROR`。
- teacher 读取 own course active enrolled learner 返回课程内摘要。
- teacher 读取 foreign course 或未报名 learner 返回 `FORBIDDEN`。
- student 读取 foreign learner 返回 `FORBIDDEN`。
- student 带未报名/不存在 `courseId` 返回 `FORBIDDEN`。
- admin 可读取全局或课程内摘要；admin 带 missing `courseId` 返回 `NOT_FOUND`。
