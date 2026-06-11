# RUN-20260608 backend TODO 下一切片集成评审

## Skill Selection Report

### Task Type

P3-4 权限与安全生产化切片；涉及后端权限、数据库 schema、课程/学习路径/资源/分析模块。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目要求所有功能/修复走 Spec-first 全流程。 |
| object-scope-authorization | 本切片核心是对象级授权、IDOR 防护和 course/enrollment scope。 |
| java-security-review | 涉及权限、安全、敏感学习数据访问边界。 |
| spring-ai-agent-backend | 需要保持后端分层和 Agent/Resource 服务边界，不让权限散落到工具或前端。 |
| assessment-feedback-agent | 本轮暂不实现 answer record 读矩阵，但需要记录 assessment 领域后续边界。 |
| test-driven-development | 实现前必须先写权限回归测试。 |
| verification-before-completion | 完成声明前必须提供新鲜测试证据。 |

### Missing Skills

无。本切片可用现有项目技能覆盖。

### GitHub Research Needed

No。本切片不新增依赖、不引入第三方 SDK，按项目现有 JPA/Spring Boot 模式实现。

### New Project-Specific Skill To Create

完成后视沉淀价值扩展 `object-scope-authorization.md`，记录 enrollment scope 模式。

## Subagent Decision

Use Subagents: Yes

Reason: 用户明确要求专家 subagent 并行开发；任务横跨 RAG、模型接入、权限安全三个 P3 未完成方向。

Parallelism Level: L1 Parallel Analysis

Selected Subagents:

- RAG 生产化专家：P3-2 parser/OCR/page/section 缺口分析。
- 模型接入专家：P3-3 Spring AI Chat/Embedding provider 缺口分析。
- 权限矩阵/安全专家：P3-4 class/course/answer RBAC 缺口分析。

Implementation Mode: Single Codex implementation after integration review.

## 集成判断

优先级排序：

1. P3-4 权限安全：生产安全边界优先，且可无新增依赖推进。
2. P3-2 parser 能力分级：影响 RAG citation 可信度，但真实 OCR/复杂 parser 需 dependency review。
3. P3-3 provider skeleton：真实 SDK 需要官方文档与 dependency review，直接接入风险较高。

## 选定下一切片

`P3-4-D course enrollment scope`

## 冲突与取舍

- 安全专家建议同时补 answer record RBAC；集成后拆为后续切片，原因是 answer record 读 API 需要新增 API contract、DTO 策略和 question/course 关联策略，和 enrollment 域同切片会过大。
- 本切片不引入 JWT/RBAC 依赖，不改变 dev header 身份来源，只强化对象级授权。
- 本切片不修改 frontend。
