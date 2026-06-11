# RETRO - P3-4-L class analytics roles-first course scope

## 1. Feature Summary

完成 P3-4-L：`GET /api/analytics/classes/{courseId}/summary` 现在使用 roles-first class/course 授权。

- Controller 将 `CurrentUserService.isAdmin()` / `isTeacherUser()` 传给 service。
- Service 对 class summary 执行 admin / teacher own-course / student deny / non-admin anti-enumeration 规则。
- Bearer admin、teacher own-course、student spoofing、non-admin missing/foreign、admin missing course 矩阵测试已覆盖。

## 2. What Went Well

- 专家报告先行把边界压在 class summary 路径，避免把本切片扩大成 formal OAuth2/JWK 或全量 `CourseAccessService` 迁移。
- RED 直接暴露两个关键缺口：Bearer admin 被 legacy 字符串 gate 拒绝，非 admin missing course 泄露 `NOT_FOUND`。
- 旧签名保留并委托，降低对既有调用点的影响。
- Focused、adjacent、full backend Maven 均通过。

## 3. What Didn't Go Well

- 当前权限模型仍处于 transitional 状态，`CourseAccessService` 其他调用路径仍有 legacy inference，不能误判为 P3-4 完成。
- 工作区不是 git repository，无法用 `git diff/status` 做最终变更归纳；证据改用文件内容、测试名和 Maven 输出。
- `node_repl` MCP 未启动，本切片不需要使用。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 对对象级 analytics 端点，controller 传 role-derived booleans，service 决定 object scope 与 missing-vs-forbidden。 | Yes | 暂不新增；后续可并入 `docs/skills/project-specific/object-scope-authorization.md`。 |
| Bearer role + spoofed header 必须在真实业务 endpoint 覆盖，而不只测 auth helper。 | Yes | 暂不新增；后续可并入 `docs/skills/project-specific/auth-context-boundary.md`。 |

本切片不创建新 skill；现有 `object-scope-authorization` 与 `auth-context-boundary` 足够覆盖。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Scope control | 单端点最小授权修复 | 后续 P3-4 继续按 endpoint / module 切片，不做大爆炸 RBAC 重写。 |
| Testing | 在 `AnalyticsControllerTest` 中补 roles-first 矩阵 | 后续同类端点继续补 Bearer role + spoofed header + missing/foreign anti-enumeration。 |
| Documentation | P3-4-L 与 P3-4-K 分开记录 | TODO 中继续显式标注 broader class/course 与 formal auth open。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计 Course API / `CourseAccessService` roles-first overload 局部迁移 | Main Codex / Backend Expert | 后续 P3-4-M |
| 规划 formal OAuth2/JWK/Spring Security migration slice | Main Codex / Security Expert | 后续 P3-4 auth slice |
| 扩展 PromptVersion / Evaluation / RAG KB management full RBAC matrix | Main Codex / Test Expert | 后续 P3-4 RBAC matrix slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] CHANGELOG.md
- [x] backend architecture TODO
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）
