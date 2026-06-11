# 教师端 Class Summary Subagent 集成记录

## 背景

本切片用于补齐 `docs/planning/backend-architecture-todolist.md` 中 P1-5 的教师端 class summary 能力。此前已启动三个只读 subagent 分析：

- P0-1 Orchestrator DTO / failure / retry 策略分析
- P1-2 Knowledge DAG 与路径规划增强分析
- P1-5 教师端 Class Summary 后端分析

## 结论

本次只实现 P1-5 的后端最小切片，不继续新开 subagent，不并行修改代码。

## 采用的分析结果

- 当前没有 `Class`、`Enrollment` 或 `ClassMember` 表，不能声称具备真实班级成员模型。
- 最小实现使用 `Course.teacherId` 作为教师权限边界。
- 课程学习者集合暂由 `LearningPath.goalId == courseId` 推断。
- API 只返回聚合数据和待审核资源元数据，不返回学生原始答案、完整 prompt、LLM 输出正文或未审核资源正文。
- 复用现有 Analytics 模块，避免新依赖和数据库迁移。

## 冲突处理

- subagent 建议路径包括 `/api/analytics/classes/{courseId}/summary` 和 `/api/analytics/teachers/classes/{courseId}/summary`。为贴近现有 analytics 路由，采用 `/api/analytics/classes/{courseId}/summary`。
- 完整 RBAC 和课程/班级授权关系不在本切片实现，记录为 P3 权限加固后续项。
