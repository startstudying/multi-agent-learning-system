# PLAN - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-o-evaluation-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-o-evaluation-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-o-evaluation-rbac.md`

## 2. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 新增 Controller RED 测试 | TASK-01 | 完成 |
| 2 | Controller 传递 role facts | TASK-01 | 完成 |
| 3 | Service 改为 roles-first 授权 | TASK-01 | 完成 |
| 4 | 运行 focused/adjacent/full 验证 | TASK-01 | 完成 |
| 5 | 补 Evidence/Acceptance/Memory/Changelog/Retro | TASK-01 | 完成 |

## 3. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java` | 修改 | 1 | Codex |
| `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java` | 修改 | 1 | Codex |
| `backend/src/test/java/com/learningos/evaluation/application/EvaluationSetServiceTest.java` | 修改 | 3 | Codex |
| `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java` | 修改 | 3 | Codex |
| `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java` | 修改 | 2 | Codex |
| `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java` | 修改 | 2 | Codex |
| `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java` | 修改 | 3 | Codex |
| `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java` | 修改 | 3 | Codex |
| `docs/evidence/EVIDENCE-20260609-p3-4-o-evaluation-rbac.md` | 新增 | 5 | Codex |
| `docs/acceptance/ACCEPT-20260609-p3-4-o-evaluation-rbac.md` | 新增 | 5 | Codex |
| `docs/retrospectives/RETRO-20260609-p3-4-o-evaluation-rbac.md` | 新增 | 5 | Codex |
| `docs/changelog/CHANGELOG.md` | 更新 | 5 | Codex |
| `docs/memory/PROJECT_MEMORY.md` | 更新 | 5 | Codex |
| `docs/memory/BACKEND_MEMORY.md` | 更新 | 5 | Codex |
| `docs/memory/AGENT_RAG_MEMORY.md` | 更新 | 5 | Codex |
| `docs/planning/backend-architecture-todolist.md` | 更新 | 5 | Codex |

## 4. 依赖

- 前置条件：P3-4-I auth context 已完成。
- 新增依赖：无。

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| Controller 使用 `CurrentUserService.isAdmin()` 误带入 dev/test legacy inference | Bearer role-confusion 测试失效 | Controller 只从 `UserContext.roles()` 计算 role facts |
| 移除 legacy service 签名导致测试编译失败 | 验证中断 | 同步迁移 service tests 到 roles-first 签名 |
| missing vs foreign 行为变化影响旧测试 | 旧测试失败 | 更新 SPEC，并保留 admin missing `NOT_FOUND` |
| 权限收口影响 response redaction | 敏感数据泄露 | 保留现有 comparison response negative assertions |

## 6. 回滚策略

- 回滚本切片代码文件到上一版本。
- 保留文档记录为未完成。
- 不涉及 DB migration，无数据回滚。

## 7. 测试策略

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
mvn --% -Dtest=EvaluationSetServiceTest,EvaluationRunServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```

验证结果：

- RED：`EvaluationSetControllerTest,EvaluationRunControllerTest` 首次运行 `15 run, 9 failures`，命中 legacy subject inference / IDOR oracle 缺口。
- Controller focused：`15 run, 0 failures, 0 errors`。
- Service focused：`19 run, 0 failures, 0 errors`。
- Auth-adjacent：`48 run, 0 failures, 0 errors`。
- Cross-RBAC adjacent：`73 run, 0 failures, 0 errors`。
- Full backend：`419 run, 0 failures, 0 errors, 1 skipped`。

## 8. Subagent 计划

| 专家 | 是否需要 | 职责 |
|---|---|---|
| 产品分析师 | 否 | 需求明确 |
| 规格架构师 / 后端专家 | 是 | roles-first 服务边界 |
| 前端专家 | 否 | 不改前端 |
| Agent/RAG 专家 | 否 | 不改 Agent/RAG runtime |
| 安全与质量 | 是 | RBAC/IDOR 测试矩阵 |
| 测试工程师 | 是 | RED/GREEN 验证策略 |
| 集成评审员 | 是 | Main Codex 合并专家意见 |

并行级别：L1 分析。

## 9. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved |

## 10. 完成记录

- 状态：完成。
- Evidence：`docs/evidence/EVIDENCE-20260609-p3-4-o-evaluation-rbac.md`
- Acceptance：`docs/acceptance/ACCEPT-20260609-p3-4-o-evaluation-rbac.md`
- Retrospective：`docs/retrospectives/RETRO-20260609-p3-4-o-evaluation-rbac.md`
- 架构漂移：未发现；无 API / DB / dependency / frontend drift。
