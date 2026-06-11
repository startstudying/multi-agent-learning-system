# RETRO - P3-4-K 权限渗透测试矩阵补齐

## 1. Feature Summary

完成 P3-4-K 当前 transitional 权限渗透测试矩阵补齐：

- staging header-only auth 显式拒绝。
- Bearer `ADMIN` role 能驱动 analytics admin-only 入口，并忽略 spoofed `X-User-Id`。
- token-budget governance 改为 role-derived admin gate。
- active enrolled student 仍不能写 course graph。
- student course list 不泄露 dropped enrollment course。
- student 即使拥有 public KB 且 enrolled，也不能伪造 RAG document course metadata。

## 2. What Went Well

- 三份专家报告先行收敛，避免把 P3-4-K 扩大成 OAuth2/JWK 或 broader class/course 重构。
- RED 暴露了 analytics 仍依赖 `"admin"` 字符串身份判断，修复边界清晰。
- 相邻测试类复用了既有 fixtures，比新建一个大型 `PermissionMatrixControllerTest` 更稳。
- Focused、adjacent、full backend Maven 均通过，覆盖了新增矩阵和既有 assessment/resource/review anti-enumeration。

## 3. What Didn't Go Well

- 工作区不是 git repository，无法用 `git diff/status` 直接归纳变更；证据改用文件内容和 Maven 输出核对。
- `PermissionMatrixControllerTest` 没有落地为单独文件；当前采用相邻测试类补齐，后续若矩阵继续增长，可再抽统一 lightweight matrix test。
- P3-4 的 TODO 容易被误读为全部完成；本次必须持续强调只是 transitional matrix 完成。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Bearer role + spoofed header 必须在真实 Controller admin-only / teacher-only 入口验证，而不能只测 filter/helper。 | Yes | 暂不新增；先并入 `docs/skills/project-specific/auth-context-boundary.md` 的后续维护候选。 |
| 权限渗透矩阵可先补相邻测试类，再在矩阵膨胀时抽 lightweight `PermissionMatrixControllerTest`。 | Yes | 暂不新增；保留为 `object-scope-authorization` 后续模板候选。 |

本切片不创建新 skill；现有 `object-scope-authorization` 与 `auth-context-boundary` 足够覆盖。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Matrix test placement | 使用相邻测试类补缺口 | 后续同一攻击模型超过 5 个端点时再抽统一 matrix test。 |
| RED evidence | analytics Bearer role RED 清晰 | 后续每个权限切片继续保留 expected status 与实际 status。 |
| TODO wording | P3-4 多个子项交织 | 每次只标记具体 slice 完成，不把 broader class/course 或 formal auth 误标完成。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 继续设计 broader class/course 权限模型 | Main Codex / Security Expert | 后续 P3-4 slice |
| 评估 formal OAuth2/JWK/Spring Security 迁移计划 | Main Codex / Backend Architecture Expert | 后续 P3-4 auth slice |
| 复核 teacher class summary missing vs foreign 语义 | Main Codex / Security Expert | 后续 anti-enumeration slice |
| 如权限矩阵继续增长，抽 `PermissionMatrixControllerTest` lightweight 冒烟矩阵 | Main Codex / Test Expert | 后续 P3-4 regression slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] CHANGELOG.md
- [x] backend architecture TODO
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）
