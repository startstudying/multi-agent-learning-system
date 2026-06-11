# RETRO - P3-4-M Course API / CourseAccessService roles-first overload

## 1. Feature Summary

完成 P3-4-M：Course API / Knowledge Catalog 主路径现在使用 roles-first Course access overload。

- `CourseAccessService` 新增 read/manage/list roles-first overload，旧签名保留。
- `KnowledgeCatalogService` 新增 create/read/list/chapter/knowledge point/dependency/graph roles-first overload。
- `CourseController` 与 `KnowledgePointController` 读取 `UserContext.roles()` 并向 service 传显式 role facts。
- `CourseKnowledgeControllerTest` 覆盖 Bearer admin、teacher、student spoofing、missing course、`USER sub=admin` 与 `USER sub=teacher_1` 角色混淆。

## 2. What Went Well

- RED 不仅暴露了 Bearer admin/teacher 被 legacy 字符串 gate 降权，也暴露了 `USER sub=admin` / `USER sub=teacher_1` 会被业务层升权的反向风险。
- 旧签名保留并委托，降低了对 Assessment、RAG Document、Learning Workflow、Resource Generation 等已验收切片的影响。
- Controller 改为从 `UserContext.roles()` 取显式 role facts，使 Bearer token 场景不再被 test/dev legacy userId inference 干扰。
- Focused、adjacent、full backend Maven 均通过。

## 3. What Didn't Go Well

- 当前 auth 仍是 transitional 模式，`CurrentUserService.isAdmin()` / `isTeacherUser()` 在 test/dev 下仍有 legacy inference；Course API 为避免 Bearer 角色混淆改为使用显式 roles，这个差异需要在后续 formal auth 迁移中统一。
- `CourseAccessService` 旧签名仍存在，其他模块仍可能在 Bearer 场景下使用 legacy inference；本切片不能误判为全仓库迁移完成。
- 工作区不是 git repository，无法用 `git diff/status` 做最终变更归纳；证据改用文件内容、测试名和 Maven 输出。
- `node_repl` MCP 未启动，本切片不需要使用。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Controller 对 Bearer-sensitive 业务入口应传显式 `UserContext.roles()` facts，避免 service 层用 userId 字符串二次推断。 | Yes | 暂不新增；后续可并入 `docs/skills/project-specific/auth-context-boundary.md`。 |
| `CourseAccessService` 迁移用 overload 保留旧签名、先迁移 HTTP 主路径，避免大爆炸 RBAC 重写。 | Yes | 暂不新增；后续可并入 `docs/skills/project-specific/object-scope-authorization.md`。 |

本切片不创建新 skill；现有 `object-scope-authorization` 与 `auth-context-boundary` 足够覆盖。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Scope control | Course API 主路径最小迁移 | 后续全仓库调用方继续按模块切片迁移，不一次性改全部 RBAC。 |
| Testing | `CourseKnowledgeControllerTest` 补 Bearer role + role-confusion 矩阵 | 后续同类端点同时测“有 role 但 subject 非 legacy 名称”和“无 role 但 subject 像 legacy 角色名”。 |
| Documentation | P3-4-M 与 P3-4-L 分开记录 | TODO 中继续显式标注 broader class/course、full RBAC、formal auth open。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 规划其他旧 `CourseAccessService` 调用方 roles-first 迁移 | Main Codex / Backend Expert | 后续 P3-4-N 或模块切片 |
| 规划 formal OAuth2/JWK/Spring Security migration slice | Main Codex / Security Expert | 后续 P3-4 auth slice |
| 扩展 PromptVersion / Evaluation / RAG KB management full RBAC matrix | Main Codex / Test Expert | 后续 P3-4 RBAC matrix slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] CHANGELOG.md
- [x] backend architecture TODO
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）
