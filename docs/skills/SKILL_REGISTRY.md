# Skill Registry

| Skill | Type | Use When | Source | Status |
|---|---|---|---|---|
| feature-development-workflow | workflow | 用户提出原始需求时自动走 S/M/L 分级开发流水线 | `.agents/skills/` | active |
| prd-writer | product | 写产品决策、MVP、非目标 | project | active |
| requirement-analysis | requirement | 分析用户需求、输入输出、用户流程 | project | active |
| spec-writer | workflow | 写系统规格、接口、状态、验收 | project | active |
| plan-writer | workflow | 写开发计划、文件变更、风险 | project | active |
| task-breakdown | workflow | 拆小任务、定义 Done Criteria | project | active |
| vue3-component-design | frontend | Vue 页面、组件、状态管理 | project | active |
| ai-streaming-ui | frontend | SSE 流式输出、AI 对话界面 | project | active |
| dashboard-visualization | frontend | 图表、看板、可视化 | project | active |
| spring-boot-architecture | backend | Spring Boot 分层、Service、Controller | project | active |
| api-contract-design | backend | API 合同、请求响应、错误码 | project | active |
| database-design | database | 表结构、索引、状态流转 | project | active |
| spring-ai-agent-tool | agent | Spring AI Tool、Agent 编排 | project-specific | active |
| agent-workflow-design | agent | 多智能体流程、Agent DAG | project | active |
| rag-project-review | rag | RAG 检索、引用、评估 | project | active |
| agent-trace-design | agent | Agent Trace、工具调用记录 | project-specific | active |
| security-review | security | 权限、安全、敏感数据 | project | active |
| dependency-review | security | 新增依赖审查 | project | active |
| architecture-drift-check | quality | 检查架构漂移 | project | active |
| test-generator | quality | 单元测试、集成测试 | project | active |
| changelog-writer | delivery | 更新 Changelog | project | active |
| retrospective-skill-extraction | workflow | 复盘并沉淀新 Skill | project | active |
| rag-parser-boundary | rag | RAG document parser adapter boundary, configurable/process OCR fallback provider boundary, real PDFBox/POI SDK providers, resource limits, safe parse errors, shared manual/worker indexing path | project-specific | active |
| object-scope-authorization | security | Backend object-level authorization, parent-scope checks, course/enrollment/RAG document metadata/assessment record detail/list/grading-evaluation scope, IDOR prevention, missing-vs-forbidden response hardening, and list pagination redaction | project-specific | active |
| auth-context-boundary | security | Backend authentication context, Bearer JWT validation, dev/test identity fallback, production header-spoofing prevention, and roles-first RBAC helper rules | project-specific | active |
| structured-request-logging | observability | Backend HTTP request logging, field whitelisting, traceId safety, route/status/latency/errorCode logging | project-specific | active |
| micrometer-observability | observability | Backend Micrometer metrics, low-cardinality tags, runtime latency/failure/token/cost meters, and Actuator metrics exposure | project-specific | active |
| deep-health-checks | observability | Backend dependency health checks, low-cost probes, fixed error codes, and sensitive configuration redaction | project-specific | active |
| ops-alerting | observability | Backend operations alerting, query-time alert aggregation, slow RAG/model/no-source/review-backlog risk views, and alert response redaction | project-specific | active |
| model-gateway-boundary | agent | Model gateway structured output validation, safe provider errors, provider normalization/persistence, and gateway-sourced model/token/cost logs | project-specific | active |
| rag-hybrid-retrieval | rag | RAG allowed-KB keyword/recency/RRF retrieval, reranker fallback status, vector-disabled metadata, and safe sourcesJson | project-specific | active |
| rag-embedding-vector-adapter | rag | RAG embedding service contract, optional VectorDB adapter boundary, noop/default states, allowed-KB vector hit filtering, and safe vector metadata | project-specific | active |

## Project-Specific Skills

See `docs/skills/project-specific/` for detailed skill documents:

- `rag-parser-boundary.md` - RAG parser adapter boundary, configurable/process OCR fallback boundary, and safe parser error handling
- `resource-citation-governance.md` - AI generated resource citation governance and NO_SOURCE review gate
- `object-scope-authorization.md` - Object-level authorization, course/enrollment/RAG document metadata/assessment record and grading-evaluation scope, and missing-vs-forbidden response hardening
- `auth-context-boundary.md` - Authentication context boundary, Bearer JWT validation, dev/test fallback, and roles-first RBAC helper rules
- `structured-request-logging.md` - HTTP request log field whitelist, traceId safety, and errorCode propagation
- `micrometer-observability.md` - Micrometer business metrics, low-cardinality tags, and Actuator metrics exposure
- `deep-health-checks.md` - Dependency health probes, safe status semantics, and health response redaction
- `ops-alerting.md` - Query-time operations alert aggregation, safe alert DTOs, and alert response redaction
- `model-gateway-boundary.md` - Model gateway schema validation, provider normalization/persistence, safe errors, and model-call evidence source rules
- `rag-hybrid-retrieval.md` - RAG keyword/recency/RRF retrieval, reranker fallback, and safe retrieval metadata rules
- `rag-embedding-vector-adapter.md` - RAG embedding/vector adapter boundary, noop behavior, allowed-KB vector filtering, and safe metadata rules

- `agent-trace-design.md` — Agent Trace 设计与实现
- `rag-citation-viewer.md` — RAG 引用展示
- `spring-ai-agent-tool.md` — Spring AI Tool 封装
- `vue-ai-learning-ui.md` — Vue AI 学习界面
