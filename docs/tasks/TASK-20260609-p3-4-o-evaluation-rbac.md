# TASK - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 追踪

- PLAN: `docs/plans/PLAN-20260609-p3-4-o-evaluation-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-o-evaluation-rbac.md`
- 任务编号: TASK-20260609-p3-4-o

## 2. 目标

将 Evaluation Set / Run HTTP 管理主路径迁移到 roles-first RBAC，并补齐 Bearer spoof / role-confusion / IDOR oracle 回归测试。

## 3. 范围

### 纳入范围

- Evaluation Set Controller/Service role facts 传递和授权。
- Evaluation Run Controller/Service role facts 传递和授权。
- Controller 和 Service 测试迁移。
- Evidence / Acceptance / Memory / Changelog / Retro。

### 排除范围

- Formal OAuth2/JWK/Spring Security。
- RAG KB management RBAC。
- GradingEvaluation 其他 legacy caller。
- DB schema、frontend、dependency 变更。

## 4. 允许修改的文件

- `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationSetServiceTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java`
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java`
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`
- `docs/evidence/EVIDENCE-20260609-p3-4-o-evaluation-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-o-evaluation-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-o-evaluation-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- 本任务对应 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 文档

## 5. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- 非 Evaluation/RBAC 相关后端模块

## 6. 实施步骤

1. 新增 Controller RED 测试并运行 focused test，确认失败点命中 legacy subject 推断。
2. 修改 Evaluation Set/Run Controller，从 `UserContext.roles()` 计算 `ADMIN/TEACHER`。
3. 修改 Evaluation Set/Run Service，方法签名接收 role facts，授权 helper 不再从 userId 推断角色。
4. 迁移 Service tests 到 roles-first 签名，并补充必要角色矩阵。
5. 运行 focused/adjacent/full verification。
6. 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO。

## 7. 测试命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```

## 8. 完成标准

- [x] RED 测试已先失败，失败原因命中权限缺口。
- [x] 代码已实现 roles-first 授权。
- [x] focused/adjacent/full backend tests 已运行。
- [x] 无架构漂移。
- [x] 仅修改 Context Pack 允许的文件。
- [x] Evidence 文档已创建。
- [x] Acceptance 报告已创建。
- [x] Changelog / Memory / TODO 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 完成 |
| 负责人 | Codex |
| 开始日期 | 2026-06-09 |
| 完成日期 | 2026-06-09 |

## 10. 验证摘要

- RED：`mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test` 首次运行 `15 run, 9 failures`。
- GREEN：
  - `EvaluationSetControllerTest,EvaluationRunControllerTest`：`15 run, 0 failures, 0 errors`。
  - `EvaluationSetServiceTest,EvaluationRunServiceTest`：`19 run, 0 failures, 0 errors`。
  - auth-adjacent：`48 run, 0 failures, 0 errors`。
  - cross-RBAC adjacent：`73 run, 0 failures, 0 errors`。
  - full backend：`419 run, 0 failures, 0 errors, 1 skipped`。
