# Retrospective - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. Feature Summary

完成 RAG embedding/vector 最小边界切片：`EmbeddingService` 批量 contract、`VectorIndexAdapter` noop boundary、索引链路 EMBEDDING/VECTOR_UPSERT 阶段、vector hit id-only 搜索结果、allowed KB 下推与服务层二次过滤、安全 metadata，以及 focused/full Maven 验证。

## 2. What Went Well

- 集成评审先裁定只做 boundary/noop/fake，避免直接引入真实 provider、VectorDB SDK、schema 和部署复杂度。
- 安全反馈及时收敛：adapter upsert 不再携带 raw chunk content，search 不再返回完整 `KbDocChunk`。
- `IndexService` 采用保存新 chunks 前完成 embedding/vector 的顺序，enabled 失败时不会留下新的 searchable chunks。
- `ChunkService` 对 adapter hit 执行回表加载和 allowed KB 二次过滤，避免只依赖外部 adapter 权限。
- 聚焦 P3-2-A 测试和全量后端测试均通过。

## 3. What Didn't Go Well

- 早期文档沿用了“save chunks 后再 embedding”的流程描述，和最终安全实现不一致；收尾阶段已修正文档。
- Context Pack 初版未列出新增 `IndexServiceEmbeddingVectorTest`，收尾阶段已补入允许文件和测试命令。
- 全量 `mvn test` 首次工具超时为 124 秒，低于实际全量套件耗时；后续用更长超时重跑通过。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| embedding/vector boundary、noop/default、safe enabled failure、id-only vector hits、allowed-KB 二次过滤、metadata 最小化 | Yes | `docs/skills/project-specific/rag-embedding-vector-adapter.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| RAG indexing flow docs | 文档可能滞后于安全实现顺序 | Evidence/Acceptance 前必须核对流程图与最终代码顺序 |
| Context Pack | 新测试文件可能在实施中出现 | 新增测试文件后同步更新 Context Pack allowed files |
| Verification | 全量 Maven 测试可能超过默认工具超时 | 全量后端测试默认使用至少 300 秒超时 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 真实 Spring AI embedding provider 接入，含官方文档核验、dependency review、安全错误码与配置治理 | 后续开发者 | 新建 PRD/REQ/SPEC/PLAN/TASK |
| 真实 VectorDB adapter 接入，含 collection/schema、allowed-KB query filter、id-only hit contract、回表授权校验和运维治理 | 后续开发者 | 新建 PRD/REQ/SPEC/PLAN/TASK |
| 在真实 provider/VectorDB 接入前为 embedding/vector pipeline 增加 errorCode 白名单/枚举映射，未知值降级为固定安全错误码 | 后续开发者 | 新建 PRD/REQ/SPEC/PLAN/TASK |
| Query log retention / replay snapshot 脱敏和审计分层治理 | 后续开发者 | 新建安全治理切片 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] SKILL_REGISTRY.md
- [N/A] ARCHITECTURE_BASELINE.md（本切片无基线规则变更）
