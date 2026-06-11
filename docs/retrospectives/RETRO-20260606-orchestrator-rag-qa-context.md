# Retrospective - Orchestrator RAG_QA 上下文收敛

## 1. Feature Summary

完成 `RAG_QA` workflow 的 Orchestrator 接入。现在同一个 workflow 会生成统一的 `workflowId / agentTaskId / traceId`，RAG 查询和 citation 会继承这个 traceId，workflow query 也能回放完整步骤。

## 2. What Went Well

- TDD 先暴露出 `queryWithTraceId(...)` 缺失，避免直接写“看起来能用”的接入代码。
- `RAG_QA` 的 payload 校验前置，保证无效请求不会落半成品 task。
- RAG 仍然保留权限过滤和 citation 落库，Orchestrator 只负责上下文统一。
- 子代理先做架构评审，把本轮边界锁在最小切片内，没有把 `ANSWER_SUBMISSION` 一起拉进来。

## 3. What Didn't Go Well

- 现有 workflow 查询仍依赖 `agent_task.inputJson` 里的 `workflowId` 标记，长期可维护性一般。
- 运行期权限/安全失败的 durable evidence 还没做，本轮只能在文档里明确留后续切片。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Orchestrator-owned RAG traceId 收敛 | Yes | 复用现有 `agent-trace-design.md` / `rag-citation-viewer.md` |
| 先测后改的 workflow 接入方式 | Yes | 复用现有 `test-driven-development` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | Orchestrator 仍依赖字符串 envelope 查询 | 后续考虑独立 workflow 表或更稳定的索引键 |
| Testing | 先 RED 再 GREEN，效果清楚 | 继续把集成测试作为 workflow 接入的最小门槛 |
| Documentation | Evidence / Acceptance / Retro 都能直接对照测试结果 | 后续切片继续保持同样的验收格式 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 接入 `ANSWER_SUBMISSION` workflow context | Backend/Orchestrator | P0-1 后续切片 |
| 设计 workflow retry/recovery 语义 | Backend/Orchestrator | P0-1 后续切片 |
| 继续补运行期失败证据 | Backend/RAG | P0-1 / P0-3 后续切片 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [ ] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md
