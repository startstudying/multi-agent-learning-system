# CONTEXT - 深度健康检查

## 1. 当前任务

实现 P3-5 深度健康检查：数据库、Redis、MinIO、模型 provider。

## 2. 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

## 3. 关联文档

- PRD：`docs/product/PRD-20260607-deep-health-checks.md`
- REQ：`docs/requirements/REQ-20260607-deep-health-checks.md`
- SPEC：`docs/specs/SPEC-20260607-deep-health-checks.md`
- PLAN：`docs/plans/PLAN-20260607-deep-health-checks.md`
- TASK：`docs/tasks/TASK-20260607-deep-health-checks.md`
- TODO：`docs/planning/backend-architecture-todolist.md`
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 可观测性文档：`docs/architecture/observability.md`
- 子代理报告：`docs/subagents/runs/RUN-20260607-deep-health-checks.md`

## 4. 已选 Skills

- `feature-development-workflow`
- `brainstorming`
- `Confidence Check`
- `test-driven-development`
- `java-security-review`
- `verification-before-completion`

## 5. Subagent 计划

### 是否启用 Subagent

是。

### 专家

- Backend/Architecture Expert：健康检查探测边界。
- Security Reviewer：敏感信息、SSRF、外连和 actuator 暴露风险。

### 并行级别

- [x] L1 - 并行分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

单 Codex 实现，不并行改代码。

## 6. 允许修改的文件

- `backend/src/main/java/com/learningos/health/application/HealthService.java`
- `backend/src/main/java/com/learningos/health/api/HealthDtos.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/health/api/HealthControllerTest.java`
- `backend/src/test/java/com/learningos/health/application/HealthServiceTest.java`
- `docs/product/PRD-20260607-deep-health-checks.md`
- `docs/requirements/REQ-20260607-deep-health-checks.md`
- `docs/specs/SPEC-20260607-deep-health-checks.md`
- `docs/plans/PLAN-20260607-deep-health-checks.md`
- `docs/tasks/TASK-20260607-deep-health-checks.md`
- `docs/context/CONTEXT-20260607-deep-health-checks.md`
- `docs/subagents/runs/RUN-20260607-deep-health-checks.md`
- `docs/evidence/EVIDENCE-20260607-deep-health-checks.md`
- `docs/acceptance/ACCEPT-20260607-deep-health-checks.md`
- `docs/retrospectives/RETRO-20260607-deep-health-checks.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/observability.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/deep-health-checks.md`

## 7. 禁止修改的文件

- `docs/superpowers/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- 业务 API DTO 和 Controller 合同
- 数据库实体 schema

## 8. 测试命令

```bash
cd backend && mvn --% -Dtest=HealthControllerTest test
cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest test
cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest,StructuredRequestLoggingFilterTest test
cd backend && mvn test
```

## 9. 当前任务边界

本切片只做 `/api/health` 的最小深度健康检查。告警、Dashboard、Prometheus/Grafana、真实模型 provider 调用、MinIO 对象读写、Redis 写入探测不在本次范围内。
