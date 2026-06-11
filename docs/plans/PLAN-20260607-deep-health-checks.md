# PLAN - 深度健康检查
状态：已完成（2026-06-07）

## 1. Skill Selection Report

### Task Type

后端运维可观测性 / 健康检查增强 / 安全脱敏。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 按项目规则执行完整 Spec-first 流程 |
| brainstorming | 健康检查需要先明确探测深度、外连边界和脱敏策略 |
| Confidence Check | 实现前确认当前缺口、重复能力、架构约束 |
| test-driven-development | 新增健康语义必须先写失败测试 |
| java-security-review | 健康接口涉及敏感配置和外连探测，必须审查泄露风险 |
| verification-before-completion | 完成前必须有新鲜测试证据 |

### Missing Skills

暂不需要外部技能。本切片收尾时沉淀项目专用技能 `deep-health-checks`，用于后续依赖健康探测和脱敏响应设计。

### GitHub Research Needed

否。现有 Spring Boot、DataSource、Redis、MinIO SDK 能力足够，不新增外部依赖。

### New Project-Specific Skill To Create

创建 `docs/skills/project-specific/deep-health-checks.md`。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 本切片涉及后端依赖探测和安全脱敏，影响 health、config、Redis、MinIO、model provider 边界。

Parallelism Level: L1

Selected Subagents:

- Backend/Architecture Expert：分析健康检查探测边界与实现策略。
- Security Reviewer：分析敏感信息泄露、SSRF、外部探测和 actuator 暴露风险。

Implementation Mode: Single Codex after analysis integration

## 3. Confidence Check

| 检查项 | 结果 |
|---|---|
| 重复实现检查 | 通过。当前 `HealthService` 只做配置态检查，未做数据库/Redis 连通性探测 |
| 架构合规 | 通过。增强点集中在 `HealthService` 和 DTO，Controller 路径不变 |
| 依赖检查 | 通过。已有 `spring-boot-starter-data-redis`、MinIO SDK、DataSource，无需新增依赖 |
| 安全根因 | 明确。健康检查要增强诊断，但必须继续脱敏，不能输出 endpoint/secret/raw exception |
| 风险评估 | 可控。模型 provider 本切片只做配置态，不做真实外部调用 |

Confidence: 0.93。可进入文档和 TDD。

## 4. 实施步骤

1. [x] 读取项目记忆、P3-5 TODO、健康检查现有代码和测试。
2. [x] 集成 Backend/Architecture 与 Security Reviewer 报告。
3. [x] 创建 PRD/REQ/SPEC/PLAN/TASK/Context Pack。
4. [x] 写健康检查失败测试，确认 RED。
5. [x] 实现 `HealthService` 深度检查和 DTO 状态。
6. [x] 运行定向测试。
7. [x] 运行全量 `mvn test`。
8. [x] 更新 Evidence、Acceptance、TODO、Changelog、Memory、Retrospective。

## 5. 风险与处理

| 风险 | 处理 |
|---|---|
| 健康响应泄露内部地址或凭据 | metadata 只输出布尔值和稳定错误码，测试断言敏感值不存在 |
| Redis/MinIO 不可用导致接口 500 | 单组件 try/catch，组件返回 `DOWN`，整体 envelope 仍为 OK |
| 探测造成外部副作用 | Redis 只 ping，MinIO 只构造 client，model 不调用外部 API |
| actuator 暴露面扩大 | 本切片不修改 actuator exposure |

## 6. 验证命令

```bash
cd backend && mvn --% -Dtest=HealthControllerTest test
cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest,StructuredRequestLoggingFilterTest test
cd backend && mvn test
```

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | `HealthController` 继续只委托 `HealthService`，健康探测集中在 application service |
| Frontend rules | PASS | 不涉及 frontend |
| Agent / RAG rules | PASS | 不调用模型 provider，不改 Agent/RAG 链路 |
| Security | PASS | 响应 metadata 仅包含布尔值、稳定状态和错误码；不输出原始异常或凭据 |
| API / Database | PASS | `/api/health` 路径和统一 envelope 不变；无 DB schema 变更 |
