# PLAN-20260608 P3-4-F Assessment Record List RBAC / Pagination

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 Spec-First 工作流 |
| object-scope-authorization | assessment list 对象范围、课程父级 scope、防越权 |
| assessment-feedback-agent | answer / wrong-question / grading 数据边界 |
| java-security-review / security-review | IDOR、列表枚举、敏感字段泄露复核 |
| test-driven-development | 先写 RED 权限矩阵测试 |
| verification-before-completion | 用真实测试结果更新证据 |

GitHub Research Needed: No

New Project-Specific Skill: 暂不新增；完成后更新 `object-scope-authorization`。

## 2. Subagent Decision

- Use Subagents: Yes
- Reason: 涉及权限、安全、分页列表和 assessment 数据面扩大。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - Backend / Spec Architect: list API 与 scoped repository 查询设计。
  - Security & Quality: IDOR/list 枚举风险复核。
  - Integration Reviewer: Spec-First 文档与验收边界。
- Implementation Mode: Main Codex 单线程实现。

## 3. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 当前只有 detail API，无 answer/wrong-question list API。 |
| Architecture compliance | PASS | 复用 Controller -> Service -> Repository 和 `CourseAccessService`。 |
| Official docs needed | PASS | 使用既有 Spring Data JPA repository/page 模式，无新增外部 API。 |
| OSS references needed | PASS | 不新增依赖，不需要 GitHub reference。 |
| Root cause identified | PASS | P3-4 TODO 明确 answer record list RBAC 缺口。 |

Confidence: 0.95

## 4. Implementation Steps

1. [x] RED：新增 student answer/wrong-question list owner-only 测试。
2. [x] RED：新增 teacher 必须带 `courseId`，且只能看 own-course active enrollment learner list 测试。
3. [x] RED：新增 admin global/filter list 测试。
4. [x] RED：新增非法 page/size 与敏感字段不暴露测试。
5. [x] GREEN：新增 list summary / page DTO。
6. [x] GREEN：新增 repository scoped page 查询方法。
7. [x] GREEN：在 `AssessmentService` 实现 list scope、分页校验和 summary 映射。
8. [x] GREEN：在 `AssessmentController` 暴露两个 GET list 端点。
9. [x] 验证 focused / adjacent / full backend Maven。
10. [x] 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill。

## 5. Risks

| Risk | Mitigation |
|---|---|
| `AnswerRecord` 无 `courseId` | 通过 course knowledge points 反推出当前 questionId 集合；不做全表无 scope 暴露。 |
| teacher list 形成 learner 枚举 | teacher 查询未 enrolled learner 返回空 page；foreign/missing course 返回安全 `FORBIDDEN`。 |
| 列表泄露原始 answer 或快照 | 使用 summary DTO，不返回 `answer/requestId/requestHash/responseJson/payloadJson`。 |
| 无界分页 | `size` 限制为 `1..50`。 |

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
mvn test
```
