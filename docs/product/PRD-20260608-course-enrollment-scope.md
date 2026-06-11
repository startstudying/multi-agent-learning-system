# PRD-20260608 P3-4-D Course Enrollment Scope

## 背景

当前课程读写、学习路径、资源生成和教师班级分析已经有若干对象级权限切片，但学生与课程之间缺少真实授权关系。学生端目前不能列出课程，学习路径和资源生成只检查 learner owner，教师班级分析通过 `learning_path.goalId == courseId` 推断班级成员。

这会导致两个问题：

1. 学生一旦知道课程 ID，可能通过学习路径或资源生成侧门使用未授权课程知识图谱。
2. 教师班级分析可能把未授权但创建了 learning path 的学生纳入班级统计。

## 目标

建立最小 `course_enrollment` 授权域，让课程读取、学习路径、资源生成和教师班级分析具备同一 enrollment source of truth。

## 非目标

- 不实现 JWT / OAuth / 完整 RBAC。
- 不新增前端页面。
- 不新增选课管理 API。
- 不实现 answer record 详情/list API；该矩阵放入下一 P3-4 切片。
- 不修改 RAG KB ACL 与课程授权联动。

## 用户价值

- 学生只能读取已选课程。
- 学生只能为已选课程创建课程 DAG 学习路径和资源生成任务。
- 教师班级分析只基于正式 enrollment，不再受旧 learning path 污染。

## 验收摘要

- 存在 `course_enrollment` schema/entity/repository。
- 学生 course list/detail/graph 只返回 active enrollment course。
- 未 enrollment 学生创建 course-bound path/resource 返回 `FORBIDDEN` 且无 `data`。
- teacher class summary learner set 来自 enrollment。
- focused/adjacent/backend 测试有证据。
