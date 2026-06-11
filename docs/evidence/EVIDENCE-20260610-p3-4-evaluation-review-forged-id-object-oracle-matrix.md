# EVIDENCE-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix

## 1. 任务

P3-4 子任务：Evaluation/Review forged-id object-oracle matrix

## 2. 变更摘要

测试-only 补强 Evaluation Run 与 Resource Review 的 forged-id / object-oracle 权限矩阵。

新增测试：

- `EvaluationRunControllerTest.bearerTeacherCannotRecordRunForForeignOrMissingEvaluationSetAndDoesNotPersistRun`
- `ResourceReviewControllerTest.bearerTeacherWithSpoofedAdminHeaderCannotDistinguishMissingReviewFromForeignReviewAndDoesNotMutate`

验证点：

- Bearer `TEACHER` 即使伪造 `X-User-Id: admin`，也不能为 foreign 或 missing `evaluationSetId` 创建 evaluation run。
- foreign / missing evaluation set 对非 admin 收敛为安全 `FORBIDDEN`，无 `data`，且不泄露 forged id、prompt version 或 trace。
- Bearer `TEACHER` 即使伪造 `X-User-Id: admin`，也不能通过 missing review 与 foreign review 的响应差异形成 object oracle。
- foreign / missing review 决策均返回安全 `FORBIDDEN`，无 `data`，且不泄露 review/task/resource/request summary。
- 拒绝请求不新增 `EvaluationRun` / `EvaluationRunMetric`，也不改变 review/resource/task 状态。

## 3. 修改文件

- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationRunControllerTest,ResourceReviewControllerTest test
```

结果：

- `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest test
```

结果：

- `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 578, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

说明：此前一次 full backend 失败由并行 Maven 进程互踩 `backend/target` 引发 `ClassNotFoundException`；停止并行 Maven 后重跑通过，不判定为代码缺陷。

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 测试-only；生产权限仍由既有 Controller/Service 路径执行。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG runtime。 |
| Security | PASS | 补强 forged-id、防枚举 oracle、防副作用回归测试。 |
| API / Database | PASS | 未改 API contract 或 schema。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| foreign/missing evaluation set run 创建拒绝路径已覆盖 | PASS |
| evaluation run 拒绝响应不泄露 forged id / prompt / trace 且无 `data` | PASS |
| evaluation run 拒绝请求无 run/metric 持久化副作用 | PASS |
| foreign/missing review decision object-oracle 收敛已覆盖 | PASS |
| review 拒绝响应不泄露 review/task/resource/request summary 且无 `data` | PASS |
| review 拒绝请求不改变 review/resource/task 状态 | PASS |
| 专家 subagent 复核结论为 PASS | PASS |
| focused / adjacent / full backend 验证完成 | PASS |

最终结论：PASS。P3-4 父项仍保持 open。
