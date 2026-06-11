# PLAN-20260607 后端架构 TODO 完成计划

## Task 3 实施计划：P3-5-A 运维告警 API

Status: Done。聚焦测试、相邻回归与完整后端测试均已通过；证据见 `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`。

1. [x] 落盘 Observability/Ops、Security & Quality、Integration Reviewer 三份告警报告。
2. [x] 更新 PRD / REQ / SPEC / TASK / CONTEXT，将当前边界切换到 Task 3。
3. [x] TDD RED：在 `AnalyticsControllerTest` 增加告警 API 权限、默认值、四类告警和脱敏测试，并运行聚焦测试确认失败。
4. [x] GREEN：在 `AnalyticsController` / `AnalyticsService` 增加 `GET /api/analytics/ops/alerts` 和白名单 DTO。
5. [x] 运行聚焦测试：`mvn --% -Dtest=AnalyticsControllerTest test`。
6. [x] 运行相邻回归：`mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test`。
7. [x] 可行时运行完整后端测试：`mvn test`。
8. [x] 更新 Evidence / Acceptance / TODO / Changelog / Memory。

### Task 3 风险

| Risk | Mitigation |
|---|---|
| 当前身份模型仍是 `X-User-Id` 字符串 | 文档和测试只声明临时 admin-only，不声明真实 RBAC |
| 告警响应泄露敏感字段 | 使用新白名单 DTO；测试断言敏感字段名和敏感 seed 内容不存在 |
| `RAG_NO_SOURCE` 口径不够结构化 | 本切片采用 `retrievalCount <= 0`；后续 schema 强化另开任务 |
| 查询型告警不是外部推送 | 明确本切片只提供 query-time API |

## 1. 执行策略

采用专家 subagent 并行分析，主 Codex 串行集成实现。

## 2. 切片顺序

| Order | Slice | Scope | Parallelism |
|---|---|---|---|
| 1 | P3-4-A Course / Knowledge Catalog 权限收口 | `common/auth`, `knowledge` | 已完成，串行实现 |
| 2 | P3-4-B 对象级防枚举与 scoped query | learning/resource/rag object detail | 已完成，串行实现 |
| 3 | P3-5-A 告警规则服务 | observability/analytics | 已完成，专家分析落盘后由主 Codex 串行实现 |
| 4 | P3-3-A 模型网关结构化校验与日志补齐 | agent/model gateway | 需依赖审查前置判断 |
| 5 | P3-3-B 真实 Spring AI provider 接入 | gateway/config/dependency | 需官方文档与 dependency review |
| 6 | P3-2-A Embedding service 边界 | rag/index/model gateway | 与 P3-3 串行 |
| 7 | P3-2-B Hybrid retrieval/RRF/reranker fallback | rag/query/retrieval | 可在 embedding 后执行 |
| 8 | P3-2-C 复杂 PDF/DOCX/OCR | rag/parser | 可能需要新依赖审查 |

## 3. 第一轮实施步骤

1. 补充 `CourseKnowledgeControllerTest` 失败测试。
2. 运行聚焦测试确认 RED。
3. 修改 `CurrentUserService` 增加角色辅助方法。
4. 修改 `CourseController` / `KnowledgePointController` 传入 current user。
5. 修改 `KnowledgeCatalogService` 执行写权限检查。
6. 运行聚焦测试。
7. 运行相关安全/知识图谱测试。
8. 更新 Evidence / Acceptance / Memory / Changelog / TODO。

## 4. 风险

| Risk | Mitigation |
|---|---|
| 既有测试未带 `X-User-Id` 导致默认 dev_user 被视为 student 后失败 | 更新既有写入测试显式使用 teacher/admin header。 |
| 过渡角色模型仍依赖 userId 字符串 | 在文档中标记为 P3 后续 Slice A，不声称完成真实 RBAC。 |
| 收紧写权限影响 demo 数据创建 | 测试和本地请求使用 teacher/admin header。 |

## 5. 测试命令

```bash
cd backend && mvn --% -Dtest=CourseKnowledgeControllerTest test
cd backend && mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest test
cd backend && mvn test
```

## 6. 第二轮实施步骤

Status: Done。聚焦测试、相邻回归与完整后端测试均已通过；证据见 `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`。

1. 补充对象详情防枚举失败测试：
   - `LearningWorkflowControllerTest`
   - `ResourceGenerationControllerTest`
   - `DocumentControllerTest`
2. 运行聚焦测试确认 RED。
3. 将 owner/admin 授权判断下沉到 Service：
   - `LearningWorkflowService.getPathForUser(...)`
   - `ResourceGenerationService.getTask(...)` / `getLearnerResources(...)`
   - `AgentTraceGovernanceService.getTrace(...)`
   - `DocumentService.getDocument(...)` / `reindex(...)` / `getIndexTask(...)`
4. 运行聚焦测试确认 GREEN。
5. 运行相邻权限/RAG/资源生成回归。
6. 更新 Evidence / Acceptance / Memory / Changelog / TODO。
