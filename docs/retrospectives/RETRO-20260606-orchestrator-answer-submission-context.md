# Retrospective - Orchestrator ANSWER_SUBMISSION 上下文收敛

## 1. Feature Summary

完成 `ANSWER_SUBMISSION` workflow 的 Orchestrator 接入。现在同一个 workflow 会生成统一的 `workflowId / agentTaskId / traceId`，答题提交、评分、反馈诊断、掌握度更新、错题记录、学习事件都继承该 traceId，并可通过 GET workflow 回放。

## 2. What Went Well

- 子代理提前指出了 replay 和 traceId 漂移风险，避免直接把 `submitAnswer(...)` 挂进 Orchestrator。
- TDD 的 RED 阶段准确暴露缺少 `submitAnswerWithTraceId(...)` 和 preflight replay。
- 没有新增 schema，用现有 `agent_task` envelope 完成最小 P0-1 切片。
- `ANSWER_SUBMISSION` envelope 不保存完整答案，降低了一点审计表敏感数据暴露。
- 复查阶段补齐了原先容易漏掉的精确 replay 匹配、trace drift 清理和 service 层 `requestId` 校验。

## 3. What Didn't Go Well

- 现有 workflow 没有独立表，replay 仍依赖 `agent_task.inputJson` marker 查询，长期不够稳。
- 运行期失败 evidence 仍可能受事务影响，本轮只处理了前置校验和成功路径。
- 全量测试暴露 RAG timeout recovery 用例依赖固定时间戳，说明测试 fixture 需要避免随当前日期漂移。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Orchestrator 子流程接入时先加显式 traceId service 入口，再做 workflow replay preflight | Yes | 暂不新建，已有 `agent-trace-governance` 和 `ai-learning-architecture` 可覆盖 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow replay | 依赖 `agent_task.inputJson` | 后续 P0-3 增加 workflow request 表或唯一键 |
| Testing | controller + service 双层覆盖 | 后续补并发 Orchestrator replay 测试 |
| Time-based tests | 曾使用固定 cutoff 时间 | 使用相对当前时间或可注入 Clock，避免测试随日期失效 |
| Documentation | 每个子切片单独 evidence/acceptance | 保持 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 补 workflow 节点失败策略和可重试策略 | Backend | 后续 P0-1 |
| 设计 workflow-level idempotency/recovery | Backend | 后续 P0-3 |
| 补题目/课程/路径级权限校验 | Backend/Security | 后续 P3 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md 不需要更新
- [x] ARCHITECTURE_BASELINE.md 不需要更新
