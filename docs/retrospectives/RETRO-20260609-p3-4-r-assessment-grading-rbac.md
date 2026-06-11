# RETRO - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. Feature Summary

完成 Assessment answer/wrong-question read paths 与 GradingEvaluation HTTP path 的 roles-first RBAC 迁移。HTTP 主路径不再从 subject 字符串推断 admin/teacher，而是由 Controller 从 `UserContext.roles()` 派生 explicit role facts 并传入 Service。

## 2. What Went Well

- 专家报告及时指出仅做 GradingEvaluation 会漏掉同一 controller 下的 Assessment read paths。
- RED 测试一次性命中 11 个预期失败，覆盖正向 role facts 和负向 subject-name role-confusion。
- 实现 diff 较小，未触碰 API/DB/frontend/dependency。
- focused、adjacent、full backend 验证均通过。

## 3. What Didn't Go Well

- 初始测试设计中 `USER sub=admin` list 场景一度过严；修正为显式查询 foreign `learnerId=bob` 后更准确。
- `AssessmentService` 的 list/detail helper 参数扩展较多，后续可考虑提取小型 `RoleFacts` 值对象降低方法签名噪音。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Controller 从 `UserContext.roles()` 派生 facts，Service 接 explicit booleans，legacy overload 兼容委托 | Yes | 暂并入 `auth-context-boundary` / `object-scope-authorization`，暂不新建 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 每个 P3-4 切片重复复制 JWT helper | 后续可抽测试工具类，但需单独小切片避免跨包测试影响 |
| Testing | Controller-level Bearer tests 最有效 | 对每个 legacy role caller 都补 Bearer admin/teacher + `USER sub=...` 矩阵 |
| Documentation | 文档数量多但可追溯 | 保持切片文档简洁，避免展开到非目标模块 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 继续处理 LearningPath / ResourceGeneration course-bound create role facts | Codex | 后续 P3-4-S |
| broader class/course permission matrix | Codex | 后续 P3-4 |
| formal OAuth2/JWK/Spring Security | Codex | 独立生产化任务 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] API_MEMORY.md
- [x] CHANGELOG.md
- [ ] SKILL_REGISTRY.md：本次不新增 skill
- [ ] ARCHITECTURE_BASELINE.md：无架构基线变更
