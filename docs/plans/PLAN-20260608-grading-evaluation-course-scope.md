# PLAN-20260608 P3-4-G Grading Evaluation Course Scope

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 Spec-First 工作流。 |
| object-scope-authorization | course scope、missing/foreign 防枚举和 teacher/admin/student 矩阵。 |
| assessment-feedback-agent | grading evaluation 属于 assessment 评估数据面。 |
| java-security-review / security-review | IDOR、权限绕过、敏感响应复核。 |
| multi-agent-coder | 用户要求专家 subagent 并行开发，本切片使用 L1 并行分析。 |
| test-driven-development | 先写 RED 权限矩阵测试。 |
| verification-before-completion | 用真实测试输出证明完成状态。 |
| Confidence Check | 实现前确认重复、架构和根因。 |

Missing Skills: 无。

GitHub Research Needed: No。不新增依赖，不接入外部 API。

New Project-Specific Skill: 不新建；完成后更新 `object-scope-authorization`。

## 2. Subagent Decision

- Use Subagents: Yes
- Reason: 涉及权限、安全、course scope 和 assessment API 行为变更。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - Backend Expert: 当前实现与最小改动边界。
  - Security & Quality: 权限矩阵、missing vs foreign、对象枚举风险。
  - Integration Reviewer: 合并专家结论并固化最终实施边界。
- Implementation Mode: Main Codex 单线程实现。

## 3. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 当前 `GradingEvaluationService` 只有 teacher/admin gate，无 course scope。 |
| Architecture compliance | PASS | 复用 `CourseAccessService` 与 Service-layer authorization。 |
| Official docs needed | PASS | 无外部 API / SDK / 新依赖。 |
| OSS references needed | PASS | 不需要 GitHub reference。 |
| Root cause identified | PASS | `GradingEvaluationRequest` 无 `courseId`，teacher 无法绑定 own-course。 |

Confidence: 0.94

## 4. Implementation Steps

1. [x] RED：新增 teacher 不带 `courseId`、student 带 course、teacher foreign/missing course、admin missing course、sample KP outside course 测试。
2. [x] GREEN：`GradingEvaluationRequest` 增加 `courseId` 并保留现有构造器兼容。
3. [x] GREEN：`GradingEvaluationService` 注入 `CourseAccessService` / `KnowledgePointRepository`，实现 course gate 和 sample KP 校验。
4. [x] GREEN：更新既有 controller/service 测试数据，使 teacher success 请求显式绑定 own course。
5. [x] 验证 focused / adjacent / full backend Maven。
6. [x] 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill。

## 5. Risks

| Risk | Mitigation |
|---|---|
| 旧 teacher 请求不带 `courseId` 被拒绝 | SPEC 明确 HTTP 入口强制 course scope；纯指标 service 方法保留兼容。 |
| sample KP 不存在与 foreign KP 形成枚举差异 | 统一返回 `VALIDATION_ERROR`，不暴露具体 KP/course 信息。 |
| Service 构造器变化破坏单元测试 | 为指标-only 使用保留无参构造器或在测试中传 mock。 |
| API 行为变化需前端适配 | 本切片不改前端，但 API memory/changelog 记录 `courseId` required。 |

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```
