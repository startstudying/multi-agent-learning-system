# PLAN - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 追踪

- PRD：`docs/product/PRD-20260608-rag-hybrid-retrieval.md`
- REQ：`docs/requirements/REQ-20260608-rag-hybrid-retrieval.md`
- SPEC：`docs/specs/SPEC-20260608-rag-hybrid-retrieval.md`

## 2. Skill Selection Report

### Task Type

RAG / retrieval 生产化增强；后端 service 内部行为变更；测试与安全边界增强。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 用户目标属于后端系统增强，必须走 Spec-first 完整闭环 |
| `educational-rag-pipeline` | 涉及 RAG retrieval、rerank、citation、query log |
| `agent-trace-governance` | RAG_QA 与 trace/query log/citation 证据相关 |
| `multi-agent-coder` | 用户要求专家 subagent 并行分析 |
| `subagent-driven-development` | 专家分析完成后由主 Codex 集成实施 |
| `test-driven-development` | 本切片必须 RED tests 先行 |
| `verification-before-completion` | 完成前必须 fresh test evidence |
| `Confidence Check` | 实施前确认无重复实现、架构符合、根因明确 |

### Missing Skills

无。项目已有 RAG / security / workflow 规则覆盖本切片。

### GitHub Research Needed

No。不新增依赖，不采用外部 SDK；RRF 为本地小算法，已有项目架构文档约束足够。

### New Project-Specific Skill To Create

完成后视实现稳定性沉淀 `rag-hybrid-retrieval.md`。

## 3. Confidence Check

| Check | Result | Evidence |
|---|---|---|
| No duplicate implementations | PASS | `ChunkService` 仅 recency；`RerankerService` passthrough；无 RRF |
| Architecture compliance | PASS | `docs/architecture/rag-architecture.md` 明确 optional hybrid/RRF/reranker fallback |
| Official docs needed | N/A/PASS | 不接外部 API/SDK；使用本地架构文档与 Java 标准库 |
| OSS reference needed | N/A/PASS | 不复制外部代码；RRF 为可测试小算法 |
| Root cause identified | PASS | 当前 retrieval 不使用 question，reranker timeout 配置未消费 |

Confidence: 0.93。可以进入实现。

## 4. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 写 RED tests：RRF、keyword、fallback、脱敏/权限 | TASK-01 | 已完成 |
| 2 | 实现 `ChunkService` retrieval result、RRF 与 keyword scoring | TASK-01 | 已完成 |
| 3 | 实现 `RerankerService` 状态化 timeout/error fallback | TASK-01 | 已完成 |
| 4 | 调整 `RagQueryService` metadata 持久化与 router 降级语义 | TASK-01 | 已完成 |
| 5 | 运行聚焦、相邻、全量测试并写 Evidence/Acceptance | TASK-01 | 已完成 |

## 5. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/src/main/java/com/learningos/rag/application/ChunkService.java` | 修改 | 2 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/RerankerService.java` | 修改 | 3 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/RagQueryService.java` | 修改 | 4 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/AdaptiveRagRouter.java` | 修改 | 4 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/RetrievalResult.java` | 新增 | 2 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/RerankResult.java` | 新增 | 3 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/RerankerStatus.java` | 新增 | 3 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/RrfRanker.java` | 新增 | 2 | Main Codex |
| `backend/src/test/java/com/learningos/rag/application/RrfRankerTest.java` | 新增 | 1 | Main Codex |
| `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java` | 修改 | 1/5 | Main Codex |
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 修改 | 5 | Main Codex |
| `docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md` | 新增 | 5 | Main Codex |
| `docs/acceptance/ACCEPT-20260608-rag-hybrid-retrieval.md` | 新增 | 5 | Main Codex |

## 6. 依赖

- 前置条件：RAG permission strict hardening、query replay snapshot、chunk production metadata 已完成。
- 新增依赖：无。
- DB schema：无变更。

## 7. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| `rerankerStatus` 语义从 routing strategy 改为 reranker status | 现有测试需同步 | SPEC 明确 routing strategy 保留在 `sourcesJson.retrieval.strategy` |
| Timeout 测试 flaky | CI 不稳定 | 使用超短配置和 fake reranker，避免真实外部调用 |
| keyword scoring 过度复杂 | 难维护 | 保持 bounded pool + 简单 token scoring |
| query log 敏感面扩大 | 安全风险 | 新增 metadata 只写枚举/计数/latency，不写 raw prompt/error/full chunk |

## 8. 回滚策略

- 回滚 RAG application 层新增类型和方法，恢复 `ChunkService` recency retrieval 与 `RerankerService` passthrough。
- 因不改 schema/API/dependency，回滚无需数据迁移。

## 9. 测试策略

- RED：
  - `mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test`
- GREEN focused：
  - `mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test`
- 相邻回归：
  - `mvn --% -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test`
- 全量：
  - `mvn test`

## 10. Subagent 计划

| 专家 | 是否需要 | 职责 |
|---|---|---|
| Backend Expert | 是 | service/repository/API 边界分析 |
| Agent/RAG Expert | 是 | hybrid/RRF/reranker 方案 |
| Security & Quality | 是 | 权限、脱敏、依赖边界 |
| Test Engineer | 是 | RED tests 与回归策略 |
| Integration Reviewer | 是 | 本文件与集成评审 |

并行级别：L1 并行分析 + L2 设计；实施由 Main Codex 串行完成。

## 11. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Approved |

## 12. 完成记录

- 完成日期：2026-06-08。
- 实现结果：allowed KB 内 `keyword + recency + RRF` 已接入 `ChunkService`，真实 vector branch 保持 disabled metadata；`RerankerService` 已返回稳定状态枚举并在 timeout/error 时 fallback 到 fused candidates。
- 文档闭环：Evidence、Acceptance、Retrospective、Changelog、Memory、TODO 和项目技能已补齐。
- 验证证据：见 `docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md`。
