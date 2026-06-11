# RETRO-20260610 P3-4 子任务：KB-course binding governance

## 1. Feature Summary

完成 RAG KB-course binding schema/lifecycle governance：

- `kb_knowledge_base` 增加 `course_id`、`binding_status`、`bound_by`、`bound_at`。
- `BOUND` KB read/write 统一走 `CourseAccessService`，不允许 `PUBLIC`、owner、explicit permission 绕过 course access。
- 空 `UNBOUND` KB 首次合法 course document upload 自动绑定为 `BOUND`。
- `CONFLICTED` KB 非 admin 不可读写，文档上传路径拒绝。
- requestId payload conflict 保持 `409 CONFLICT` 优先级。

## 2. What Went Well

- 专家并行审查有效拦截了两个关键问题：requestId 优先级和 `UNBOUND` 自动绑定并发锁。
- Final Verification Expert 发现 admin 早返回不满足“BOUND 必须走 CourseAccessService”的严格语义，已通过代码和回归测试收紧。
- 全量 backend 验证通过，测试规模从 519 增至 520，说明新增回归纳入主测试集。

## 3. What Didn't Go Well

- 初始 REQ/SPEC 保留了“UNBOUND 一律禁止 course metadata”的旧语义，和最终实现发生漂移。
- 收尾文档一开始滞后于代码，导致专家判定交付闭环 FAIL；后续补齐 Evidence/Acceptance/Retro/Memory。
- MySQL smoke 仍受本机 Docker/MySQL 凭据限制，不能作为通过证据。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Course-bound object permissions must route through parent-scope service even for admin paths when the contract says so | Yes | `docs/skills/project-specific/object-scope-authorization.md` |
| requestId payload conflict should be checked before later validation that depends on mutable lifecycle state | Yes | `docs/skills/project-specific/rag-parser-boundary.md` not suitable; keep as memory for now |
| Empty-unbound-to-bound lifecycle transitions need row-level concurrency protection | Yes | future project-specific lifecycle-governance skill if repeated |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | L 级任务有专家并行和完整文档 | 在实现语义变化后立即回写 REQ/SPEC，避免收尾集中漂移 |
| Testing | Focused/adjacent/full after implementation | 专家发现的安全语义必须补一条固定回归测试再跑 full |
| Documentation | Evidence/Acceptance 最后集中写 | 每个专家发现修复后同步记录在 Evidence 草稿 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 为 `CONFLICTED` KB 设计显式冲突修复/拆分工作台 | Future Codex | 后续 P3-4 子任务 |
| 恢复可用 MySQL 8 smoke 环境并重新执行 V20 smoke | Future Codex / Environment | 后续 P3-1/P3-4 验证 |
| 继续 broader class/course/answer-record RBAC matrix | Future Codex | 后续 P3-4 子任务 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md not required; no new skill created
- [x] ARCHITECTURE_BASELINE.md not required; no architecture rule changed
