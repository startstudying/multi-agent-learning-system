# PLAN - Micrometer 运行指标
状态：已完成（2026-06-07）

## 1. Skill Selection Report

### Task Type

后端可观测性 / 运维质量增强 / Agent 与 RAG 指标治理。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 强制执行项目 Spec-first 开发流水线 |
| brainstorming | 需求涉及新增运行时指标语义，需要先形成设计；项目规则禁止写入 `docs/superpowers/**`，所以设计落在本工作流文档 |
| Confidence Check | 实现前确认无重复埋点、架构合规、根因明确 |
| test-driven-development | 新增行为必须先写失败测试 |
| spring-ai-agent-backend | 模型网关、Agent recorder、token 统计属于 Agent 后端边界 |
| agent-trace-governance | 指标不得替代 trace/model/token 审计事实表，并需避免敏感上下文进入 tags |
| security-review | 控制 Actuator 暴露面、敏感信息和高基数 tag 风险 |
| verification-before-completion | 完成前必须用新鲜测试证据支撑结论 |

### Missing Skills

暂缺项目专用 Micrometer 指标技能。收尾时如模式可复用，创建 `docs/skills/project-specific/micrometer-observability.md`。

### GitHub Research Needed

否。现有项目边界清晰，Micrometer/Spring Boot 官方文档足够。

### New Project-Specific Skill To Create

计划在收尾阶段沉淀“Micrometer 低基数业务指标”项目技能。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 本切片影响 `common`、`rag`、`agent` 和配置，且涉及 Agent/RAG 与安全治理。

Parallelism Level: L1

Selected Subagents:

- Backend Expert：已返回后端落点与测试建议。
- Agent/RAG Expert：已返回 RAG/model/token 语义边界建议。
- Security & Quality：尝试启动失败，原因是 agent thread limit reached；由 Main Codex 本地执行安全审查并记录补偿措施。

Implementation Mode: Single Codex after analysis integration

## 3. Confidence Check

| 检查项 | 结果 |
|---|---|
| 重复实现检查 | 通过。`rg` 未发现业务 `MeterRegistry`、`Timer`、`Counter`、`DistributionSummary` 埋点 |
| 架构合规 | 通过。埋点放在 filter/service/gateway/recorder，不放 Controller，不改 DB/API |
| 官方文档验证 | 通过。Micrometer Timer/Counter/DistributionSummary 与 Spring Boot Actuator metrics exposure 语义已确认 |
| OSS 参考 | 不需要。使用 Spring Boot Actuator 与 Micrometer 官方能力，无新增依赖 |
| 根因识别 | 通过。当前缺口是业务事件发生时未写入 Micrometer registry，且 metrics endpoint 未暴露 |

Confidence: 0.95。可以进入实现。

## 4. 方案取舍

### 方案 A：直接在业务类注入 `MeterRegistry`

优点：改动最少。

缺点：meter name 和 tag 策略分散，容易引入高基数 tag。

### 方案 B：新增 `LearningOsMetrics` 适配层（采用）

优点：集中控制 meter name、tag 白名单、fallback 和测试断言；业务类只表达事件语义。

缺点：多一个内部服务类。

### 方案 C：使用 AOP / `@Timed`

优点：横切侵入少。

缺点：无法可靠表达 RAG `outcome/strategy/no_source/replay`、模型 `status`、token 数值等业务维度。

## 5. 集成后的设计决策

- HTTP 指标放在 `StructuredRequestLoggingFilter`，复用已有 latency/errorCode/route 计算。
- RAG 指标放在 `RagQueryService`，fresh query 记录 latency，replay 只记录 count。
- 模型 latency 放在 `AiModelGateway`，以真实网关边界耗时为准。
- token/cost 放在 `AgentRunRecorder`，以写入 `token_usage_log` 的事实为准。
- `traceId/userId/requestId/agentTaskId/kbId/question/prompt/source/errorMessage` 禁止进入 tags。
- `/actuator/metrics` 默认暴露，且只新增 `metrics`，不扩大到高风险 endpoint。

