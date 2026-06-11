# 自动批改质量评估开发计划

## Skill Selection Report

## Task Type

后端 API / Service 增量，实现 TODO P2-3「自动批改质量评估」最小可验收切片。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求功能先走 PRD/REQ/SPEC/PLAN/TASK/Context，再实现和验收 |
| `assessment-feedback-agent` | 指标涉及评分、等级一致和错因分类一致 |
| `spring-ai-agent-backend` | 约束 Spring Boot Controller / Service / DTO 分层 |
| `test-driven-development` | 用户明确要求先写测试 RED，再写生产代码 GREEN |
| `Confidence Check` | 实现前确认重复实现、架构合规和根因 |
| `verification-before-completion` | 完成前必须提供新鲜测试证据 |

## Missing Skills

无。现有后端、测试和 assessment 相关技能足够覆盖。

## GitHub Research Needed

No。本切片是项目内离线指标计算，不新增外部框架或依赖。

## New Project-Specific Skill To Create

暂不创建。该切片模式简单，后续若扩展为批改评估长期治理再抽取技能。

## Subagent Decision

- Use Subagents: No
- Reason: 当前执行者已是并行后端 worker；改动限制在 assessment 单模块和本次文档，且不涉及 DB、RAG、Agent 或前端联动。
- Parallelism Level: L1 worker analysis report
- Selected Subagents: 无新增派发
- Implementation Mode: Single worker implementation

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementations | 通过；现有 `GradingEvaluationService` 只有分数数组基础指标，未实现 `samples`、错因一致率和分组分析 |
| Architecture compliance | 通过；沿用 Controller -> Service -> DTO，不新增依赖、不写数据库 |
| Official docs verified | 不适用；未使用新的外部 SDK/API，仅使用项目内 Spring Boot/JUnit/AssertJ 模式 |
| Working OSS referenced | 不适用；指标口径由项目 TODO 和本次 SPEC 定义，不复制外部代码 |
| Root cause identified | 通过；P2-3 缺口是人工评分样本字段、错因指标和分组分析缺失 |

Confidence: 0.95

## 实施步骤

1. 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / worker RUN 文档。
2. 编写 `GradingEvaluationServiceTest` 新断言，覆盖总体指标和三类分组。
3. 编写 `AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport` 新断言，覆盖 `samples` 请求与 `groupedAnalysis` 响应。
4. 运行定向测试，确认 RED。
5. 扩展 `GradingEvaluationRequest`、`GradingEvaluationSummary`、`GradingEvaluationService`，并最小改动 controller endpoint。
6. 运行定向测试，确认 GREEN。
7. 写入 Evidence 和 Acceptance。

## 风险控制

- 不修改 `AssessmentService`，避免影响答题主闭环。
- 不修改 `evaluation` 包和 V14 migration。
- 不新增依赖。
- 不更新共享 memory/changelog/todolist，遵守并行 worker 限制。
