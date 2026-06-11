# Analytics 学习分析扩展任务

## TASK-1：工作流文档

状态：完成。

产物：PRD、REQ、SPEC、PLAN、TASK、CONTEXT。

## TASK-2：TDD RED

状态：完成。

Done Criteria：

- `AnalyticsControllerTest` 覆盖 overview 旧字段仍存在。
- `AnalyticsControllerTest` 覆盖新增 student summary endpoint。
- 已运行目标测试并看到因接口或字段缺失导致失败。

## TASK-3：实现 analytics 聚合

状态：完成。

允许文件：

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`

Done Criteria：

- `GET /api/analytics/overview` 保持兼容并新增 `agentSummary`、`tokenUsage.byUser`、`tokenUsage.byAgentName`。
- `GET /api/analytics/students/{learnerId}/summary` 返回确定性学习汇总。
- 跨 `learnerId` 查询返回 forbidden。

## TASK-4：测试、证据、验收、记忆

状态：完成。

命令：

```bash
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

Done Criteria：

- 目标测试通过。
- 已更新 evidence、acceptance、changelog、memory。
