# ACCEPT - P3-4-X LearningPath Detail Roles-First RBAC

## Acceptance Result

Accepted。

## Acceptance Criteria

| Criteria | Result | Evidence |
|---|---|---|
| PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已存在 | PASS | `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context` 下均有 P3-4-X 文档。 |
| RED 失败已观察 | PASS | focused RED `7 run, 4 failures`，失败点覆盖 Bearer explicit admin、`USER sub=admin` role-confusion、admin missing 与 non-admin missing。 |
| `GET /api/learning-paths/{pathId}` 使用 explicit admin role fact | PASS | `LearningPathController.get(...)` 从 `UserContext.roles()` 派生 `ADMIN` 并传入 roles-first service overload。 |
| Bearer `ADMIN sub=ops_admin` foreign detail 与 missing 语义正确 | PASS | focused GREEN 覆盖 foreign `200 OK` 与 missing `404 NOT_FOUND`。 |
| Bearer `USER sub=admin` foreign/missing 均按 non-admin 语义拒绝 | PASS | focused GREEN 覆盖 foreign/missing 均为 `403 FORBIDDEN`，且响应不泄露目标 id。 |
| owner 与 non-owner anti-enumeration 回归通过 | PASS | focused GREEN 覆盖 owner spoofed header `200 OK`、non-owner foreign/missing `403 FORBIDDEN`。 |
| focused / adjacent / full verification 完成 | PASS | focused `7/7`，controller `20/20`，adjacent `52/52`，full backend `474 run, 0 failures, 0 errors, 1 skipped`。 |
| 不修改 REST API / DTO / schema / dependency / frontend | PASS | 仅修改 LearningPath detail controller/service/test 与本切片文档。 |
| Evidence / Acceptance / Retro 已创建 | PASS | `docs/evidence/EVIDENCE-20260609-p3-4-x-learning-path-detail-rbac.md`、`docs/acceptance/ACCEPT-20260609-p3-4-x-learning-path-detail-rbac.md`、`docs/retrospectives/RETRO-20260609-p3-4-x-learning-path-detail-rbac.md`。 |
| Changelog / Memory / TODO 已更新 | PASS | 已更新 P3-4-X 条目，并保留 P3-4 未整体完成状态。 |

## Remaining Work

- P3-4 broader class/course authorization matrix。
- P3-4 formal OAuth2/JWK/Spring Security。
- P3-4 broader permission penetration tests。
- `LearningWorkflowService.createPathForUser(String currentUserId, CreateLearningPathRequest request)` legacy subject-name helper cleanup。
- P3-2 工业级 PDF/DOCX layout/page/section 层级。
