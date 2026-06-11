# PRD-20260608 P3-4-E Assessment Record RBAC Matrix

## 1. 背景

P3-4 已完成课程/知识图谱读写、对象详情防枚举、Course Enrollment Scope 等最小权限切片，但答题记录、评分结果、错题记录等 assessment 派生数据还没有独立读取矩阵。

当前公开 assessment 端点只有：

- `POST /api/assessment/answers`
- `POST /api/assessment/grading-evaluations`

答题提交写路径已有 `currentUserId == learnerId` owner gate，但如果后续直接新增详情或列表接口，容易形成 answerId / wrongQuestionId 的 IDOR 枚举风险。

## 2. 目标

建立 assessment record 最小只读详情矩阵：

- 学生只能读取自己的答题详情和错题详情。
- 教师只能读取自己课程 active enrollment 学生的课程相关答题/错题详情。
- 管理员可读取全局详情，并能区分真实 missing。
- 非管理员访问 foreign / missing 对象统一返回 `FORBIDDEN`，不暴露对象存在性。

## 3. 非目标

- 不实现 answer list / wrong question list / pagination。
- 不实现真实 JWT/RBAC。
- 不新增 DB migration，不给 assessment 记录补 `courseId`。
- 不改答题提交幂等、评分、掌握度更新、路径重规划逻辑。
- 不暴露 `requestHash`、`responseJson` 原文、`LearningEvent.payloadJson` 原文。
- 不新增依赖。

## 4. 用户价值

- 学生可安全查看自己的答题反馈详情。
- 教师可在授权课程范围内查看学生错题/评分详情。
- 管理员可运维排查特定答题记录。
- 后续列表接口和教师工作台可复用相同授权边界。

## 5. 验收摘要

- 新增详情端点具备 student / teacher / admin 行为矩阵测试。
- 非管理员 missing 与 foreign 响应同形：`FORBIDDEN` 且无 `data`。
- 响应 DTO 不包含敏感内部字段。
- 现有答题提交、grading evaluation、analytics 权限测试继续通过。
