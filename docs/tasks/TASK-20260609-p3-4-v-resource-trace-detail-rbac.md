# TASK - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-v-resource-trace-detail-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md`

## 2. Goal

关闭 ResourceGeneration task detail、learner-resources missing 分支、Agent Trace detail/search 中的 legacy `sub == "admin"` role-confusion。

## 3. Files Allowed To Modify

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/AgentTraceControllerTest.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceController.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceGovernanceController.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-v-resource-trace-detail-rbac-*.md`
- `docs/product/PRD-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-v-resource-trace-detail-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider runtime
- Review Gate completed paths
- CourseAccessService legacy cleanup

## 5. Implementation Steps

1. 在 `ResourceGenerationControllerTest` 和 `AgentTraceControllerTest` 添加 Bearer roles RED tests。
2. 运行 focused RED 并记录失败。
3. Controller 使用 `currentUser()` 派生 `ADMIN` role fact。
4. Service 新增 roles-first overload，HTTP 主路径调用新 overload。
5. 保持 legacy overload 兼容；不开放 admin cancel。
6. 运行 focused、adjacent、full tests。
7. 写 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest#resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion+resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound+learnerResourcesRejectsBearerUserSubjectAdminMissingTaskAsForbidden,AgentTraceControllerTest#traceGovernanceSearchUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceGovernanceSearchRejectsBearerUserSubjectAdminRoleConfusion+traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceDetailRejectsBearerUserSubjectAdminRoleConfusion+traceDetailBearerAdminMissingTaskReturnsNotFound test
mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,ResourceReviewControllerTest,OrchestratorWorkflowControllerTest,AnalyticsControllerTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] ResourceGeneration detail 使用 roles-first admin fact。
- [x] learner-resources missing 语义使用 roles-first admin fact，且仍 owner-only。
- [x] Agent Trace detail/search 使用 roles-first admin fact。
- [x] Bearer `USER sub=admin` role-confusion 被拒。
- [x] Bearer `ADMIN sub=ops_admin` + spoofed header 成功执行 admin detail/search。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done。
