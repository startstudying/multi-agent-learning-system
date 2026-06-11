# Retrospective - RAG 查询重放与响应快照

## 1. Feature Summary

完成 P0-3 `RAG_QA query replay / response snapshot` 最小切片。RAG 查询现在支持 `requestId` 幂等、payload hash 冲突检测、完整响应快照和 Orchestrator workflow 级重放。

## 2. What Went Well

- TDD RED 阶段准确暴露缺口：`RagQueryService` 没有带 `requestId` 的 query 方法。
- 架构 subagent 建议复用答题提交的 `requestId + requestHash + responseJson` 模式，降低了新机制复杂度。
- 安全 subagent 提醒 workflow envelope 不应保存 RAG 问题原文，本轮同步完成了 `RAG_QA` payload 脱敏。
- 聚焦、交叉回归和全量后端测试均通过。

## 3. What Didn't Go Well

- 当前 `kb_query_log.question` 仍保存问题原文，这是既有 query log 设计；本轮只避免 Orchestrator workflow envelope 扩大敏感面。
- 并发唯一键兜底没有新增专门并发测试，只通过唯一索引和顺序 replay 测试覆盖主要路径。
- `responseJson` 保存完整 RAG response，后续如果接入真实私有课程内容，需要配合 retention 和权限审计策略。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| requestId + requestHash + responseJson 幂等模式 | Yes | 暂不新建，先沉淀在 memory；后续文档上传幂等可复用 |
| Orchestrator envelope 脱敏快照 | Yes | 暂不新建，后续可合并到 agent-trace-governance |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Subagent parallelism | L1 架构/测试/安全审查有效 | 后续 P0 DB/权限切片继续使用 L1 |
| TDD | 先写服务层和 Orchestrator RED | 后续并发场景可先补线程级 RED |
| Privacy | RAG workflow payload 已脱敏 | 后续评估 `kb_query_log.question` retention 策略 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 文档上传 requestId 或业务唯一键 | Backend/RAG | 后续 P0-3 |
| 文档索引 DB 级并发去重约束或锁 | Backend/RAG | 后续 P0-3 |
| 后台恢复任务 | Backend/Operations | 后续 P0-3 |
| MySQL 8 Flyway smoke 覆盖 V7 | Backend/Database | P3-1 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] DATABASE_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend-architecture-todolist.md
