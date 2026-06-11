# RETRO - P3-4-X LearningPath Detail Roles-First RBAC

## What Went Well

- RED 矩阵直接覆盖了漏洞的两个方向：explicit admin 被误拒，以及 `USER sub=admin` 被误提权。
- 实现保持了小切片：只改 `GET /api/learning-paths/{pathId}` detail 主路径，没有混入 create path、ResourceGeneration、Agent Trace 或 formal OAuth2 配置。
- roles-first overload 让 HTTP 主路径的授权事实来自 `UserContext.roles()`，同时保留兼容签名为 non-elevating 默认。
- focused、adjacent、full backend 验证都覆盖到 detail、owner、admin、missing/foreign anti-enumeration。

## What Was Risky

- `LearningWorkflowService` 仍存在 `isAdmin(String)`，因为 legacy create overload 仍调用它；本切片不能顺手删除，否则会扩大到 P3-4-S 之外的 create semantics cleanup。
- `GET /api/learning-paths/{pathId}` 既要允许 explicit admin foreign read，又要保持 non-admin missing/foreign 防枚举，测试矩阵必须同时覆盖两类语义。
- P3-4-X 完成后容易误读为 P3-4 全部完成；实际 broader class/course、formal OAuth2/JWK/Spring Security、broader penetration tests 仍未完成。

## Reusable Pattern

后续 detail RBAC role-confusion cleanup 可复用：

1. 先写 HTTP RED 矩阵覆盖 Bearer explicit role、spoofed `X-User-Id`、`USER sub=admin/teacher_1` role-confusion、owner、foreign、missing。
2. Controller 只从 `UserContext.roles()` 派生 role fact。
3. Service 新增 roles-first overload，并让 legacy overload 默认 non-elevating，避免继续传播 subject-name inference。
4. 保持 non-admin missing/foreign collapse 为 safe `FORBIDDEN`，admin missing 保留 `NOT_FOUND`。
5. focused、adjacent、full 验证后只标记当前切片完成，不关闭父级 P3-4。

## Skill Extraction Decision

不新增项目技能；现有 `auth-context-boundary`、`object-scope-authorization`、`test-driven-development` 和 `security-review` 已覆盖该模式。

## Follow-up

建议下一步独立处理：

- `LearningWorkflowService.createPathForUser(String currentUserId, CreateLearningPathRequest request)` legacy subject-name admin 判断。
- broader class/course authorization matrix。
- formal OAuth2/JWK/Spring Security。
- broader permission penetration tests。
