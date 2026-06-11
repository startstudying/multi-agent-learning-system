# ACCEPT - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## Acceptance Result

Accepted。

## Acceptance Criteria

| Criteria | Result | Evidence |
|---|---|---|
| ResourceGeneration detail 使用 explicit `ADMIN` role fact | PASS | Bearer `ADMIN sub=ops_admin` + spoofed header 可读他人 task detail。 |
| `USER sub=admin` 不能读取他人 ResourceGeneration detail | PASS | 新增 controller test 返回 `FORBIDDEN` 且无 `data`。 |
| Admin missing ResourceGeneration detail 返回 `NOT_FOUND` | PASS | 新增 controller test 覆盖。 |
| learner-resources 保持 owner-only，missing 分支不让 `USER sub=admin` 获得 admin 语义 | PASS | 新增 controller test 返回 `FORBIDDEN`。 |
| Agent Trace detail 使用 explicit `ADMIN` role fact | PASS | Bearer `ADMIN sub=ops_admin` + spoofed header 可读他人 trace detail。 |
| Agent Trace search 只允许 explicit admin | PASS | Bearer admin 可 search；Bearer `USER sub=admin` 被拒。 |
| 非管理员 missing/foreign anti-enumeration 保持 | PASS | 既有与新增测试均通过。 |
| 不修改 API/DTO/schema/dependency/frontend | PASS | 仅修改目标 backend Java/test 与文档。 |
| Verification 完成 | PASS | Focused 9/9、adjacent 108/108、full 463 run 通过。 |

## Remaining Work

- P3-4 CourseAccessService legacy overload cleanup。
- P3-4 broader class/course authorization matrix。
- P3-4 formal OAuth2/JWK/Spring Security。
- P3-4 broader permission penetration tests。
- P3-2 工业级 PDF/DOCX layout/page/section 层级仍未完成。
