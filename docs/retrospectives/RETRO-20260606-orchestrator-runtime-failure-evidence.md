# Retrospective - Orchestrator 运行期失败证据持久化

## 1. Feature Summary

完成 P0-1 运行期失败证据最小切片：Orchestrator workflow task 创建后，如果下游服务抛出 `ApiException`，系统会保留失败 task 和失败 trace，并保持原 HTTP 错误码。

## 2. What Went Well

- RED 测试准确暴露了真实缺口：失败后 task 停在 `RUNNING`。
- 安全 subagent 及时指出失败摘要不能保存原始异常全文，本轮改为只保存 error code。
- 聚焦、交叉和全量测试都通过，说明新事务边界没有破坏现有成功链路和前置校验。

## 3. What Didn't Go Well

- 最初测试预期偏向“权限失败”，而 subagent 建议也可用“安全失败”；最终选择权限失败，因为它能同时验证不写 query/citation。
- 本轮只实现通用 `step_runtime_failure`，没有按 RAG/Assessment 细分失败 step id，后续如果做更细 Trace UI 可以再扩展。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Orchestrator runtime failure evidence | No | 已由 `ai-learning-architecture` 和 `agent-trace-governance` 覆盖 |
| Safe failure summary | No | 写入 memory 即可 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Subagent parallelism | L1 审查有效发现测试和安全边界 | 后续 P0-3 RAG replay 继续采用 L1 审查 |
| TDD | 单测试先 RED，再最小实现 | 后续保持先跑 narrow RED |
| Privacy | 初版实现曾打算保存原始 message | 后续失败 evidence 默认只存 error code / safe summary |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 实现 RAG_QA query replay / response snapshot | Backend/RAG | 后续 P0-3 |
| 实现文档上传 requestId 或业务唯一键 | Backend/RAG | 后续 P0-3 |
| 设计 retry endpoint 或 retry workflow contract | Backend/Orchestrator | 后续 P0-1 |
| 梳理 model_call_log 错误脱敏策略 | Backend/Governance | 后续 P2/P3 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend-architecture-todolist.md
