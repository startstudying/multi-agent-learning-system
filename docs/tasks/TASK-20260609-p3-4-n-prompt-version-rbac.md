# TASK-20260609 P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## Status

Done

## Parent Docs

- PRD: `docs/product/PRD-20260609-p3-4-n-prompt-version-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-n-prompt-version-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-n-prompt-version-rbac.md`
- PLAN: `docs/plans/PLAN-20260609-p3-4-n-prompt-version-rbac.md`
- CONTEXT: `docs/context/CONTEXT-20260609-p3-4-n-prompt-version-rbac.md`

## Done Criteria

- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已创建。
- [x] 多专家报告已收集，并完成集成评审。
- [x] RED 测试已观察到预期失败。
- [x] `POST /api/agent/prompt-versions` 仅允许 roles-first admin。
- [x] `GET /api/agent/prompt-versions` 与 detail 仅允许 roles-first admin/teacher。
- [x] Teacher 响应不包含 `promptText`。
- [x] Bearer spoofed header 与 `USER/STUDENT sub=admin/teacher_1` role confusion 测试通过。
- [x] 不新增依赖。
- [x] 不改 DB schema。
- [x] 不改前端。
- [x] Focused verification 已执行并记录。
- [x] Adjacent regression 已执行并记录。
- [x] Full backend Maven verification 已执行或限制已说明。
- [x] Evidence / Acceptance / Changelog / Memory / backend TODO / Retro 已更新。

## Implementation Checklist

1. [x] 修改 `PromptVersionControllerTest`，新增 Bearer JWT helper 与 PromptVersion RBAC 矩阵。
2. [x] 运行 focused RED：`mvn --% -Dtest=PromptVersionControllerTest test`。
3. [x] 修改 `PromptVersionResponse` 支持 `promptText` 脱敏省略。
4. [x] 修改 `PromptVersionService` 增加 role-aware upsert/list/get。
5. [x] 修改 `PromptVersionController` 传入 roles-first facts。
6. [x] 修改 `PromptVersionServiceTest` 适配 service 签名和脱敏语义。
7. [x] 运行 focused / adjacent / full tests。
8. [x] 收尾文档。

## Allowed Files

见 Context Pack。

## Test Commands

```powershell
cd backend
mvn --% -Dtest=PromptVersionControllerTest test
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

## Verification Results

```text
RED:      mvn --% -Dtest=PromptVersionControllerTest test
Result:   9 run, 5 failures, 0 errors, 0 skipped

Focused:  mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest test
Result:   14 run, 0 failures, 0 errors, 0 skipped

Adjacent: mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
Result:   48 run, 0 failures, 0 errors, 0 skipped

Full:     mvn test
Result:   410 run, 0 failures, 0 errors, 1 skipped
```
