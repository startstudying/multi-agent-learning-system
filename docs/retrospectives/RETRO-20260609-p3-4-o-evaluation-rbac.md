# RETRO - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. Feature Summary

完成 P3-4-O：Evaluation Set / Run HTTP 管理主路径已迁移到 roles-first RBAC。

- Controller 从 `CurrentUserService.currentUser()` 获取 `UserContext`。
- Controller 只从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts。
- `EvaluationSetService` 和 `EvaluationRunService` 的管理入口接收显式 role facts。
- Service 授权 helper 不再从 `userId == "admin"` 或 `userId.startsWith("teacher_")` 推断角色。
- 非 admin missing/foreign evaluation set / run comparison 统一返回 `FORBIDDEN`，admin missing 保留 `NOT_FOUND`。
- 测试覆盖 Bearer admin spoof、teacher no-prefix、student subject-admin、USER subject-teacher-prefix 与 anti-enumeration。
- Full backend Maven verification 通过：`419 run, 0 failures, 0 errors, 1 skipped`。

## 2. What Went Well

- RED 命中非常清晰：Bearer admin 被拒、Bearer student `sub=admin` 被放行、Bearer user `sub=teacher_1` 被放行、missing/foreign oracle 四类问题都被测试捕获。
- 实现切片保持小范围：只触碰 Evaluation Set / Run controller/service/test 和收尾文档。
- roles-first 边界明确：Controller 层只做 `roles()` -> facts，Service 层只信显式 facts，不再混入 dev/test compatibility subject inference。
- 与 P3-4-N PromptVersion RBAC 的测试模式一致，后续可继续复用到 RAG KB management / GradingEvaluation legacy caller。

## 3. What Didn't Go Well

- 旧 service 方法签名与测试中仍散落 legacy subject 语义，迁移时需要同步重建/更新 service tests。
- Evaluation Set / Run 属于 prompt/evaluation 管理能力，但和 RAG / grading / resource generation 样本类型存在概念交叉，证据文档必须反复声明“不代表 RAG KB 或 full RBAC 完成”。
- PowerShell 对部分 UTF-8 中文文档的控制台输出显示为 mojibake，阅读旧文档时需要以路径和英文标识为锚点，避免误改历史内容。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 管理 API Controller 从 `UserContext.roles()` 派生显式 role facts，Service 禁止从 subject 名称推断角色。 | Yes | 后续可合并进 `docs/skills/project-specific/auth-context-boundary.md`。 |
| RBAC RED 矩阵固定覆盖：Bearer admin with spoofed header、valid role without legacy prefix、wrong role with legacy-looking subject、missing/foreign anti-enumeration。 | Yes | 后续可沉淀为 `docs/skills/project-specific/roles-first-rbac-test-matrix.md`。 |

本切片不新增 project-specific skill；现有 `feature-development-workflow`、RBAC 记忆和 P3-4-K/N/O 文档足够支撑继续执行。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| RED 矩阵 | 每个切片手写 Bearer helper 和 role-confusion cases | 后续 P3-4 切片可抽取测试辅助函数或统一 fixture，降低重复。 |
| Service 边界 | legacy 与 roles-first 方法可能并存 | 新管理面 public 方法默认要求显式 role facts；legacy overload 只保留在必要兼容点。 |
| 文档收尾 | Evidence/Acceptance 手动复述验证结果 | 后续可在 TASK 中固定“RED / focused / adjacent / full / drift”小节模板。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 继续收口 RAG KB management RBAC | Main Codex / Security Expert | 后续 P3-4 |
| 继续迁移 GradingEvaluation 其他 legacy caller / CourseAccessService caller | Main Codex / Backend Expert | 后续 P3-4 |
| 扩展 broader class/course 教师与学生权限模型 | Main Codex / Architecture + Security | 后续 P3-4 |
| 规划 formal OAuth2/JWK/Spring Security 迁移 | Main Codex / Security Expert | 后续 P3-4 大切片 |
| 复用 P3-4-O RBAC 矩阵到后续管理 API | Main Codex / Test Expert | 后续 P3-4 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend architecture TODO
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）
