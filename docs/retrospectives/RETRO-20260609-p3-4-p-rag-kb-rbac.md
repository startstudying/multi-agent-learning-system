# RETRO - P3-4-P RAG KB management roles-first RBAC

## 1. Feature Summary

完成 P3-4-P：RAG Knowledge Base management 主路径已迁移到 roles-first RBAC。

- `KnowledgeBaseController` / `DocumentController` 从 `CurrentUserService.currentUser()` 获取 `UserContext`。
- Controller 只从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts，避免 dev/test legacy subject inference 污染 Bearer role tests。
- `KnowledgeBaseService`、`DocumentService`、`PermissionService` 新增 role-aware overload，旧签名保留用于兼容路径。
- Bearer admin 可 list/read/write active KB，不依赖 `sub == "admin"`，也不受 spoofed `X-User-Id` 影响。
- Teacher course metadata 校验改为 role-aware `CourseAccessService`，不依赖 `teacher_` subject 前缀。
- `scopedMissing(...)` 改为基于显式 `currentUserAdmin`，`USER sub=admin` 不再得到 admin-like `NOT_FOUND`。
- Full backend Maven verification 通过：`426 run, 0 failures, 0 errors, 1 skipped`。

## 2. What Went Well

- RED 覆盖有效：admin spoof、teacher no-prefix、subject-name role-confusion、missing-vs-foreign oracle 都被测试捕获。
- 实施切片保持边界清晰：只迁移 RAG KB management 主路径，不碰 retrieval runtime、parser/vector/index worker、DB schema 或 frontend。
- 兼容策略稳：旧签名保留并默认非 admin/teacher，降低 `/api/rag/query` 等非目标路径语义漂移风险。
- 与 P3-4-M/N/O 的 roles-first 模式一致，后续可继续沿用“Controller role facts + Service 授权”的迁移方式。

## 3. What Didn't Go Well

- RAG KB 权限路径历史上同时承担 personal KB、public KB、显式 permission、course metadata、index task detail 多种语义，收口时必须非常小心不把 retrieval runtime 一起改坏。
- `CurrentUserService.isAdmin()` / `isTeacherUser()` 在 dev/test 仍保留 legacy fallback；本切片通过 Controller 显式读取 `UserContext.roles()` 绕开，但全量迁移前仍容易被误用。
- Evidence/Acceptance 需要反复声明“不代表 P3-4 完成”，否则容易把 P3-4-P 的局部完成误解成 RAG/full RBAC 完成。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| RAG / management Controller 从 `UserContext.roles()` 派生 explicit facts，Service overload 接收 facts，legacy overload 默认非提权。 | Yes | 后续可合并进 `docs/skills/project-specific/roles-first-rbac-overload.md`。 |
| RBAC RED 矩阵固定覆盖：Bearer admin with spoofed header、valid role without legacy prefix、wrong role with legacy-looking subject、missing/foreign anti-enumeration。 | Yes | 后续可沉淀为 `docs/skills/project-specific/roles-first-rbac-test-matrix.md`。 |

本切片不新增 project-specific skill；现有 `feature-development-workflow`、P3-4-K/M/N/O/P 文档和项目记忆足够支撑继续执行。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Role derivation | Controller 中存在重复 `hasRole(...)` helper | 后续可抽取小型静态工具或统一 role facts DTO，但不要为本切片扩大 diff。 |
| Legacy overload | 旧签名默认非 admin/teacher，兼容但需要持续识别调用方 | 后续 P3-4 切片逐步迁移管理面 public 方法到显式 role facts。 |
| RBAC tests | 每个 Controller 自带 Bearer helper | 后续可抽取测试 fixture，减少重复 JWT 构造。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 继续扩展 broader class/course 权限矩阵 | Main Codex / Architecture + Security | 后续 P3-4 |
| 继续迁移更多 legacy `CourseAccessService` caller | Main Codex / Backend Expert | 后续 P3-4 |
| 规划 `/api/rag/query` roles-first retrieval runtime 迁移 | Main Codex / RAG + Security Expert | 后续 P3-4 / RAG |
| 规划 formal OAuth2/JWK/Spring Security 迁移 | Main Codex / Security Expert | 后续 P3-4 大切片 |
| 复用 P3-4-P RBAC 矩阵到后续管理 API | Main Codex / Test Expert | 后续 P3-4 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] API_MEMORY.md
- [x] CHANGELOG.md
- [x] backend architecture TODO
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）
