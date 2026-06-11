# PLAN-20260609 P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. 状态

Done

## 2. 实施阶段

### Phase 1: Spec-first 文档

- 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- 创建多专家集成评审报告。
- 明确 PromptVersion 为 P3-4-N，Evaluation/GradingEvaluation 后置。

### Phase 2: RED 测试

- 修改 `PromptVersionControllerTest`。
- 增加 Bearer JWT helper 与 roles-first 矩阵。
- 运行 focused RED。

### Phase 3: 最小实现

- `PromptVersionController` 注入 `CurrentUserService`。
- Controller 从 `UserContext.roles()` 计算 admin/teacher facts。
- `PromptVersionService` 增加 role-aware upsert/list/get。
- `PromptVersionResponse` 支持 `promptText` 脱敏序列化。
- 更新 `PromptVersionServiceTest` 适配 service 签名。

### Phase 4: 验证与收尾

- 运行 focused / adjacent / full Maven tests。
- 创建 evidence / acceptance / retrospective。
- 更新 changelog / memory / backend TODO。

## 3. 允许修改文件

生产代码：

- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java`

测试：

- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/PromptVersionServiceTest.java`

文档：

- `docs/product/PRD-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/subagents/runs/RUN-20260609-p3-4-n-next-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-n-integration-review.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. 禁止修改

- `backend/pom.xml`
- DB migration / schema files
- frontend files
- RAG / Evaluation / Assessment / Analytics / Learning modules
- `docs/superpowers/**`

## 5. 风险与缓解

| Risk | Mitigation |
|---|---|
| 误用 `CurrentUserService.isAdmin()` 导致 Bearer `sub=admin` 提权 | Controller 仅从 `UserContext.roles()` 计算 role facts |
| Teacher 响应中仍序列化 `promptText: null` | `PromptVersionResponse` 使用 non-null serialization，并新增 `doesNotExist` 测试 |
| Service 内部 active prompt 查询被鉴权阻断 | 保留 `findActiveByCode` 内部读取能力 |
| 旧 controller 测试无身份创建失败 | 更新测试为 admin Bearer 或 dev/test `X-User-Id=admin` |

## 6. Rollback Strategy

- 若 focused RED/GREEN 后 adjacent 大面积回归，回退到只改 Controller admin-only 写入，不做读脱敏前需更新 SPEC。
- 若 DTO 序列化影响其他响应，改为新增 summary DTO，但仍不改 API path/schema。

## 7. Test Strategy

```powershell
cd backend
mvn --% -Dtest=PromptVersionControllerTest test
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

## 8. Architecture Drift

实施前后检查：无架构漂移。

- Controller 只处理 HTTP/current user extraction。
- Service 负责权限与业务逻辑。
- 不新增依赖。
- 不改 DB schema。
- 不改 frontend。

## 9. Verification Results

```text
Focused:  mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest test
Result:   14 run, 0 failures, 0 errors, 0 skipped

Adjacent: mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
Result:   48 run, 0 failures, 0 errors, 0 skipped

Full:     mvn test
Result:   410 run, 0 failures, 0 errors, 1 skipped
```
