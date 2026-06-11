# PRD - Review Gate 课程范围收口

状态：已完成（2026-06-07）。

## 1. 背景

当前 Review Gate 已完成学生端最小收口，学生不能绕过审核读取未发布资源。但教师审核仍停留在临时 `teacher/admin` 边界，无法按课程范围限制审核队列。只要知道 `reviewId` 或查看全局列表，教师就可能接触到不属于自己课程的资源审核记录。

## 2. 目标

把 Review Gate 收口到“管理员全局可见，教师仅可处理自己课程的资源审核”。本切片只处理资源审核列表与审核决策，不扩展完整 RBAC，不新增数据库结构。

## 3. 范围

纳入：

- `GET /api/reviews/resources`
- `POST /api/reviews/resources/{reviewId}/decision`
- 基于 `ResourceGenerationTask.goalId -> Course.id -> Course.teacherId` 的课程归属判断
- 教师只能看到自己课程的审核记录
- 教师只能决策自己课程的审核记录
- 管理员保留全局访问
- 越权请求返回安全 `FORBIDDEN`
- 更新单测、文档、证据与验收

不纳入：

- JWT / Session / 角色系统改造
- 完整 teacher/class/course 关系模型重构
- 知识目录、RAG、答题记录的完整 RBAC 矩阵
- 数据库 schema 变更

## 4. 用户价值

- 学生继续被阻断在 Review Gate 之外。
- 教师只能审核自己课程的内容，避免跨课程误操作。
- 管理员仍可做全局治理。

## 5. 成功标准

- 教师无法看到其他课程的审核列表。
- 教师无法对其他课程的 review 做决策。
- 管理员保持原有全局能力。
- 越权响应不泄露 review/resource/task 详情。
- 单测覆盖 teacher/admin/student 三类行为。

## 6. 非目标

本切片不解决完整身份认证，不替换临时 `teacher/admin` 字符串策略，也不扩展到课程/班级之外的所有对象级权限。
