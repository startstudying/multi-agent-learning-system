# TASK-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. Goal

移除服务层残留的 subject-name role inference 兼容入口，使课程目录、答题记录、自动评分评估相关 Service 的权限语义只来自显式 role facts。

## 2. Task Type

Security / RBAC cleanup；Refactor + regression tests。

## 3. Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目规则从需求走文档、实现、验证、证据闭环。 |
| `subagent-driven-development` | 用户要求专家 subagent 并行开发；本任务采用并行分析。 |
| `test-driven-development` | 用反射守卫固定 legacy overload/helper 不回归。 |
| `security-review` | 清理 subject-name role confusion 风险。 |
| `auth-context-boundary` | 角色必须来自 `UserContext.roles()`，不能来自 subject 字符串。 |
| `object-scope-authorization` | 课程与 assessment record 属于对象级权限边界。 |
| `verification-before-completion` | 完成前必须跑真实测试并记录证据。 |

## 4. Size Decision

- Size: M
- Required docs: REQ / SPEC / PLAN / TASK / CONTEXT
- PRD: 不需要
- Reason: 涉及三个 Service 与多组权限测试，安全风险高于 S；不涉及 API/DB/依赖/前端，低于 L。

## 5. Subagent Plan

| Expert | Scope | Output |
|---|---|---|
| Security Reviewer | 审查 legacy subject-name role inference 删除边界 | `docs/subagents/runs/RUN-20260610-p3-4-service-legacy-subject-auth-cleanup-security.md` |
| Test Engineer | 审查反射守卫与测试矩阵 | `docs/subagents/runs/RUN-20260610-p3-4-service-legacy-subject-auth-cleanup-test.md` |

Implementation 由主 Codex 单线完成，避免多个 agent 修改同一 Service 文件。

## 6. Allowed Files

- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`（仅需要补写路径矩阵时）
- `docs/requirements/REQ-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/specs/SPEC-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/plans/PLAN-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/tasks/TASK-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/context/CONTEXT-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/subagents/runs/RUN-20260610-p3-4-service-legacy-subject-auth-cleanup-*.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 7. Disallowed Files

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- Spring Security / OAuth2 / JWK 配置文件
- RAG parser、VectorDB、model gateway、Review Gate、ResourceGeneration 非目标模块
- 任意 secret、credential、local env 文件

## 8. Implementation Checklist

- [x] 删除 `KnowledgeCatalogService` legacy overload/helper。
- [x] 删除 `AssessmentService` legacy overload/helper。
- [x] 删除 `GradingEvaluationService` legacy overload/helper。
- [x] 新增/补齐反射守卫测试。
- [x] focused Maven 测试通过。
- [x] adjacent Maven 测试通过。
- [x] full backend Maven 测试通过，或记录限制。
- [x] Evidence / Acceptance / Changelog / Memory / TODO 更新。

## 9. Acceptance Criteria

- [x] 目标 Service 不再通过 `admin` / `teacher_*` subject-name 推断角色。
- [x] 目标 legacy public overload 不再存在。
- [x] Controller roles-first 调用保持编译通过。
- [x] `GradingEvaluationService` 纯算法入口保留。
- [x] 无 API/DTO/DB/dependency/frontend 改动。
- [x] P3-4 父项仍保持 open，只记录该语义子任务完成。

## 10. Planned Verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest,AssessmentServiceTest,GradingEvaluationServiceTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,AssessmentControllerTest,AssessmentServiceTest,GradingEvaluationServiceTest,AnalyticsControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,RagQueryServiceTest,LearningWorkflowControllerTest test
mvn test
```

## 11. Status

Done.

Evidence / Acceptance:

- `docs/evidence/EVIDENCE-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-service-legacy-subject-auth-cleanup.md`

Verification:

- Compile guard: passed
- Focused: `22 run, 0 failures, 0 errors, 0 skipped`
- Adjacent: `197 run, 0 failures, 0 errors, 0 skipped`
- Full backend: `536 run, 0 failures, 0 errors, 1 skipped`
