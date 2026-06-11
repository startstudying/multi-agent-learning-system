# RETRO - P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. Feature Summary

完成 P3-4-N：PromptVersion HTTP 管理 API 现在具备 roles-first RBAC，并按角色收口 `promptText` 暴露。

- `PromptVersionController` 读取 `UserContext.roles()` 并传显式 admin/teacher facts。
- `PromptVersionService` 写入只允许 admin，读取只允许 admin/teacher。
- `PromptVersionResponse` 支持 teacher metadata-only 响应，省略 `promptText`。
- `PromptVersionControllerTest` 覆盖 Bearer admin、teacher、student、spoofed header、`sub=admin` / `sub=teacher_1` role-confusion。
- Full backend Maven verification 通过：`410 run, 0 failures, 0 errors, 1 skipped`。

## 2. What Went Well

- Security/Backend/Test 三位专家意见分歧清晰，集成评审能明确为什么先做 PromptVersion，而不是直接做 Evaluation 或 GradingEvaluation。
- RED 失败非常集中：无鉴权写入、无鉴权读取、teacher 暴露 `promptText` 三类问题都被捕获。
- 实现保持小切片：只触碰 `agent` 模块 PromptVersion 相关文件，没有引入 schema、依赖或 frontend 改动。
- 收紧 Service 公共方法后，避免未来生产代码误用无鉴权 `upsert/get/list`。

## 3. What Didn't Go Well

- 初始 GREEN 后发现 Service 中保留无鉴权便捷方法不够干净，需要二次收紧并重新跑验证。
- 现有 transitional auth 仍允许 dev/test 无 Bearer 时通过 `X-User-Id` 派生 roles；这符合当前兼容规则，但 formal OAuth2/JWK 迁移前仍需持续测试 Bearer 优先级。
- P3-4 仍有多条并行未完成线：Evaluation/GradingEvaluation/RAG KB/broader class-course/formal auth，不能因为 PromptVersion 完成就降低 TODO 粒度。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 管理 API 应从 `UserContext.roles()` 传显式 role facts，不用 subject 字符串推断权限。 | Yes | 可后续并入 `docs/skills/project-specific/auth-context-boundary.md`。 |
| 对敏感字段使用同一 DTO + non-null 序列化进行 role-based redaction，保持 API path/request 合同稳定。 | Yes | 暂不新增；后续若重复出现可沉淀为 `management-api-rbac-redaction`。 |

本切片不创建新 skill；现有 `auth-context-boundary`、`object-scope-authorization`、`security-review` 足够覆盖。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Subagent 决策 | 三个专家分别推荐不同候选 | 后续继续保留集成评审文件，显式记录未采纳方案。 |
| RED 矩阵 | 覆盖 role success、spoofed header、role confusion、field redaction | 后续 Evaluation/RAG KB 同样要求“有 role 非 legacy subject”和“无 role 但 subject 像 legacy 角色名”两类测试。 |
| Service API | 起初保留无鉴权便捷方法 | 管理面 Service public 方法默认要求显式权限 facts；内部读取另设清晰命名方法。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 规划 Evaluation Set/Run roles-first RBAC matrix | Main Codex / Test Expert | 后续 P3-4-O 候选 |
| 规划 GradingEvaluation old `CourseAccessService` caller roles-first migration | Main Codex / Backend Expert | 后续 P3-4-O/P |
| 规划 RAG KB management 权限模型 | Main Codex / Security Expert | 后续 P3-4 |
| 规划 formal OAuth2/JWK/Spring Security | Main Codex / Security Expert | 后续 P3-4 大切片 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend architecture TODO
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）