## 6. 文件变更计划

计划修改：

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

计划新增或更新文档：

- `docs/product/PRD-20260607-micrometer-indicators.md`
- `docs/requirements/REQ-20260607-micrometer-indicators.md`
- `docs/specs/SPEC-20260607-micrometer-indicators.md`
- `docs/plans/PLAN-20260607-micrometer-indicators.md`
- `docs/tasks/TASK-20260607-micrometer-indicators.md`
- `docs/context/CONTEXT-20260607-micrometer-indicators.md`
- `docs/subagents/runs/RUN-20260607-micrometer-indicators.md`

收尾更新：

- `docs/evidence/EVIDENCE-20260607-micrometer-indicators.md`
- `docs/acceptance/ACCEPT-20260607-micrometer-indicators.md`
- `docs/retrospectives/RETRO-20260607-micrometer-indicators.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/observability.md`
- `docs/skills/SKILL_REGISTRY.md`（如创建新技能）
- `docs/skills/project-specific/micrometer-observability.md`（如创建新技能）

## 7. 实施步骤

1. [x] 读取项目记忆、架构基线、P3-5 TODO 和相关代码。
2. [x] 集成 Backend Expert 与 Agent/RAG Expert 报告。
3. [x] 完成本轮 PRD/REQ/SPEC/PLAN/TASK/Context Pack。
4. [x] 写 HTTP metrics 测试并确认失败。
5. [x] 写 RAG metrics 测试并确认失败。
6. [x] 写 model/token metrics 测试并确认失败。
7. [x] 实现 `LearningOsMetrics` 和三处业务埋点。
8. [x] 暴露 `metrics` actuator endpoint。
9. [x] 运行定向测试。
10. [x] 运行全量 `mvn test`。
11. [x] 更新 Evidence、Acceptance、TODO、Changelog、Memory、Retrospective 和技能沉淀。

## 11. 完成证据

| 验证项 | 结果 |
|---|---|
| 定向 Micrometer 测试 | `28 tests, 0 failures, 0 errors` |
| MVC slice 回归测试 | `9 tests, 0 failures, 0 errors` |
| 全量后端测试 | `244 tests, 0 failures, 0 errors, 1 skipped` |
| 架构漂移 | 无新增依赖、无 API/DB 变更、无 Controller 埋点 |
| 子代理限制 | Security & Quality / code review 子代理启动受 `agent thread limit reached` 限制，已本地补偿审查 |

## 8. 风险与处理

| 风险 | 处理 |
|---|---|
| 高基数 tag 导致 metrics 系统不可用 | `LearningOsMetrics` 集中白名单，测试遍历 meter tag keys |
| 指标泄露学习内容或 Prompt | 禁止 question/prompt/source/errorMessage 进入 tags，仅记录数值 |
| replay 污染真实 RAG latency | replay 只记录 count，不记录 duration |
| `/actuator/metrics` 暴露面扩大 | 只新增 `metrics`，文档注明生产需访问控制，不暴露高风险 endpoint |
| 模型 latency 与持久化日志 latency 口径不一致 | metrics 采用网关真实耗时；`model_call_log` 保持现状，后续 P3-3 真实模型接入时再统一日志 latency |

## 9. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 埋点位于 filter/service/gateway/recorder，不改变 Controller 合同 |
| Frontend rules | PASS | 不涉及 frontend |
| Agent / RAG rules | PASS | 不改变 trace/citation/model log 持久化语义 |
| Security | PASS | 无新增依赖；禁止敏感/高基数 tags |
| API / Database | PASS | 不改业务 API，不改 DB schema |

## 10. 验证命令

```bash
cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,RagQueryServiceTest,AiModelGatewayTest,AgentRunRecorderTest test
cd backend && mvn test
```
