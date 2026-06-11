# Retrospective - P3-4-G Grading Evaluation Course Scope

## 1. Feature Summary

将 `POST /api/assessment/grading-evaluations` 从 teacher/admin 角色门禁收口为 `courseId` 授权锚点。teacher 只能对 own course 运行评估，admin 可运行任意 existing course，student 优先拒绝；sample 非空 `knowledgePointId` 必须属于请求 course。

## 2. What Went Well

- TDD RED 明确复现了 5 个权限缺口：teacher 缺 course、foreign/missing course、admin missing course、sample KP outside course。
- 三个专家 subagent 的结论快速收敛到强制 `courseId`，避免保留旧请求绕过 scope。
- 复用 `CourseAccessService`，没有在 Controller 复制权限逻辑。
- Focused / adjacent / full backend Maven 验证均通过。

## 3. What Didn't Go Well

- 旧 grading quality SPEC 和 P3-4-C SPEC 中仍写着“course scope 未来做”，需要本切片同步补注，防止文档漂移。
- 安全技能自带 hardcoded secret 脚本因编码/引号问题无法执行，只能用 targeted scan 补证据。
- `GradingEvaluationService` 原本是纯离线计算服务；引入 course scope 后需要 mock 依赖，单测构造略微变重。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Evaluation/governance API 一旦具备 `courseId`，teacher/admin HTTP path 应强制 course scope，不保留无 scope 旧请求。 | Yes | `docs/skills/project-specific/object-scope-authorization.md` |
| student 优先拒绝，再校验 course/sample，避免通过 validation 差异探测对象存在性。 | Yes | `docs/skills/project-specific/object-scope-authorization.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| API compatibility | 旧无 `courseId` 请求与权限目标冲突。 | 安全切片优先明确“兼容范围”：可保留算法兼容，但 HTTP path 不保留越权兼容。 |
| Security scans | 一条脚本因自身语法错误失败。 | Evidence 中记录失败原因，并用 targeted scan / review 补足证据。 |
| Test matrix | 初版缺 admin success / admin missing courseId。 | 权限矩阵测试按 actor x missing/own/foreign/missing object 明确列全。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 实现正式 JWT/RBAC，替换 `X-User-Id` 过渡身份。 | Backend | Future P3-4 |
| 将 grading evaluation 接入 evaluation set / scheduled runner。 | Backend | Future P2/P3 |
| 扩展 broader class/course 权限矩阵。 | Backend | Future P3-4 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] API memory file
- [x] SKILL_REGISTRY.md
- [x] Project-specific skill
