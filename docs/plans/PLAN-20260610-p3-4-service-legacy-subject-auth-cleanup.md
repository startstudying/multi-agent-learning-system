# PLAN-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. 任务概述

清理 `KnowledgeCatalogService`、`AssessmentService`、`GradingEvaluationService` 中仍通过 subject 字符串推断角色的 legacy overload/helper，防止后续内部调用或 Agent/Orchestrator 调用绕过 HTTP roles-first 边界。

## 2. Skill Selection Report

### Task Type

Security / RBAC cleanup；Refactor + regression tests。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 用户要求完成 backend TODO 计划，项目规定必须走 S/M/L 工作流。 |
| `subagent-driven-development` | 用户明确要求专家 subagent 并行开发；本任务启用安全与测试专家并行分析。 |
| `test-driven-development` | 删除 legacy overload/helper 前后需要反射守卫和回归测试。 |
| `security-review` | 本任务直接处理 subject-name role-confusion 风险。 |
| `auth-context-boundary` | Runtime role semantics 必须来自 `UserContext.roles()` 显式事实。 |
| `object-scope-authorization` | 课程、答题记录、评分评估均属于对象级权限边界。 |
| `verification-before-completion` | 完成前必须基于真实 Maven 输出生成证据。 |

### Missing Skills

无。

### GitHub Research Needed

No。该任务为项目内部 RBAC 边界清理，不需要外部参考或新增依赖。

### New Project-Specific Skill To Create

暂不需要；已有 `auth-context-boundary` 与 `object-scope-authorization` 覆盖。

## 3. Size Classification

- Size: M - Standard Feature Slice
- Reason: 涉及三个后端 Service 和多组权限回归测试，有安全集成风险；但不改 API/DTO/DB/schema/dependency/frontend，不属于 L 级架构改造。
- Required Documents: `REQ` / `SPEC` / `PLAN` / `TASK` / `CONTEXT`
- PRD: 不需要；用户可见产品行为不变。
- Can Skip: L 级 PRD、ADR、dependency review、frontend design。
- Upgrade Trigger: 如果发现 Controller/API 合同、数据库 schema、正式认证架构或跨模块 Agent/RAG 调用链必须变更，则停止并升级为 L。

## 4. Subagent Decision

- Use Subagents: Yes
- Reason: 用户明确要求专家 subagent 并行开发；本任务为 M 级安全/RBAC 清理。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - Security Reviewer：审查 legacy subject-name role inference 清理边界。
  - Test Engineer：审查反射守卫、focused/adjacent 测试矩阵。
- Implementation Mode: 主 Codex 单线实现；专家只读分析，不并行改同一文件。

## 5. 实施步骤

1. 创建 M 级工作流文档和 Context Pack。
2. 等待专家报告并落盘到 `docs/subagents/runs/`。
3. 删除 `KnowledgeCatalogService` legacy overload/helper。
4. 删除 `AssessmentService` legacy overload/helper。
5. 删除 `GradingEvaluationService` legacy overload/helper。
6. 补反射守卫测试。
7. 运行 focused 测试。
8. 运行 adjacent 测试。
9. 运行 full backend 测试。
10. 更新 Evidence、Acceptance、Changelog、Memory、TODO。

## 6. 风险与控制

| Risk | Control |
|---|---|
| 删除 overload 导致隐藏调用方编译失败 | 先用 `rg` 搜索调用方，再运行 Maven 编译/测试。 |
| HTTP 行为意外变化 | Controller 不改 API，只保留现有 roles-first 调用；跑邻接 Controller 测试。 |
| 纯算法评估入口被误删 | 明确保留 `evaluate(GradingEvaluationRequest)` 和 `evaluate(List<Double>, List<Double>, double)`。 |
| P3-4 父项被错误关闭 | 只更新子任务状态说明，不勾选父项剩余开放项。 |

## 7. Test Plan

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest,AssessmentServiceTest,GradingEvaluationServiceTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,AssessmentControllerTest,AssessmentServiceTest,GradingEvaluationServiceTest,AnalyticsControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,RagQueryServiceTest,LearningWorkflowControllerTest test
```

Full:

```powershell
cd D:\多元agent\backend
mvn test
```

## 8. Architecture Drift 前置检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 只修改 Application Service 与测试。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；清理可降低后续内部调用风险。 |
| Security | PASS | 权限语义从 subject-name 推断迁移到显式 role facts。 |
| API / Database | PASS | 不改 API/DB。 |

## 9. Post-Implementation Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 只修改 Application Service 与测试；Controller 仍只提取 HTTP 当前用户并委托 Service。 |
| Frontend rules | PASS | 未修改 frontend。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime；服务层 legacy 清理降低未来内部调用风险。 |
| Security | PASS | 目标服务不再通过 subject-name 推断 admin/teacher。 |
| API / Database | PASS | 未修改 API/DTO/DB/schema/dependency。 |

## 10. 当前状态

Done。

- Evidence: `docs/evidence/EVIDENCE-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- Acceptance: `docs/acceptance/ACCEPT-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- Full backend verification: `536 run, 0 failures, 0 errors, 1 skipped`
