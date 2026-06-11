# RUN-20260610 P3-4 SSE production auth strategy - Test Engineer

## 角色

Test Engineer。

## 现有覆盖

- `ChatControllerTest.streamUsesRagQueryServiceAndEmitsStatusTokenAndDoneEvents` 覆盖 test 环境 Chat SSE 成功流。
- `TutorControllerTest.streamDelegatesToRagQueryServiceAndEmitsTutorEvents` 覆盖 test 环境 Tutor SSE 成功流。
- `SecurityFilterChainTest` 已覆盖 production 普通 API 的 no Bearer / invalid Bearer / valid Bearer / subject-name role-confusion。

## 主要缺口

缺少 production-like SSE endpoint 专项回归：需要证明 Chat/Tutor SSE 在 production/staging 下没有 Bearer 时不会启动 async，不会调用 `RagQueryService`，并返回统一 `UNAUTHORIZED`。

## 推荐测试矩阵

新增 `SseProductionAuthStrategyTest`：

- `chatStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork`
- `chatStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork`
- `chatStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader`
- `chatStreamInProductionDoesNotInferAdminFromBearerSubjectName`
- `tutorStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork`
- `chatStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork`

## 推荐验证命令

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,SecurityFilterChainTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test
```

Full:

```powershell
cd D:\多元agent\backend
mvn test
```

