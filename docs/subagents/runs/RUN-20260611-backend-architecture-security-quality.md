# RUN-20260611 backend architecture Security & Quality 子代理报告

## 范围

只读分析 `backend-architecture-todolist.md` 后续增强中涉及权限持续维护、新增依赖、外部 provider、告警推送、token 预算门禁和 VectorDB 运维的安全质量风险。

## 结论

总体风险为 MEDIUM。当前默认关闭、脱敏、RBAC/IDOR 回归和外部服务 opt-in 基础较好；后续增强的重点应是把出站网络治理、分域 token 门禁、VectorDB 运维校验和持续权限抽样制度化。

## 关键风险

| 风险 | 等级 | 建议 |
|---|---:|---|
| Provider baseUrl / alert webhook 出站请求可能形成 SSRF 面 | MEDIUM | 后续建立统一 outbound URL policy，优先 HTTPS + allowlist |
| Provider API key 已 AES-GCM 加密，但缺少生产轮换设计 | LOW-MEDIUM | 后续增加 key version 和轮换窗口 |
| Token 预算门禁当前以全局统计为主 | MEDIUM | 后续升级到 user/course/agent 维度的调用前门禁 |
| VectorDB 默认关闭与 payload 最小化良好，但真实运维需 smoke/dimension gate | MEDIUM | 生产启用时要求 expected dimension 并纳入 health |
| 告警 webhook payload 必须保持 allowlist | LOW-MEDIUM | 禁止直接序列化内部 alert 对象 |

## 推荐权限抽样矩阵

- Student/Teacher 访问 `/api/admin/model-providers*` 必须 `FORBIDDEN`。
- Student/Teacher 访问 token governance、ops alerts persisted/ack 必须 `FORBIDDEN`。
- Teacher foreign course 的 KB list/query/reindex/index-task detail 必须拒绝或列表脱敏。
- Vector retrieval 返回 forbidden `chunkId` 后必须被 allowed KB 过滤丢弃。
- Orchestrator replay 在 scope 变化后必须重新鉴权，不返回旧 workflow/task/trace。
- Provider test/webhook 后续不得允许任意私网、metadata IP 或未审查内网 host。

## 测试建议

```powershell
cd backend
mvn --% -Dtest=BusinessPermissionMatrixRegressionTest,SecurityFilterChainTest,SecurityJwtAuthenticationTest test
mvn --% -Dtest=AnalyticsControllerTest,TokenBudgetGateServiceTest test
mvn --% -Dtest=ModelProviderControllerTest,ModelProviderServiceTest,ModelProviderSecretCodecTest,AiModelGatewayTest,EmbeddingServiceTest test
mvn --% -Dtest=RagVectorConfigurationTest,QdrantVectorIndexAdapterTest,ChunkServiceVectorRetrievalTest,IndexServiceVectorPayloadTest test
```

## 备注

本地 `D:\多元agent` 不是 git repository，无法执行 git history secrets scan。依赖审计建议在可联网 CI 中运行 OWASP Dependency Check 和 npm audit。
