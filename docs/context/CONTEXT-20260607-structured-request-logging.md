# CONTEXT - 结构化请求日志

## 1. 当前任务

实现 P3-5 最小可观测性切片：后端 HTTP 请求完成结构化日志。

## 2. 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`

## 3. 关联文档

- PRD：`docs/product/PRD-20260607-structured-request-logging.md`
- REQ：`docs/requirements/REQ-20260607-structured-request-logging.md`
- SPEC：`docs/specs/SPEC-20260607-structured-request-logging.md`
- PLAN：`docs/plans/PLAN-20260607-structured-request-logging.md`
- TASK：`docs/tasks/TASK-20260607-structured-request-logging.md`
- TODO：`docs/planning/backend-architecture-todolist.md`
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 架构漂移检查：`docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 4. 已选 Skills

- `feature-development-workflow`
- `spring-boot-architecture`
- `agent-trace-design`
- `security-review`
- `test-generator`
- `architecture-drift-check`
- `test-driven-development`
- `verification-before-completion`

## 5. Subagent 计划

### 是否启用 Subagent

是。

### 原因

本切片涉及后端过滤器顺序、异常错误码传递和日志脱敏。采用 L1 并行分析，主 Codex 负责集成与实现。

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 1 | 否 | 是 |

### 选中的专家

- Backend Expert
- Security & Quality

### 并行级别

- [x] L1 - 仅并行分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

仅并行分析，单 Codex 实现。

### 文件归属

| 领域 | 负责方 | 允许修改的文件 |
|---|---|---|
| 后端 common | Main Codex | `backend/src/main/java/com/learningos/common/**` |
| 测试 | Main Codex | `backend/src/test/java/com/learningos/common/**` |
| 文档 | Main Codex | 本切片 `docs/**` 文件 |

## 6. 关联代码区域

- `backend/src/main/java/com/learningos/common/trace/TraceFilter.java`
- `backend/src/main/java/com/learningos/common/trace/TraceContext.java`
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/main/java/com/learningos/common/auth/UserContextHolder.java`
- `backend/src/main/java/com/learningos/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/learningos/common/api/ErrorCode.java`
- `backend/src/test/java/com/learningos/common/trace/TraceFilterTest.java`
- `backend/src/test/java/com/learningos/common/exception/GlobalExceptionHandlerTest.java`

## 7. 允许修改的文件

- `backend/src/main/java/com/learningos/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/learningos/common/trace/TraceFilter.java`
- `backend/src/main/java/com/learningos/common/trace/StructuredRequestLoggingFilter.java`
- `backend/src/test/java/com/learningos/common/trace/TraceFilterTest.java`
- `backend/src/test/java/com/learningos/common/trace/StructuredRequestLoggingFilterTest.java`
- `docs/product/PRD-20260607-structured-request-logging.md`
- `docs/requirements/REQ-20260607-structured-request-logging.md`
- `docs/specs/SPEC-20260607-structured-request-logging.md`
- `docs/plans/PLAN-20260607-structured-request-logging.md`
- `docs/tasks/TASK-20260607-structured-request-logging.md`
- `docs/context/CONTEXT-20260607-structured-request-logging.md`
- `docs/subagents/runs/RUN-20260607-structured-request-logging.md`
- `docs/evidence/EVIDENCE-20260607-structured-request-logging.md`
- `docs/acceptance/ACCEPT-20260607-structured-request-logging.md`
- `docs/retrospectives/RETRO-20260607-structured-request-logging.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`（仅当创建项目技能）
- `docs/skills/project-specific/structured-request-logging.md`（仅当创建项目技能）

## 8. 禁止修改的文件

- `docs/superpowers/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `backend/src/main/java/com/learningos/**` 中除 `common` 以外的业务模块
- `backend/src/test/java/com/learningos/**` 中除 `common` 以外的测试模块

## 9. 测试命令

```bash
cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest test
cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,TraceFilterTest,GlobalExceptionHandlerTest test
cd backend && mvn test
```

## 10. 任务边界

本次只完成请求完成结构化日志。不实现 Micrometer 指标、深度健康检查、告警、日志平台接入、JSON encoder、认证体系改造、API 契约变更或数据库迁移。
