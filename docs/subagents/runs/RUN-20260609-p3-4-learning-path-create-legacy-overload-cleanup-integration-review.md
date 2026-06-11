# RUN-20260609 P3-4 LearningPath create legacy overload cleanup - Integration Review

## Integrated Decision

PASS。三个专家结论一致：删除 `LearningWorkflowService` 两参 legacy create overload 和 `isAdmin(String)` helper，保留四参 roles-first overload。

## Conflict Resolution

无冲突。

## Implementation Boundary

本子任务只允许修改：

- `LearningWorkflowService.java`
- `LearningWorkflowServiceTest.java`
- 对应任务、证据、memory、changelog、TODO 文档

不改 Controller、REST API、DTO、schema、依赖、frontend、CourseAccessService、ResourceGeneration、Orchestrator、Agent Trace、Review Gate 或 OAuth2/JWK/Spring Security。

## Verification Required

- RED reflection tests must fail before production deletion.
- Focused tests must pass after deletion.
- Compile guard must pass to prove no stale production callers.
- Adjacent LearningPath service/controller tests must pass.
- Full backend test should pass because this is a permission cleanup under P3-4.
