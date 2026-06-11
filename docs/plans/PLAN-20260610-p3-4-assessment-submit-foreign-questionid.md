# PLAN-20260610-p3-4-assessment-submit-foreign-questionid

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 该任务由 RED 安全测试升级为 M，必须补 REQ/SPEC/PLAN/TASK/CONTEXT、测试、证据和记忆。 |
| `object-scope-authorization` | 需要按 `questionId -> KnowledgePoint -> course -> enrollment` 做对象级授权。 |
| `auth-context-boundary` | Bearer 身份必须优先，spoofed `X-User-Id` 不得提升权限。 |
| `spring-boot-architecture` | 修复应落在 Service 层，保持 Controller 薄。 |
| `test-driven-development` | 已有 RED 测试，修复后转 GREEN。 |

Missing skills：无。

GitHub research needed：No。该任务使用项目内既有授权服务与测试模式。

New project-specific skill：暂不创建。

## 2. Size Classification

Size：M - Standard Feature Slice。

原因：

- RED 测试暴露真实生产安全缺陷。
- 需要修改 `AssessmentService` 生产代码。
- 影响 assessment submit 写路径和 Orchestrator `ANSWER_SUBMISSION` 间接调用。
- 不涉及 API/DTO/schema/dependency/frontend，因此不升级 L。

Required Documents：

- `docs/requirements/REQ-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/specs/SPEC-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/plans/PLAN-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/tasks/TASK-20260610-p3-4-assessment-submit-foreign-questionid.md`
- `docs/context/CONTEXT-20260610-p3-4-assessment-submit-foreign-questionid.md`

PRD：不需要；这是安全缺陷修复，不改变产品用户流程。

## 3. Subagent Decision

Use Subagents：Yes。

Parallelism Level：L1 Parallel Analysis。

已使用专家：

- Assessment security expert：补充 RED 测试并确认缺陷。
- Evaluation/Review expert：并行复核相邻权限矩阵，避免混淆任务边界。

Implementation Mode：Single Codex implementation。

原因：修复集中在 `AssessmentService` 和同一测试类，不能并行编辑同一文件。

## 4. Implementation Plan

1. 保留专家新增 RED 测试。
2. 在 `AssessmentService.submitAnswerWithTraceId(...)` 中新增提交前 scope 校验。
3. 校验逻辑：
   - 用 `AssessmentFeedbackService.resolveKnowledgePointId(questionId)` 解析知识点 id。
   - 若 `KnowledgePointRepository.findById(...)` 命中且有 `courseId`，调用 `CourseAccessService.requireCourseRead(learnerId, false, false, courseId)`。
   - 若未命中知识点，保留现有 legacy/template 行为。
4. 运行 focused assessment 测试。
5. 运行 adjacent assessment/orchestrator 测试。
6. 运行 full backend 测试；若并行 Maven 互踩 `target`，等待进程结束后重跑并记录原因。

## 5. 风险与回滚

| Risk | Impact | Mitigation |
|---|---|---|
| 误拒 legacy/template `q_sql_join` | 破坏现有答题闭环 | 仅在 resolved `KnowledgePoint` 存在时强制 enrollment |
| idempotency replay 仍绕过权限 | 旧 response 可能泄露或重放 | scope 校验放在 replay 之前 |
| Orchestrator ANSWER_SUBMISSION 间接受影响 | 可能新增 `FORBIDDEN` 失败路径 | 属于正确安全行为；相邻测试覆盖 |

## 6. Architecture Drift

预检 PASS：不改 API/DB/依赖；授权在 Service 层；不新增 Prompt/前端/Agent 工具权限逻辑。
