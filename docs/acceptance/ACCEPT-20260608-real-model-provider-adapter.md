# ACCEPT - P3-3 真实模型 Provider Adapter

## 1. 追踪

- PRD：`docs/product/PRD-20260608-real-model-provider-adapter.md`
- REQ：`docs/requirements/REQ-20260608-real-model-provider-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-real-model-provider-adapter.md`
- Evidence：`docs/evidence/EVIDENCE-20260608-real-model-provider-adapter.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：通过 Spring AI OpenAI-compatible starter 接入 Chat provider adapter。
- [x] FR-02：通过 Spring AI OpenAI-compatible starter 接入 Embedding provider adapter。
- [x] FR-03：默认 `AI_MODEL_PROVIDER=none` 时保持 deterministic/noop，本地测试不外呼。
- [x] FR-04：provider 配置完整但缺 Spring AI bean 时 fail closed，不返回虚假成功。
- [x] FR-05：Chat provider 输出必须是 JSON，资源生成路径继续执行 schema validation。
- [x] FR-06：Embedding provider 响应必须返回与 chunk 数量一致的非空向量。
- [x] FR-07：raw provider error、prompt、chunk、secret 不泄露到业务错误码。

### 非功能验收

- [x] NFR-01：新增依赖已通过 dependency review。
- [x] NFR-02：不引入 DashScope / VectorDB / DB migration / frontend 变更。
- [x] NFR-03：Spring AI OpenAI starter 的未使用 audio/image/moderation auto-config 已显式禁用。
- [x] NFR-04：全量后端 Maven 测试通过。

### 架构验收

- [x] Frontend 未直接调用 LLM。
- [x] API key 未写入前端或仓库文件。
- [x] 模型 SDK 仅在 `AiModelGateway` / `EmbeddingService` 内部使用。
- [x] Agent 结构化输出仍由 gateway 校验。
- [x] RAG embedding 仍通过 service boundary，不直接绕过 VectorIndexAdapter。
- [x] 未提交真实 secret。

### 文档验收

- [x] PRD / REQ / SPEC / PLAN / TASK / Context Pack 已创建。
- [x] Dependency Review 已创建。
- [x] Evidence 已创建。
- [x] Acceptance 已创建。
- [x] Changelog / Memory / TODO 已更新。
- [x] Retrospective 已创建。

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| Focused tests | PASS | `AiModelGatewayTest` + `EmbeddingServiceTest`，18 tests |
| Adjacent regression | PASS | Resource generation / RAG query / embedding vector 相关，53 tests |
| Dependency tree | PASS | `mvn dependency:tree` |
| Compile | PASS | `mvn compile` |
| Full backend tests | PASS | `mvn test`，357 run / 0 failures / 0 errors / 1 skipped |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 真实外部 provider smoke 未作为默认测试执行 | Medium | 后续在受控环境用 runtime env 验证 `AI_CHAT_MODEL=gpt-5.5` 与 embedding model |
| Spring AI Alibaba DashScope 未接入 | Low | 后续独立 P3-3 增强切片 |
| VectorDB 仍是 noop boundary | Medium | 后续 P3-2/P3-3 向量检索增强切片 |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

P3-3 “用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型”的最小 Spring AI OpenAI-compatible provider adapter 已完成。DashScope 与 VectorDB 作为后续增强，不再阻塞本切片验收。

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | ACCEPTED |
