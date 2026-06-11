# RETRO - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## What Went Well

- RED 测试精准复现了 9 个角色混淆和 real admin 误拒问题。
- 最小实现只新增 roles-first overload，并保持 legacy overload 兼容。
- 没有触碰 API、DB、frontend、Agent/RAG/model runtime。
- Focused、adjacent、full backend tests 均通过。

## What Was Risky

- `learner-resources` 需要特别避免误开放 admin 读取能力；最终只让 explicit admin 影响 missing `NOT_FOUND` 语义。
- `cancel` 与 trace detail 同属 `/api/agent/tasks` controller，容易顺手修改；本切片保持 owner-only，不开放 admin cancel。

## Reusable Pattern

对于后续 roles-first RBAC 收口：

1. Controller 使用 `CurrentUserService.currentUser()`，从 `roles()` 派生 explicit facts。
2. Service 新增 explicit facts overload；旧 overload 保留兼容。
3. HTTP 主路径必须调用新 overload。
4. RED 覆盖：
   - Bearer real role + spoofed header 成功。
   - Bearer `USER sub=admin` / `USER sub=teacher_1` 被拒。
   - admin missing `NOT_FOUND` 与 non-admin missing/foreign `FORBIDDEN`。

## Skill Extraction Decision

不新增项目技能；现有 `auth-context-boundary` 与 `object-scope-authorization` 已覆盖该模式。

## Follow-up

下一步建议处理 P3-4 的 `CourseAccessService` legacy overload cleanup，或继续 broader class/course authorization matrix。
