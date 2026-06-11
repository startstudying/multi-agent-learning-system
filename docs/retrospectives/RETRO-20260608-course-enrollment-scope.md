# RETRO-20260608 P3-4-D Course Enrollment Scope

## What Changed

- 将课程访问授权从分散的 service 判断收敛到 `CourseAccessService`。
- 用 `course_enrollment.status == ACTIVE` 作为学生课程 list/detail/graph、course-bound learning path、course-bound resource generation、teacher class summary learner set 的授权事实来源。
- 保留既有临时角色模型：`admin`、`teacher` / `teacher_*`、其他用户作为 student。
- 保留非 course goal 的原有学习路径/资源生成行为。

## What Went Well

- 先建立了 P3-4-D 的 PRD / REQ / SPEC / PLAN / TASK / Context Pack，再进入实现。
- 测试覆盖了 active / dropped / missing enrollment、course goal 与 template goal 兼容、teacher summary 不再从 legacy path 推断 learner set。
- 安全和测试 subagent 并行给出了边界审查，主线程按 SPEC 解决错误语义与范围蔓延风险。

## What Did Not Go Well

- 早期工具层 shell / Node REPL / Maven 曾被 `windows sandbox: setup refresh failed` 阻断；后续已用可运行的 Maven 命令补齐 focused / adjacent / full backend 验证。
- 部分文档曾在默认 PowerShell 输出下显示 mojibake，后续改用 UTF-8 读取并用小补丁更新状态。

## Follow-up

- P3-4 后续仍需单独实现 answer record 详情/list RBAC 矩阵，不应把 P3-4 整体标为完成。
- 若后续引入 enrollment 管理 API，应新建独立 Spec-first 切片，不在本切片扩展。
