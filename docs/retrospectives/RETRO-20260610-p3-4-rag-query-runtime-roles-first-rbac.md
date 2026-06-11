# RETRO-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

## 1. Feature Summary

完成 RAG query runtime roles-first RBAC 补口：

- `/api/rag/query`、Chat/Tutor runtime、Orchestrator `RAG_QA` replay precheck 和执行路径均传递 explicit admin/teacher facts。
- `RagQueryService` legacy overload 保持兼容但默认非提权。
- 补齐 Bearer admin spoofed header、Bearer `USER sub=admin` role-confusion、requestId replay 和 Orchestrator side-effect-free denial 回归。

## 2. What Went Well

- M 级切片边界清楚，没有混入 KB-course schema/lifecycle 大改。
- 专家 subagent 并行给出了明确缺口：Orchestrator `RAG_QA` 回归测试。
- Focused、adjacent、full backend 验证闭环，测试数据足够支撑 acceptance。

## 3. What Didn't Go Well

- 初始 Context Pack allowed files 缺少 Orchestrator 测试文件，后续补测时需要回填。
- TASK 状态曾滞后于代码验证，需要收口时手动回填 checklist。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Runtime service calls must receive explicit role facts, not infer from `userId` | Yes | `docs/skills/project-specific/auth-context-boundary.md` |
| RAG query replay precheck must use same authorization facts as execution | Yes | `docs/skills/project-specific/object-scope-authorization.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | M 级任务已有 REQ/SPEC/PLAN/TASK/CONTEXT | Context Pack 初版就列出 likely adjacent test files，避免后补 allowed files |
| Testing | Focused 后再 adjacent/full | 对 workflow wrapper 场景默认补一组 wrapper-level RBAC 回归 |
| Documentation | Evidence/Acceptance 最后集中写 | TASK checklist 在验证完成后立即更新，减少状态漂移 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| KB-course binding schema/lifecycle governance | Future Codex | 后续 P3-4 L 级任务 |
| SSE production auth transport strategy | Future Codex | 后续 auth-context task |
| Dev/test legacy fallback cleanup | Future Codex | 后续 auth-context cleanup |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md not required; no new skill created
- [x] ARCHITECTURE_BASELINE.md not required; no architecture rule changed
