# RUN - Backend Plan RAG Production Integration Review

## 1. 集成结论

接受 RAG 架构专家和测试专家建议。本轮继续推进 `docs/planning/backend-architecture-todolist.md`，但不尝试一次完成全部 P3 剩余项。下一步选择 P3-2 的 **parser adapter 最小生产切片**。

理由：

- 解析质量是 Embedding/VectorDB、hybrid retrieval、reranker 的输入前提。
- 当前 `IndexService` 内嵌轻量 PDF/DOCX 解析，缺少 adapter 边界，不利于后续引入真实解析库或 OCR fallback。
- 本切片可不新增依赖，先完成可测试的内部边界抽取、统一 section 输出、错误脱敏与 worker/manual 路径一致性。

## 2. 已接收专家输出

| 专家 | 报告 | 关键结论 |
|---|---|---|
| RAG/后端架构专家 | `docs/subagents/runs/RUN-20260607-backend-plan-rag-production-architect.md` | 先做 parser adapter 最小生产切片，不直接做满 OCR。 |
| 测试专家 | `docs/subagents/runs/RUN-20260607-backend-plan-rag-production-test-strategy.md` | chunk 子切片测试健康；后续需用 RED 测试锁定 parser、embedding/vector、hybrid/reranker 边界。 |
| 安全专家 | 待返回 | 本集成评审先按现有项目安全规则约束：不新增依赖、不保存原文/密钥/原始错误、错误码脱敏、权限边界不变。 |

## 3. 本轮切片范围

纳入：

- 抽出 RAG parser adapter 边界。
- 让 Markdown/TXT/PDF/DOCX 通过统一 parser 接口产出 section。
- 保留现有轻量 PDF/DOCX 解析能力，不新增 Tika/PDFBox/POI/OCR 依赖。
- 为后续 OCR fallback 预留状态/接口边界，但不接真实 OCR。
- 保持 `IndexService` chunk/hash/metadata 行为不回退。
- 更新测试、Evidence、Acceptance、Memory、Changelog。

排除：

- 真实 OCR 引擎。
- 新解析库依赖。
- VectorDB / Embedding provider。
- hybrid retrieval、RRF、reranker timeout fallback。
- 公开 RAG query API 变更。

## 4. 冲突处理

| 议题 | 决策 |
|---|---|
| 是否马上引入 PDFBox/POI/Tika | 不引入。先抽 adapter 边界，依赖另开 review。 |
| OCR fallback 是否本轮做完 | 不做满。只保留接口/状态边界，避免环境不可控。 |
| 是否修改 `kb_doc_chunk` schema | 本轮不预期修改 schema；若实现中必须新增字段，先更新 SPEC 和 migration。 |
| 是否并行实现 | 不并行实现。专家并行分析已完成，代码由 Main Codex 单线程集成，避免同改 `IndexService`。 |

## 5. 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Parser adapter 属于 RAG application 内部边界，Repository 不承载解析逻辑。 |
| Frontend rules | PASS | 不修改前端。 |
| Agent / RAG rules | PASS | 离线索引增强，不改变 RAG answer/citation 合同。 |
| Security | PASS | 无新依赖；不保存原文片段、storage key、OCR/provider 原始错误。 |
| API / Database | PASS | 不改公开 API；默认不新增 DB schema。 |

## 6. 执行模式

Main Codex 单线程实现。Subagents 已完成并行分析/测试策略；后续可在代码完成后追加 code review / verifier。
