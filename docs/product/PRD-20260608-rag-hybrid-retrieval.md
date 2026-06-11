# PRD - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 问题陈述

当前 RAG 在线检索只在已授权 KB 内按 `createdAt desc` 读取最近 chunk，再由占位 `RerankerService` 原样返回。该实现可证明权限和 citation 基础链路，但不能根据问题相关性排序，也没有 RRF 融合、reranker timeout fallback 或可观测降级状态。

本切片目标是在不新增依赖、不改变 DB schema、不接真实 VectorDB 的前提下，把在线检索升级为可生产演进的最小 Hybrid-ready 链路。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 学生 | RAG 问答使用者 | 获得更相关的课程引用答案 |
| 教师 | 课程资料维护者 | 确认课程资料检索结果更贴近问题 |
| 管理员 | 运维/治理人员 | 看到 reranker fallback 和检索降级证据 |
| 后端开发者 | RAG 维护者 | 在不引入外部依赖的情况下为 VectorDB/reranker 接入预留边界 |

## 3. 用户故事

- 作为学生，我希望课程问答优先引用与问题关键词相关的资料，而不是仅引用最近上传的资料。
- 作为教师，我希望 RAG 无法找到资料时仍明确返回 no-source，而不是伪造引用。
- 作为管理员，我希望 query log 能说明 reranker 是否未配置、超时或异常降级。
- 作为后端开发者，我希望后续接入真实 embedding/vector/reranker 时不需要重写 `RagQueryService` 主流程。

## 4. MVP 范围

### 纳入范围

- allowed KB 内 keyword branch + recency branch。
- RRF fusion 与去重。
- vector branch disabled metadata，不接真实 VectorDB。
- `RerankerService` timeout/error fallback 结果对象。
- `kb_query_log.rerankerStatus` 写稳定 reranker 状态。
- `sourcesJson` 写安全 hybrid/reranker metadata。
- RAG query / Orchestrator RAG_QA 聚焦与相邻测试。

### 非目标

- 不接 Spring AI embedding。
- 不新增 VectorDB adapter。
- 不调用外部 reranker provider。
- 不新增 Maven dependency。
- 不新增 Flyway migration。
- 不修改 frontend。
- 不处理复杂 PDF/DOCX/OCR。
- 不改变 requestId replay 的既有 `responseJson` 快照合同。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| 相关性排序 | keyword 命中的旧 chunk 可排在较新的无关 chunk 前 | `RagQueryServiceTest` |
| RRF 去重 | 多分支命中同一 chunk 不重复 citation | `RrfRankerTest` |
| fallback 稳定性 | reranker timeout/error 时 RAG query 仍返回 OK 和 citations | `RagQueryServiceTest` |
| 权限安全 | mixed allowed/forbidden KB 仍检索前拒绝且不写 artifact | 回归测试 |
| 架构稳定 | 不改 API/DB/dependency/frontend | 文件 diff 与测试 |

## 6. 用户流程

```text
学生提问
-> 后端安全检查
-> 权限过滤 requested kbIds
-> allowed KB 内 keyword + recency retrieval
-> RRF fusion
-> reranker 未配置 / 超时 / 异常时 fallback
-> 生成带 citations 的 RAG response
-> 写 query log / source citation / metrics
```

## 7. 依赖关系

- 依赖：既有 RAG KB、chunk、query log、citation、permission、metrics。
- 阻塞：无。
- 后续依赖本切片：真实 embedding service、VectorDB adapter、外部 reranker provider。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| 是否后续拆分 `kb_query_log` 的 replay snapshot 与 audit log | 后续 P3 安全治理 | Open |
| 是否为真实 vector retrieval 新增 schema / adapter | 后续 P3-2-B | Open |

## 9. 审批

| 角色 | 姓名 | 日期 | 状态 |
|---|---|---|---|
| Main Codex | - | 2026-06-08 | Approved for implementation |

## 10. 完成状态

- 状态：已完成。
- 完成日期：2026-06-08。
- 验收证据：`docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md`、`docs/acceptance/ACCEPT-20260608-rag-hybrid-retrieval.md`。
- 备注：本 PRD 关闭无新增依赖的 `keyword + recency + RRF + reranker fallback` 最小切片；真实 embedding service / VectorDB adapter 仍是后续 P3-2 任务。
