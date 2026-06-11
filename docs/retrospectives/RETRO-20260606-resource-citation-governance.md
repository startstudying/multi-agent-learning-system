# Retrospective - 资源生成引用与幻觉治理

## 1. Feature Summary

完成 P1-4 最小后端闭环：资源生成写入任务级 `source_citation`，Critic review 写入引用检查，`NO_SOURCE` 资源进入人工复核且不能直接审批发布。

## 2. What Went Well

- 复用了现有 `source_citation`、`ResourceReview.citationCheck` 和 Review Gate，没有引入新依赖或 schema 膨胀。
- TDD RED 明确暴露旧行为仍写固定引用文案，GREEN 后用 45 个相关测试确认资源、审核、RAG、迁移收敛未破坏。
- 发布逻辑增加了审批时拦截和 release 兜底两层防线。

## 3. What Didn't Go Well

- 之前长时间 subagent/测试流程没有及时收口，导致用户等待过久。
- 本切片仍是任务级 citation，不是 resource-level citation，治理粒度有限。
- 文档中历史中文编码输出存在乱码，后续应逐步修正关键文档可读性。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 资源生成引用治理：`traceId` citation + `citationCheck` + release gate | Yes | `docs/skills/project-specific/resource-citation-governance.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 旧 subagents 可长时间挂起 | 每个切片开始先关闭不需要的旧 subagents，长测试前给用户明确状态 |
| Testing | 先跑最小 RED，再跑相关回归 | 保持，不直接上全量长测 |
| Documentation | 部分旧文档编码显示不稳定 | 新增/重写切片文档使用可读 UTF-8 中文 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 后续增加 resource-level citation schema | Backend | P3 / 单独 citation schema 切片 |
| 将 ResourceAgent 接入真实 Course RAG retrieval | Backend/RAG | P3-2 |
| 替换临时 `teacher/admin` 审核权限为真实 RBAC | Backend/Security | P3-4 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] API_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [ ] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md
