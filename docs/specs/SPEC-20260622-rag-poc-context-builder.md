# SPEC - RAG POC Context Builder

## 1. 背景

`wiki-rag` 参考调研中最适合迁移的是检索后的 POC（parent, own, child）上下文优化，而不是 Python / LangChain / Milvus / MCP 运行时。本项目已经具备 RAG parser metadata、hybrid/RRF retrieval、roles-first permission filter、citation persistence 和 AI QA runtime，因此本切片只补强“命中 chunk 后如何组织回答上下文”。

## 2. 目标

- 在 `RagQueryService` 检索并 rerank 出源 chunk 后，构造结构化 POC 上下文。
- POC 上下文可包含命中 chunk 自身、同文档相邻 chunk、父级章节 chunk、子级章节 chunk。
- 回答生成可使用扩展上下文，citation / `source_citation` 仍只锚定原始命中源 chunk。
- 查询日志只记录安全计数和策略 metadata，不写入扩展 chunk 原文。

## 3. 非目标

- 不迁移 `wiki-rag` 的 Python / LangChain / LangGraph / Milvus / MCP runtime。
- 不新增数据库字段、Flyway migration、第三方依赖或前端接口。
- 不默认上线 query rewrite / HyDE / MCP。
- 不改变 `POST /api/rag/query`、`GET /api/rag/query`、`POST /api/ai/qa` 的请求/响应字段。

## 4. 设计

### 4.1 新增内部组件

新增 `PocContextBuilder`：

```text
build(allowedKbIds, sourceChunks) -> PocContextResult
```

职责：

- 只从已经 permission-filter 后的 `sourceChunks` 出发。
- 读取同 document 的 chunk 列表，二次过滤 `kbId`、`documentVersion` 和 `allowedKbIds`。
- 基于 `chunkIndex` 选择 previous / next。
- 基于 `metadataJson.headingPath` 推断 parent / child。
- 返回去重后的 context chunks 与安全 metadata。

### 4.2 上下文选择规则

对每个源 chunk：

1. `OWN`：保留源 chunk。
2. `ADJACENT`：补充同文档同版本的 `chunkIndex - 1` 与 `chunkIndex + 1`。
3. `PARENT`：若源 chunk 的 `headingPath` 有父路径，选择最近的前置父级 chunk。
4. `CHILD`：选择同文档后续第一个以源 `headingPath` 为前缀且更深层的 chunk。

所有补充 chunk 必须满足：

- `kbId` 在 `allowedKbIds` 中。
- `documentId` 与源 chunk 相同。
- `documentVersion` 与源 chunk 相同。
- 不跨 KB、不跨文档、不跨版本。

### 4.3 回答与引用

- `answer` 使用 `PocContextResult.contextChunks()` 构造。
- `sources` 仍使用 rerank 后的 `sourceChunks` 构造。
- `source_citation` 仍只持久化 `sources`，不为补充上下文伪造 citation。
- no-source 场景不构造扩展上下文，不持久化 citation。

### 4.4 查询日志 metadata

`kb_query_log.sources_json` 增加：

```json
{
  "pocContext": {
    "enabled": true,
    "sourceChunkCount": 1,
    "contextChunkCount": 3,
    "expandedChunkCount": 2,
    "parentChunkCount": 1,
    "adjacentChunkCount": 1,
    "childChunkCount": 0
  }
}
```

禁止写入：

- 扩展 chunk 原文
- raw question / prompt
- provider request / response / error
- secrets

## 5. API / DB / 依赖

- API：无变更。
- DB schema：无变更。
- 依赖：无新增。

## 6. 权限与隐私

- `PermissionService.requireReadableKbIds(...)` 仍先于 retrieval。
- POC expansion 只接收 `allowedKbIds`，并在 builder 内再次过滤。
- Citation 仍锚定真实命中 chunk，不把扩展上下文当作可引用来源。
- Query log 只写安全计数 metadata。

## 7. 架构漂移检查

| 检查项 | 结论 |
|---|---|
| Backend layering | PASS：Controller 不变，RAG application service 内部扩展 |
| Frontend rules | PASS：无 frontend 改动 |
| Agent / RAG rules | PASS：permission-first、citation-visible、no-source 保持 |
| Security | PASS：无 secret、无新依赖、metadata 不含原文 |
| API / Database | PASS：无 API/DB 合同变更 |

## 8. 测试策略

- RED：新增 `PocContextBuilderTest`，验证 parent/adjacent/child 扩展、allowed KB 二次过滤、去重与顺序。
- RED：扩展 `RagQueryServiceTest`，验证回答使用 POC 上下文但 citations 仍只包含源 chunk，`sourcesJson` 只写安全 metadata。
- GREEN：实现最小 builder 和服务接入。
- 回归：运行 `PocContextBuilderTest,RagQueryServiceTest` 与 backend compile。

## 9. 验收标准

- [x] 命中子章节 chunk 时，回答包含父/相邻上下文。
- [x] `sources` 与 `source_citation` 不因为扩展上下文增加。
- [x] POC 不跨 forbidden KB / documentVersion。
- [x] no-source 行为保持不伪造 citation。
- [x] `sourcesJson` 包含 `pocContext` 安全计数，不包含扩展原文。
- [x] 不新增 API、DB schema、依赖或 frontend 改动。

## 10. 实施状态

- 状态：已完成。
- 完成日期：2026-06-22。
- Evidence：`docs/evidence/EVIDENCE-20260622-rag-poc-context-builder.md`。
- Acceptance：`docs/acceptance/ACCEPT-20260622-rag-poc-context-builder.md`。
