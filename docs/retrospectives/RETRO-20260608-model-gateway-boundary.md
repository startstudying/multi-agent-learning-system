# Retrospective - P3-3-A 模型网关结构化校验与日志补齐

## 1. Feature Summary

完成模型网关最小边界硬化：资源生成请求绑定 `agent-resource-v1`，`AiModelGateway` 校验 resource structured output，失败证据只写安全错误码，成功模型日志与 token/cost 使用 gateway response。

## 2. What Went Well

- 先通过专家报告把 P3-3 拆成 P3-3-A，避免把真实 provider、schema migration、Embedding/VectorDB 混在同一切片。
- TDD 覆盖了缺 `resources`、资源 item 缺字段、非法 `safetyStatus`、provider raw error、成功 model/latency/token/cost 事实源。
- Code review 后进一步删除了未使用的 raw cause 构造器，降低未来误用风险。
- 完整后端测试通过，说明资源生成、Agent Trace、Orchestrator、Analytics 相邻链路未回退。

## 3. What Didn't Go Well

- 子代理审查在收尾阶段因线程代理数量达到上限未能启动，只能用人工核对和测试证据替代。
- `model_call_log` 没有 provider 字段，P3-3 的“provider 全量落日志”无法在本切片内完成，必须单独 schema 切片处理。
- 现有 resource draft 仍是 deterministic 业务草稿，尚未消费真实模型 structured output。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 模型网关边界：promptVersion 驱动 schema 校验、安全错误码、成功日志事实源来自 gateway response | Yes | `docs/skills/project-specific/model-gateway-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | P3-3 容易被理解为一次性完成真实 provider + schema + embedding | 继续按 P3-3-A/B 拆分，schema migration 和 provider dependency 单独过审 |
| Testing | 初始覆盖缺 `resources` 和缺字段 | 对枚举字段也加显式非法值测试，防止只校验字段存在 |
| Documentation | provider 字段限制容易被误读为已完成 | Evidence / Acceptance / TODO 中明确 provider DB 持久化未完成 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计 P3-3-B 真实 Chat provider 接入，含 dependency review 与 provider 配置红线 | 后续开发者 | 新建 PRD/REQ/SPEC/PLAN/TASK |
| 设计 `model_call_log.provider` schema 切片或替代日志结构 | 后续开发者 | 新建 schema 切片 |
| 为真实 resource structured output 消费设计资源级 citation schema | 后续开发者 | P1-4/P3 后续切片 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md（本切片无基线规则变更）
