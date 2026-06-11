# RUN - P3-4-X LearningPath Detail Roles-First RBAC Integration Review

## 1. Inputs

| Expert | Report |
|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-architect.md` |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-test.md` |

## 2. Integrated Decision

采用最小 roles-first 修复：

- `LearningPathController.get(...)` 从 `CurrentUserService.currentUser()` 获取可信 `UserContext`。
- `LearningPathController.get(...)` 将 explicit `ADMIN` fact 传入 `LearningWorkflowService`。
- `LearningWorkflowService` 新增/使用 `getPathForUser(currentUserId, currentUserAdmin, pathId)`。
- HTTP GET 主路径不再通过 `currentUserId == "admin"` 判断 admin。

## 3. Conflict Resolution

| Topic | Decision |
|---|---|
| 是否处理 create legacy overload | 不处理。P3-4-S 已完成 direct create roles-first；本切片只处理 detail GET。 |
| 是否删除本地 `isAdmin(String)` helper | 不强制删除。若旧 create overload 仍保留兼容，则 helper 暂留；本切片必须保证 GET 主路径不再调用它。 |
| 是否新增 dependency/security review | 不需要。无新增依赖。 |
| 是否改变 API/DTO/schema | 不改变。 |

## 4. Test Plan

新增 `LearningWorkflowControllerTest` HTTP 级 RED 测试，覆盖：

- explicit Bearer admin foreign read。
- explicit Bearer admin missing `NOT_FOUND`。
- Bearer `USER sub=admin` foreign/missing 均 `FORBIDDEN`。
- Bearer owner + spoofed header owner read。
- Bearer non-owner foreign/missing anti-enumeration。

## 5. Acceptance Boundary

P3-4-X 完成只代表 LearningPath detail GET 的 roles-first RBAC 收口完成，不代表：

- broader class/course authorization matrix 完成。
- formal OAuth2/JWK/Spring Security 完成。
- broader permission penetration tests 完成。

