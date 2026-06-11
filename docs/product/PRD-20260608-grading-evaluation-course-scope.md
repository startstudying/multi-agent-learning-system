# PRD-20260608 P3-4-G Grading Evaluation Course Scope

## 1. 背景

P2-3 已提供自动批改质量评估接口 `POST /api/assessment/grading-evaluations`，P3-4-C 已先把该接口限制为 teacher/admin 可用。

当前遗留问题是：接口没有课程授权锚点，teacher 只要具备临时 teacher 身份就能提交任意 `knowledgePointId` 的离线样本评估，无法限制为自己课程范围。这与已经完成的 course read、assessment record detail/list RBAC 不一致。

## 2. 目标

本切片将 grading evaluation 从“角色门禁”收口为“课程范围门禁”：

- 请求必须提供 `courseId`。
- teacher 只能对自己课程运行 grading evaluation。
- admin 可对任意存在课程运行 grading evaluation。
- student 仍不可运行。
- 样本中的 `knowledgePointId` 必须属于请求 `courseId`，避免跨课程样本混入。
- 非 admin 对 missing/foreign course 返回同类安全 `FORBIDDEN`，避免对象存在性探测。

## 3. 非目标

- 不实现真实 JWT/RBAC。
- 不新增 DB migration。
- 不修改 frontend。
- 不新增依赖。
- 不接入 evaluation set runner。
- 不改 grading metric 公式。
- 不改 answer submission、wrong-question、mastery 或 replan 流程。

## 4. 用户价值

- 教师只能评估自己课程的批改质量，防止跨课程数据误用。
- 管理员仍保留全局评估能力。
- 质量评估接口与现有课程/答题记录权限矩阵保持一致。

## 5. 验收摘要

- `GradingEvaluationRequest` 支持并要求 `courseId`。
- teacher own-course 请求成功。
- teacher missing/foreign course 返回 `FORBIDDEN` 且无 `data`。
- admin missing course 返回 `NOT_FOUND`。
- student 即使传合法课程也返回 `FORBIDDEN`。
- 样本 `knowledgePointId` 不属于 course 时拒绝。
- Focused / adjacent / full backend Maven 验证完成并写入 Evidence。
