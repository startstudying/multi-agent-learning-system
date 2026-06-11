# CONTEXT - Micrometer 运行指标

## 1. 当前任务

实现 P3-5 最小 Micrometer 业务指标：请求延迟、RAG 延迟、模型延迟、失败率、token 成本。

## 2. 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

## 3. 关联文档

- PRD：`docs/product/PRD-20260607-micrometer-indicators.md`
- REQ：`docs/requirements/REQ-20260607-micrometer-indicators.md`
- SPEC：`docs/specs/SPEC-20260607-micrometer-indicators.md`
- PLAN：`docs/plans/PLAN-20260607-micrometer-indicators.md`
- TASK：`docs/tasks/TASK-20260607-micrometer-indicators.md`
- TODO：`docs/planning/backend-architecture-todolist.md`
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 架构漂移检查：`docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- 可观测性文档：`docs/architecture/observability.md`
- 子代理报告：`docs/subagents/runs/RUN-20260607-micrometer-indicators.md`

## 4. 已选 Skills

- `feature-development-workflow`
- `brainstorming`
- `Confidence Check`
- `test-driven-development`
- `spring-ai-agent-backend`
- `agent-trace-governance`
- `security-review`
- `verification-before-completion`

## 5. Subagent 计划

### 是否启用 Subagent

是。

### 已完成专家

- Backend Expert：完成。
- Agent/RAG Expert：完成。

### 未完成专家

- Security & Quality：启动失败，原因是 agent thread limit reached。

### 补偿措施

Main Codex 本地执行安全审查：

- tags 白名单写入 SPEC。
- 禁止敏感/高基数 tag 写入测试断言。
- Actuator exposure 只增加 `metrics`，不开放高风险 endpoint。

### 并行级别

- [x] L1 - 并行分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

单 Codex 实现，不并行改代码。

## 6. 允许修改的文件

- `backend/src/main/java/com/learningos/common/observability/LearningOsMetrics.java`
- `backend/src/main/java/com/learningos/common/trace/StructuredRequestLoggingFilter.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/common/trace/StructuredRequestLoggingFilterTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `docs/product/PRD-20260607-micrometer-indicators.md`
- `docs/requirements/REQ-20260607-micrometer-indicators.md`
- `docs/specs/SPEC-20260607-micrometer-indicators.md`
- `docs/plans/PLAN-20260607-micrometer-indicators.md`
- `docs/tasks/TASK-20260607-micrometer-indicators.md`
- `docs/context/CONTEXT-20260607-micrometer-indicators.md`
- `docs/subagents/runs/RUN-20260607-micrometer-indicators.md`
- `docs/evidence/EVIDENCE-20260607-micrometer-indicators.md`
- `docs/acceptance/ACCEPT-20260607-micrometer-indicators.md`
- `docs/retrospectives/RETRO-20260607-micrometer-indicators.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/observability.md`
- `docs/skills/SKILL_REGISTRY.md`（仅创建新技能时）
- `docs/skills/project-specific/micrometer-observability.md`（仅创建新技能时）

## 7. 禁止修改的文件

- `docs/superpowers/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- 业务 API DTO 和 Controller 合同
- 数据库实体 schema 字段，除非另建 SPEC，本切片禁止

## 8. 测试命令

```bash
cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,RagQueryServiceTest,AiModelGatewayTest,AgentRunRecorderTest test
cd backend && mvn test
```

## 9. 当前任务边界

本次只完成 Micrometer 指标基础。健康检查深度、告警、Prometheus/Grafana、真实模型 provider SLA、完整 RBAC 或 RAG 生产检索增强均不在本切片内。

