# RUN-20260607 后端架构 TODO 完成计划专家并行报告

## 任务背景

目标文件：`docs/planning/backend-architecture-todolist.md`

用户要求：完成后端架构优化计划，并使用专家 subagent 并行开发。

## Subagent Decision

Use Subagents: Yes

Reason:

- 目标覆盖 RAG、模型接入、权限安全、可观测性多个后端域。
- 涉及 Agent/RAG、数据库、权限、安全与测试，按项目规则必须启用 Agent/RAG Expert 与 Security & Quality。
- 本轮先采用 L1/L2：并行分析与设计；实现由主 Codex 按 Context Pack 单任务串行集成，避免文件冲突。

Parallelism Level: L1 + L2

Selected Subagents:

- Backend Expert：模型接入边界与 `AiModelGateway` 分析。
- Security & Quality Expert：P3-4 权限与 RBAC 缺口分析。
- Integration Architect：剩余 P3 任务集成排序。因上游模型通道 429 失败，本轮由主 Codex 根据计划文件、记忆与已完成专家报告完成集成。
- Agent/RAG Expert：早期 explorer 因模型通道 503 失败，本轮由主 Codex 基于代码检索完成最小 RAG 状态归纳，后续切片可继续重派。

Implementation Mode: Single Codex after parallel expert analysis.

## Skill Selection Report

## Task Type

后端架构生产化 / 权限安全加固 / RAG 与模型接入规划 / 可观测性告警规划。

## Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制从原始需求进入 Project Memory、Skill Selection、Subagent、PRD/REQ/SPEC/PLAN/TASK/Context、实现、测试、证据、验收闭环。 |
| multi-agent-coder | 用户明确要求使用专家 subagent 并行开发。 |
| subagent-driven-development | 后续按切片执行，每个切片实施后需要审查与验证。 |
| spring-ai-agent-backend | P3-3 涉及 Spring AI、模型网关、结构化输出、模型调用日志。 |
| educational-rag-pipeline | P3-2 涉及 RAG parser、chunk、embedding、retrieval、reranker。 |
| java-security-review | P3-4 涉及越权、IDOR、RBAC、敏感数据泄露与安全测试。 |
| object-scope-authorization | 项目内已有对象级授权技能，用于课程、资源、答题、RAG 对象范围收口。 |
| architecture-drift-check | 每个切片必须检查架构漂移。 |
| test-driven-development | 实现前先写失败测试，再写最小代码。 |
| verification-before-completion | 完成声明前必须有实际测试证据。 |

## Missing Skills

无。当前切片不需要新增依赖或 GitHub 参考。

## GitHub Research Needed

No。P3-4 第一切片只收口现有接口权限，不引入新依赖；P3-3 真实 Spring AI provider 接入时再进入依赖审查和官方文档确认。

## New Project-Specific Skill To Create

暂不创建。已有 `object-scope-authorization` 可覆盖本轮 P3-4。

## 专家并行结果摘要

### Backend Expert：P3-3 模型接入边界

结论：

- 当前 `ResourceGenerationService -> AiModelGateway` 已有模型网关雏形，但 `AiModelGateway` 仍是 deterministic placeholder。
- RAG 查询当前不调用 Chat 模型，`RagQueryService` 只基于 chunk 构造确定性回答。
- `EmbeddingService` 只有 `noop-embedding-v1` 版本号，没有真实 embedding API。
- `model_call_log` 已记录模型名、token、latency、prompt metadata，但缺少 provider 字段，成功日志也未以 gateway 返回的真实 usage 为事实源。

建议：

1. P3-3 先做模型边界与结构化输出校验，再接具体 provider starter。
2. 新依赖必须单独走 `docs/security/` dependency review。
3. `application-test.yml` 保持 `provider=none`，无 key 测试必须可运行。

### Security & Quality Expert：P3-4 权限安全

结论：

- RAG KB、Learning Path owner、Resource owner、Review Gate course scope 已有最小覆盖。
- 最高风险缺口集中在：
  1. Course / Knowledge Catalog 写操作无权限控制。
  2. 身份/角色仍是 dev header + userId 字符串约定。
  3. 部分对象详情接口存在 404/403 枚举差异。
  4. 答题记录读取矩阵尚未形成。

建议第一切片：

- 建立统一 `CurrentUserService` 角色判断基础。
- 收口 Course / Knowledge Catalog 写权限：
  - student 不可创建课程、章节、知识点、依赖。
  - teacher 创建课程时 `teacherId` 必须等于 current user，或由后端覆盖。
  - teacher 只能维护自己的 course graph。
  - admin 全局可写。

## 剩余 TODO 分类

### P3-2 RAG 索引生产化

- [ ] 复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别
- [ ] embedding service 和可选 VectorDB adapter
- [ ] hybrid retrieval、RRF、reranker timeout fallback

当前代码状态：

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java` 已有 Markdown/TXT/PDF/DOCX 最小 parser adapter。
- `IndexService` 已有 parser -> token-ish chunk -> stable hash -> metadata 流程。
- `EmbeddingService` 仍为 noop。
- `RerankerService` 仍为 identity fallback。

### P3-3 模型接入边界

- [ ] Spring AI/Spring AI Alibaba 真实 Chat/Embedding provider
- [ ] 所有模型调用通过 `AiModelGateway`
- [ ] 结构化输出 schema 校验和降级
- [ ] provider/model/prompt/latency/token/error 全量日志

### P3-4 权限与安全加固

- [ ] RAG KB、课程、学习路径、资源、答题记录全部补齐生产级权限检查
- [ ] 教师端只能访问授权班级/课程数据
- [ ] 学生端只能访问自己的画像、路径、资源和答题记录
- [ ] 完整权限渗透测试矩阵

### P3-5 可观测性与运维

- [ ] 慢查询、慢模型调用、无引用回答、审核积压告警

## 推荐执行顺序

1. P3-4-A：Course / Knowledge Catalog 写权限收口。
2. P3-4-B：对象级防枚举与 scoped query。
3. P3-5-A：告警规则服务与只读告警 API，不引入外部告警系统。
4. P3-3-A：模型网关结构化输出校验、provider 字段日志、边界架构测试。
5. P3-3-B：Spring AI provider dependency review 与真实 Chat/Embedding 接入。
6. P3-2-A：Embedding service 边界和 no-key fallback。
7. P3-2-B：Hybrid retrieval / RRF / reranker timeout fallback。
8. P3-2-C：复杂 PDF/DOCX/OCR fallback，需要依赖审查或明确无依赖降级策略。

## 第一轮实现切片

选择：P3-4-A Course / Knowledge Catalog 写权限收口。

理由：

- 风险高：当前任意用户可创建 course、chapter、knowledge point、dependency。
- 范围小：主要涉及 `common/auth` 与 `knowledge`。
- 无新增依赖：可用现有 `X-User-Id` dev auth 语义和 `Course.teacherId`。
- 可 TDD：用 controller 集成测试覆盖 student/teacher/admin。

## 架构漂移检查（实施前）

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只取 current user，Service 执行业务与权限。 |
| Frontend rules | N/A | 本切片不改前端。 |
| Agent / RAG rules | N/A | 本切片不改 Agent/RAG 执行。 |
| Security | PASS | 不新增 secret，不新增依赖。 |
| API / Database | PASS | 不新增 API，不改 schema；只收紧现有写接口权限。 |

