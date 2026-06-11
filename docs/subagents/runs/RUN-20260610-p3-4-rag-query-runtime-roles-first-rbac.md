# RUN-20260610 P3-4 子任务：RAG query runtime roles-first RBAC 专家并行审查

## Scope

本次专家并行审查覆盖 P3-4 子任务 `RAG query runtime roles-first RBAC`：

- `/api/rag/query` POST / GET。
- Chat / Tutor RAG runtime。
- Orchestrator `RAG_QA` replay precheck 与执行路径。
- `RagQueryService` role-aware overload 与 legacy overload 安全默认值。

不覆盖：

- KB-course binding schema / lifecycle governance。
- SSE production auth transport strategy。
- 新依赖或 DB schema。

## Parallelism

| Item | Decision |
|---|---|
| Parallelism level | L1 Parallel Analysis / Review |
| Implementation mode | Main Codex single implementation |
| Reason | RAG runtime + security + tests 跨模块，但目标文件耦合，不适合多 agent 并行改同一批代码 |

## Expert Outputs

| Agent | Role | Status | Verdict | Key Output |
|---|---|---|---|---|
| `019ead7b-998f-7692-8e70-3470d15ee7d7` | Agent/RAG Architect | Completed | CONDITIONAL PASS | 建议将本轮限定为 M 级 `RAG query runtime roles-first RBAC`；KB-course binding schema/lifecycle governance 拆为后续 L 级任务。 |
| `019ead7b-9df7-75e1-883e-e494a7b8ac9e` | Test Expert | Completed | CONDITIONAL PASS | 同意优先覆盖 runtime RBAC；要求补 Orchestrator `RAG_QA` 回归。 |
| `019ead88-62cd-7e91-9662-8029cd3a7121` | Test Engineer | Completed | PASS after requested coverage | 建议新增两个 Orchestrator `RAG_QA` 测试：Bearer `ADMIN` + spoofed header 成功、Bearer `USER sub=admin` 不提权且无 query/citation side effects。 |
| `019ead88-3a46-7351-8016-06afdffec34b` | Security Reviewer | Completed | PASS | 未发现本轮范围内 runtime 入口漏传 explicit role facts；未发现 `userId == "admin"` 可 spoof 提权路径；建议后续在 CI/网络稳定环境补 dependency-check。 |

## Integration Decision

接受专家结论并按如下方式整合：

- 本轮完成 M 级 runtime roles-first 授权事实传递。
- 主线补 Orchestrator `RAG_QA` Bearer admin / subject-name role-confusion 回归测试。
- 不升级为 KB-course binding schema / lifecycle governance；该项保持 P3-4 后续 L 级任务。
- 不新增依赖，不做 DB/API/frontend 改动。

## Main Verification

主线已独立运行验证，不仅依赖 subagent 汇报：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest test
mvn test
```

结果：

- Orchestrator focused: `30 run, 0 failures, 0 errors`.
- Runtime RBAC focused: `60 run, 0 failures, 0 errors`.
- Adjacent permission/API: `161 run, 0 failures, 0 errors`.
- Full backend: `509 run, 0 failures, 0 errors, 1 skipped`.

## Remaining Follow-up

- P3-4 KB-course binding schema / lifecycle governance。
- Broader class/course full matrix follow-up。
- SSE production authentication transport strategy。
- Dev/test legacy fallback cleanup。
- Optional dependency-check rerun in stable CI/network environment.
