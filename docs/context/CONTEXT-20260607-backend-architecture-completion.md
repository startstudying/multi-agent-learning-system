# CONTEXT-20260607 后端架构 TODO 完成计划

## Task 3 Context Pack Override：P3-5-A 运维告警 API

### Status

Done。`GET /api/analytics/ops/alerts` 已实现并通过聚焦测试、相邻回归和完整后端测试；证据见 `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`。

### Current Task Boundary

只实现 query-time 告警 API：

```text
GET /api/analytics/ops/alerts
```

覆盖四类告警：`SLOW_RAG_QUERY`、`SLOW_MODEL_CALL`、`RAG_NO_SOURCE`、`REVIEW_BACKLOG`。

### Selected Skills

- feature-development-workflow
- multi-agent-coder
- subagent-driven-development
- agent-trace-governance
- spring-ai-agent-backend
- structured-request-logging
- micrometer-observability
- deep-health-checks
- test-driven-development
- verification-before-completion

### Subagent Plan

- Observability/Ops Expert：已完成告警取数与 API 口径分析。
- Security & Quality Expert：已完成 admin-only、脱敏和测试建议。
- Integration Reviewer：已合并 endpoint、DTO、阈值和风险决议。
- Implementation Mode：主 Codex 串行实现；不并行改同一文件。

### Files Allowed To Modify

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-observability.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-security.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-integration.md`
- `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`
- `docs/acceptance/ACCEPT-20260607-backend-architecture-completion.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/ops-alerting.md`
- `docs/retrospectives/RETRO-20260607-backend-architecture-completion-ops-alerting.md`
- `docs/tasks/TASK-20260607-backend-architecture-completion.md`
- `docs/context/CONTEXT-20260607-backend-architecture-completion.md`

### Files Not Allowed To Modify

- Frontend files.
- Database migration files.
- `backend/pom.xml`.
- Agent/model gateway execution files.
- RAG parser/indexer implementation files.
- `docs/superpowers/**`.

### Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test
mvn test
```

## 1. Related Memory and Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-model.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-observability.md`

## 2. Selected Skills

- feature-development-workflow
- multi-agent-coder
- subagent-driven-development
- spring-ai-agent-backend
- educational-rag-pipeline
- java-security-review
- object-scope-authorization
- test-driven-development
- verification-before-completion

## 3. Subagent Plan

- Backend Expert：已完成 P3-3 分析。
- Security & Quality Expert：已完成 P3-4 分析。
- Agent/RAG Expert：已重派 P3-2 分析，等待或后续集成。
- Model Boundary Expert：已完成 P3-3 分析；报告由主 Codex 落盘。
- Observability/Ops Expert：已完成 P3-5 分析；报告由主 Codex 落盘。
- Integration Reviewer：主 Codex 汇总并串行实现。

## 4. Current Task Boundary

Task 3：P3-5-A 运维告警 API 已完成。

下一次实现前应更新本 Context Pack 或创建新的 Context Pack，将边界切换到 Task 4：P3-3-A 模型网关结构化校验与日志补齐。

## 5. Files Allowed To Modify

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-observability.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-security.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-integration.md`
- `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`
- `docs/acceptance/ACCEPT-20260607-backend-architecture-completion.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/ops-alerting.md`
- `docs/retrospectives/RETRO-20260607-backend-architecture-completion-ops-alerting.md`
- `docs/tasks/TASK-20260607-backend-architecture-completion.md`
- `docs/context/CONTEXT-20260607-backend-architecture-completion.md`

## 6. Files Not Allowed To Modify In Current Task

- Frontend files.
- Database migration files.
- Agent/model gateway files.
- Build dependency files.
- Course/knowledge catalog files.
- Learning/resource/RAG implementation files outside analytics alert aggregation.

## 7. Test Commands

```bash
cd backend && mvn --% -Dtest=AnalyticsControllerTest test
cd backend && mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test
cd backend && mvn test
```

## 8. Architecture Constraints

- Controller 只读取 current user 和请求参数。
- 权限检查在 Service 层。
- 非 admin 对 missing/foreign 对象详情返回同类安全错误，避免对象存在性探测。
- 不依赖 Prompt 做权限。
- 不新增依赖。
- 不修改数据库 schema。
