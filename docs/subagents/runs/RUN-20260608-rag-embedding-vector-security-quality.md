# RUN-20260608 RAG Embedding / Vector Adapter Security & Quality

## 1. 审查目标

对 P3-2 `增加 embedding service 和可选 VectorDB adapter` 做安全、依赖、schema 与质量边界审查。

## 2. 依赖结论

本切片建议不新增 Maven dependency。

原因：

- 当前 `backend/pom.xml` 已引入 Spring AI BOM，但未引入具体 provider starter。
- 真实 provider / VectorDB SDK 会引入网络、凭据、许可证、CVE、运维和数据出境风险。
- AGENTS 明确新增依赖必须创建 `docs/security/` dependency review。

若后续新增依赖，必须补：

- `docs/security/DEPENDENCY-*.md`
- 许可证兼容性
- 维护状态
- CVE / advisories
- 传输与凭据配置方式
- 是否会把 chunk/text/embedding 发送到外部服务

## 3. Secret / API key 红线

- API key 不得写入代码、测试 fixture、memory、changelog、trace、query log。
- 配置只允许通过环境变量或 secret manager。
- frontend 不得持有 embedding provider key。
- 健康检查、错误信息、metadata 不得输出 endpoint credential、token、provider raw error。
- fake provider 测试中可使用 `sk-test` 字符串验证脱敏，但不得是真实 key。

## 4. Embedding / Vector 泄露风险

| 风险 | 说明 | 控制 |
|---|---|---|
| raw chunk 发送到外部 provider | embedding 必然消费 chunk text | 真实 provider 切片需用户/部署层明确数据出境策略 |
| metadata 存完整 vector | float array 可能膨胀并间接泄露内容特征 | metadata 只写 hash、dimension、status、model |
| VectorDB 存 forbidden chunk | search 可能返回无权限结果 | search request 带 allowed KB，服务层二次校验 |
| stale vector | reindex 后旧向量仍可命中 | upsert 前 delete/覆盖 document version vector |
| raw provider error 泄露 | provider error 可能含 prompt、endpoint、key | 只写安全错误码 |

## 5. DB schema 风险

最小切片不建议新增 schema。

如后续新增 V18：

- 不应在 `kb_doc_chunk` 堆多个大 varchar/json 字段导致 MySQL row-size 风险。
- 不保存完整 vector 数组。
- vector 状态字段应可查询、低基数、可恢复。
- MySQL smoke 必须覆盖 V18。

## 6. Permission filter 顺序要求

必须保持：

```text
requested kbIds
-> PermissionService.requireReadableKbIds(...)
-> ChunkService / vector search 只接收 allowedKbIds
-> VectorDB metadata filter by allowedKbIds
-> MySQL chunk reload 后再次校验 chunk.kbId in allowedKbIds
-> RRF / reranker
```

禁止：

- 全库 vector search 后只在 response 层过滤。
- 把 forbidden hits 写入 query log / trace / metrics。
- 让 provider / prompt 参与权限判断。

## 7. 必测安全场景

- adapter disabled 不调用外部 provider。
- provider raw error 中含 `apiKey=sk-test`，最终 task/query metadata 不含该字符串。
- vector adapter 返回 forbidden KB chunk，服务层必须剔除。
- allowed KB 无 chunk、vector fake 只返回 forbidden candidate 时，最终 no-source，不写 citation。
- reindex 同 document 后旧 vector 被 delete/覆盖。
- vector timeout/error 时 query fallback 到 keyword/recency，不暴露 raw error。
- metrics tag 不包含 query、chunkId、documentId、requestId、traceId、provider endpoint。

## 8. 最小安全边界

- 默认 disabled/noop。
- 不新增依赖。
- 不改 public API。
- 不保存完整 vector。
- 不在 logs/metadata 中保存 raw provider payload/error。
- query 阶段 fail-open 到 keyword/recency；index 阶段 adapter enabled 且 upsert 失败应 fail/retry，避免伪 INDEXED。

## 9. 结论

安全建议与集成计划一致：先做 boundary/noop/fake，不接真实 provider/VectorDB。真实 provider 与真实 VectorDB 单独立项，并在该切片强制 dependency review、secret policy、schema/migration smoke 和权限渗透测试。
