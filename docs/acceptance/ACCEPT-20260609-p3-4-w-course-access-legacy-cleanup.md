# ACCEPT - P3-4-W CourseAccessService legacy overload cleanup

## Acceptance Result

Accepted。

## Acceptance Criteria

| Criteria | Result | Evidence |
|---|---|---|
| PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已存在 | PASS | `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context` 下均有 P3-4-W 文档。 |
| RED 失败已观察 | PASS | `CourseAccessServiceTest` 初次运行 `4 run, 2 failures`，失败点为 legacy overload/helper 仍存在。 |
| `CourseAccessService` legacy public overload 已删除 | PASS | reflection 测试通过；static guard 无匹配。 |
| `CourseAccessService` subject-name helper 已删除 | PASS | `scopedCourseMissing(String)`、`isAdmin(String)`、`isTeacherUser(String)` 均无匹配。 |
| roles-first 行为未回退到 subject-name inference | PASS | `currentUserId = "admin"` / `"teacher_1"` 在 explicit role fact 为 false 时不获得 admin/teacher 语义。 |
| 编译守卫通过 | PASS | `mvn --% -DskipTests compile` 通过。 |
| focused / adjacent / full verification 完成 | PASS | focused `4/4`、adjacent `183/183`、full backend `467 run, 0 failures, 0 errors, 1 skipped`。 |
| 不修改 REST API / DTO / schema / dependency / frontend | PASS | 仅修改 `CourseAccessService`、新增服务测试与文档。 |
| Changelog / Memory / TODO 已更新 | PASS | 已更新 P3-4-W 条目，并保留 P3-4 未整体完成状态。 |

## Remaining Work

- P3-4 broader class/course authorization matrix。
- P3-4 formal OAuth2/JWK/Spring Security。
- P3-4 broader permission penetration tests。
- P3-2 工业级 PDF/DOCX layout/page/section 层级。
- `LearningWorkflowService.getPathForUser(String currentUserId, String pathId)` 中仍存在独立 subject-name admin 判断风险，建议后续单独切片处理，不混入 P3-4-W。
