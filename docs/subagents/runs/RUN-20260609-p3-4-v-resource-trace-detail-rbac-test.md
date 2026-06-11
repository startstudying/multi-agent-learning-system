# RUN - P3-4-V Test Engineer

## Scope

只读测试覆盖分析与 RED 测试矩阵设计。

## Key Coverage Gap

Existing tests cover legacy `X-User-Id` owner/foreign/missing behavior, but not Bearer roles-first behavior for:

- ResourceGeneration task detail
- learner-resources missing semantics
- Agent Trace detail
- Agent Trace governance search

## RED Matrix

### `ResourceGenerationControllerTest`

1. `resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
   - Create `alice` task.
   - Request with Bearer `ADMIN sub=ops_admin` and spoofed `X-User-Id=bob`.
   - Expected `200 OK`.
   - Current behavior: `403`, because `ops_admin != "admin"`.

2. `resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion`
   - Create `alice` task.
   - Request with Bearer `USER sub=admin`.
   - Expected `403 FORBIDDEN`, no `data`, body does not contain `taskId`.
   - Current behavior: `200`, because `sub == "admin"` is treated as admin.

3. `resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound`
   - Request missing task with Bearer `ADMIN sub=ops_admin`.
   - Expected `404 NOT_FOUND`.
   - Current behavior: `403`.

4. `learnerResourcesRejectsBearerUserSubjectAdminMissingTaskAsForbidden`
   - Request missing learner-resources with Bearer `USER sub=admin`.
   - Expected `403 FORBIDDEN`.
   - Current behavior: `404`, because missing branch treats `sub=admin` as admin.

### `AgentTraceControllerTest`

1. `traceGovernanceSearchUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
2. `traceGovernanceSearchRejectsBearerUserSubjectAdminRoleConfusion`
3. `traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
4. `traceDetailRejectsBearerUserSubjectAdminRoleConfusion`
5. `traceDetailBearerAdminMissingTaskReturnsNotFound`

## Command Recommendation

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest#resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion+resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound+learnerResourcesRejectsBearerUserSubjectAdminMissingTaskAsForbidden,AgentTraceControllerTest#traceGovernanceSearchUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceGovernanceSearchRejectsBearerUserSubjectAdminRoleConfusion+traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceDetailRejectsBearerUserSubjectAdminRoleConfusion+traceDetailBearerAdminMissingTaskReturnsNotFound test
```

## Status

Read-only report. No files were modified by the subagent.
