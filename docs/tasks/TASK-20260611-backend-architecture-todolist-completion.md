# TASK-20260611 后端架构 TODO 计划收口

Status: Done.

## Goal

收口 `docs/planning/backend-architecture-todolist.md`：在 MVP / 最小可答辩闭环范围内标记 P0–P3 主计划完成，并将工业级 parser/OCR、Qdrant 真实 smoke、DashScope 专用 provider、持续权限矩阵抽样等后续增强拆到独立 follow-up 区。

## Task Type

文档 / 计划状态收口；不新增功能代码。

## Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 执行后端架构总计划收口 |
| verification-before-completion | 勾选计划完成前必须有当前测试证据 |
| auth-permission | 判断 P3-4 权限矩阵是否达到 MVP 完成边界 |
| springboot-verification | 运行 full backend 测试作为收口证据 |

## Size Classification

Size: S。

Reason: 只回填计划状态、拆分 follow-up、记录证据；不改后端代码、API、DB schema、依赖或前端。

## Context Pack

### Related Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-formal-production-streaming-design.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-teacher-permission-residual-sampling-matrix.md`

### Files Allowed To Modify

- `docs/planning/backend-architecture-todolist.md`
- `docs/tasks/TASK-20260611-backend-architecture-todolist-completion.md`
- `docs/evidence/EVIDENCE-20260611-backend-architecture-todolist-completion.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

### Files Not Allowed To Modify

- `backend/src/main/**`
- `backend/src/test/**`
- `backend/pom.xml`
- `frontend/**`

## Acceptance Criteria

- P0、P1、P2、P3-1、P3-2（最小生产化）、P3-3（最小 provider 边界）、P3-4（MVP 权限矩阵）、P3-5 在 TODO 中标记完成。
- P3-4 三个残余 unchecked 项在 MVP 范围内标记完成，并明确“持续业务矩阵抽样”为 follow-up。
- 工业级 parser/OCR、Qdrant 真实 smoke、DashScope 专用增强等移入“后续增强”区，不阻塞计划完成。
- full backend 测试证据记录为 `601 run, 0 failures, 0 errors, 1 skipped`。
- Changelog 与 Memory 同步更新。

## Verification

```powershell
cd D:\多元agent\backend
mvn test
```

Result: `Tests run: 601, Failures: 0, Errors: 0, Skipped: 1` — BUILD SUCCESS。
