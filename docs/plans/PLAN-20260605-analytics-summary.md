# Analytics 学习分析扩展计划

## 任务边界

只修改 analytics 后端模块和对应 analytics 测试。其他 domain repository 只读，不修改。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目要求所有功能开发走 PRD/REQ/SPEC/PLAN/TASK/Context Pack |
| test-driven-development | 用户明确要求先写测试并观察失败 |
| ai-learning-agent-development | 项目级 AI 学习系统后端开发约束 |
| spring-ai-agent-backend | Spring Boot Controller/Service 分层和测试模式 |
| agent-trace-governance | 涉及 agent_task、model_call_log、token_usage_log、RAG 命中率 |
| Confidence Check | 实现前确认重复能力、架构和根因 |

缺失技能：无。

GitHub Research Needed：No。现有代码已有 Spring Boot/JPA/MockMvc 模式，本次不引入新依赖。

New Project-Specific Skill：No。

## Subagent Decision

Use Subagents：No。

Reason：用户指定后端 worker 只做 analytics 模块，影响范围集中在一个模块。Agent/RAG 相关字段仅做现有表只读汇总，不改 agent 或 rag 流程。

Parallelism Level：Single Codex。

Implementation Mode：单任务实现，避免和其他 worker 文件冲突。

## 步骤

1. 创建 workflow 文档和 Context Pack。
2. 修改 `AnalyticsControllerTest`，增加 overview 新字段兼容性断言和 student summary 断言。
3. 运行 `cd backend; mvn "-Dtest=AnalyticsControllerTest" test`，确认 RED。
4. 修改 `AnalyticsController` 和 `AnalyticsService` 实现。
5. 再运行目标测试，记录证据和验收。

## 执行结果

状态：完成。

验证命令：

```bash
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

结果：`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。

## 风险

- 当前多个实体缺少针对 analytics 的排序查询方法，因此 service 需要使用 `findAll()` 后内存聚合；MVP 数据量可接受，后续可在对应 repository 增加专用只读查询。
- `KbQueryLog` 无 getter，analytics 只能用 `DirectFieldAccessor` 读取字段；这沿用现有 RAG 测试模式，不改变实体。
