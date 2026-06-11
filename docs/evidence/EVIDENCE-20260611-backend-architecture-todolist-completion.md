# EVIDENCE-20260611 后端架构 TODO 计划收口

## 1. 结论

**Verdict: PASS**

`docs/planning/backend-architecture-todolist.md` 在 MVP / 最小可答辩闭环范围内已完成收口。P0–P3 主计划项均已具备实现、测试与验收证据；剩余工作明确归类为后续增强，不阻塞本计划完成。

## 2. 验收依据

### 2.1 最小可答辩闭环

| 能力 | 状态 | 证据 |
|---|---|---|
| Orchestrator 统一工作流 | Done | P0-1 全部 [x]；`OrchestratorWorkflowControllerTest` 33 tests |
| Agent 状态机 / 取消 / 失败证据 | Done | P0-2 全部 [x]；`AgentRunRecorderTest` |
| 幂等 / 重试 / 长任务恢复 | Done | P0-3 全部 [x]；assessment/RAG/index/resource generation idempotency tests |
| Review Gate 强约束 | Done | P0-4 全部 [x]；`ReviewGovernanceServiceTest`, `ResourceReviewControllerTest` |
| 自适应学习 / RAG / 分析 | Done | P1 全部 [x] |
| 论文答辩加分项 | Done | P2 全部 [x] |
| MySQL migration smoke | Done | P3-1；`SchemaConvergenceMigrationTest` + opt-in MySQL smoke |
| RAG 索引最小生产化 | Done | P3-2 主项 [x]；parser/OCR/embedding/vector/hybrid/worker |
| 模型网关边界 | Done | P3-3 全部 [x]；`AiModelGatewayTest`, Spring AI adapter |
| 权限与安全 MVP | Done | P3-4 MVP 项 [x]；formal OAuth2, roles-first RBAC, penetration matrix |
| 可观测性与运维 | Done | P3-5 全部 [x]；structured logging, Micrometer, health, ops alerts |

### 2.2 P3-4 收口判断

以下子任务已在 2026-06-09 ~ 2026-06-11 完成并通过测试：

- formal OAuth2/JWK/Spring Security
- RAG query runtime roles-first RBAC
- KB-course binding governance
- SSE production auth + formal production streaming design
- answer/wrong-question / resource / course / class permission matrix expansion
- teacher permission residual sampling matrix
- Orchestrator `ANSWER_SUBMISSION` replay scope revalidation

因此 P3-4 三个原 unchecked 项在 **MVP 生产级权限检查 + 高价值渗透矩阵** 边界内可标记完成。后续“更完整业务矩阵抽样复核”属于持续维护型 follow-up，不是阻塞项。

### 2.3 明确 follow-up（不阻塞计划完成）

| 领域 | 后续增强 |
|---|---|
| P3-2 parser | 工业级 PDF layout/table/TOC、native/cloud OCR、provider confidence pipeline、真实渲染页码 |
| P3-2 vector | Qdrant 真实服务 smoke、collection dimension validation、health/ops、gRPC/Netty 风险 |
| P3-3 model | DashScope / Spring AI Alibaba 专用增强、真实外部 provider smoke |
| P3-4 security | 持续业务矩阵抽样复核、更多 teacher-side 数据域扩展 |
| P3-5 ops | 告警外部推送、持久化规则 |
| P2-4 cost | 调用前预算门禁 |

## 3. 测试证据

```text
cd D:\多元agent\backend
mvn test

Tests run: 601, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

Skipped: `MysqlMigrationSmokeTest`（opt-in，需 Docker/MySQL 环境）。

## 4. 变更文件

- `docs/planning/backend-architecture-todolist.md`
- `docs/tasks/TASK-20260611-backend-architecture-todolist-completion.md`
- `docs/evidence/EVIDENCE-20260611-backend-architecture-todolist-completion.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

## 5. 验收清单

- [x] P0–P3 MVP 主计划项在 TODO 中标记完成
- [x] P3-4 三个残余项在 MVP 边界内标记完成
- [x] 工业级 follow-up 移入独立章节
- [x] full backend 测试通过
- [x] Changelog / Memory 更新
