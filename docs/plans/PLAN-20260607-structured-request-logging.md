# PLAN - 结构化请求日志

状态：已完成（2026-06-07）

## 1. Skill Selection Report

### Task Type

后端可观测性 / 运维质量增强。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 强制执行项目 Spec-first 开发流水线 |
| spring-boot-architecture | 约束 Spring Boot 过滤器、异常处理和 common 层边界 |
| agent-trace-design | 保持 `traceId` 传播和审计语义一致 |
| security-review | 控制日志字段白名单和敏感信息泄露风险 |
| test-generator | 设计日志捕获和异常路径测试 |
| architecture-drift-check | 确认不改 API、不改 DB、不新增依赖 |

### Missing Skills

暂无。

### GitHub Research Needed

否。现有项目技能和 Spring 官方文档足够。

### New Project-Specific Skill To Create

收尾阶段视复用价值沉淀“结构化请求日志白名单”技能。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 本切片虽然只影响后端 `common` 模块，但涉及日志脱敏、安全错误码和过滤器顺序，启用后端专家与安全质量专家做 L1 并行分析。

Parallelism Level: L1

Selected Subagents:

- Backend Expert
- Security & Quality

Implementation Mode: Single Codex after analysis integration

## 3. Confidence Check

| 检查项 | 结果 |
|---|---|
| 重复实现检查 | 通过，现有 `TraceFilter` 只做 traceId，不做请求完成日志 |
| 架构合规 | 通过，变更限定在 `common` 横切层 |
| 官方文档验证 | 通过，使用 Spring `HandlerMapping` route attribute 与 Spring Boot `OutputCaptureExtension` |
| OSS 参考 | 不需要，使用框架内置能力且不引入依赖 |
| 根因识别 | 通过，缺口是请求完成日志缺失，以及 errorCode 未传递给 filter |

Confidence: 0.95。可以进入实现。

## 4. 文件变更计划

计划修改：

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

收尾更新：

- `docs/evidence/EVIDENCE-20260607-structured-request-logging.md`
- `docs/acceptance/ACCEPT-20260607-structured-request-logging.md`
- `docs/retrospectives/RETRO-20260607-structured-request-logging.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`（仅当创建新技能）
- `docs/skills/project-specific/structured-request-logging.md`（仅当创建新技能）

## 5. 实施步骤

1. [x] 创建工作流文档和 Context Pack。
2. [x] 写 `StructuredRequestLoggingFilterTest`，先验证失败。
3. [x] 补 `TraceFilterTest` 覆盖非法 `X-Trace-Id` 替换。
4. [x] 新增 `StructuredRequestLoggingFilter`。
5. [x] 在 `GlobalExceptionHandler` 写入内部 `errorCode` request attribute。
6. [x] 收紧 `TraceFilter` 对 trace header 的安全校验。
7. [x] 运行定向测试。
8. [x] 运行全量 `mvn test`。
9. [x] 更新证据、验收、TODO、Changelog、Memory、Retrospective。

## 6. 风险与处理

- 过滤器顺序错误可能导致 `userId` 退回 `dev_user`：通过 `Ordered.HIGHEST_PRECEDENCE + 2` 和测试覆盖。
- `errorCode` 靠 HTTP status 猜测可能与业务错误码漂移：由 `GlobalExceptionHandler` 显式写 request attribute。
- 日志泄露敏感信息：只构造白名单字段，不读取 body/header/query。
- `OutputCaptureExtension` 捕获日志存在时序风险：测试断言在请求执行后读取输出，并保持日志同步输出。

## 7. 验证命令

```bash
cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,TraceFilterTest,GlobalExceptionHandlerTest test
cd backend && mvn test
```
