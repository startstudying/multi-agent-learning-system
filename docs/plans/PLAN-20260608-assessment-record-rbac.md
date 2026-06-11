# PLAN-20260608 P3-4-E Assessment Record RBAC Matrix

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 Spec-First 工作流 |
| object-scope-authorization | answerId / wrongQuestionId 对象级授权和防枚举 |
| assessment-feedback-agent | assessment 数据对象、评分、错题、掌握度边界 |
| java-security-review / security-review | 权限绕过与敏感学习数据泄露复核 |
| test-driven-development | 先写 RED 权限测试 |
| verification-before-completion | 用真实测试结果更新证据 |

GitHub Research Needed: No

New Project-Specific Skill: 暂不新增；若本切片沉淀通用模式，更新 `object-scope-authorization`。

## 2. Subagent Decision

- Use Subagents: Yes
- Reason: 涉及权限、安全和 teacher/student/admin 矩阵。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - Security & Quality: 已回收 assessment record RBAC 只读复核。
  - Backend Expert: 并行分析实现落点。
- Implementation Mode: Main Codex 单线程实现。

## 3. Implementation Steps

1. [x] RED：新增 student foreign/missing answer detail 防枚举测试。
2. [x] RED：新增 teacher own-course active enrollment answer detail 200、foreign/missing 403 测试。
3. [x] RED：新增 admin any/missing answer detail 测试。
4. [x] RED：新增 wrong question detail 复用授权语义测试。
5. [x] GREEN：新增安全 DTO。
6. [x] GREEN：新增 repository 查询方法和必要 domain getter。
7. [x] GREEN：在 `AssessmentService` 实现详情查询和授权辅助。
8. [x] GREEN：在 `AssessmentController` 暴露两个 GET 端点。
9. [x] 验证 focused / adjacent / full backend Maven。
10. [x] 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill。

## 6. Verification Result

- Focused：`mvn --% -Dtest=AssessmentControllerTest test`，13 tests，0 failures，0 errors，0 skipped。
- Adjacent：`mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test`，53 tests，0 failures，0 errors，0 skipped。
- Full backend：`mvn test`，316 tests，0 failures，0 errors，1 skipped。

## 7. Architecture Drift Result

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller delegates to `AssessmentService`; authorization stays in service. |
| Frontend rules | PASS | No frontend changes. |
| Agent / RAG rules | PASS | No Agent/RAG execution changes. |
| Security | PASS | Backend object authorization, anti-enumeration, and DTO whitelist are covered. |
| API / Database | PASS | SPEC documents new GET endpoints; no schema/migration change. |

## 4. Risks

| Risk | Mitigation |
|---|---|
| `AnswerRecord` 无 `courseId` | 通过 `questionId -> knowledgePointId -> KnowledgePoint.courseId` 推导；推导失败不放行 teacher |
| 过渡角色模型不是真实 RBAC | 在 Acceptance/TODO 明确保留真实 JWT/RBAC |
| 响应泄露 `requestHash` / `responseJson` | 使用专用 DTO 白名单 |
| 列表接口扩大数据面 | 本切片不做 list |

## 5. Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
mvn test
```
