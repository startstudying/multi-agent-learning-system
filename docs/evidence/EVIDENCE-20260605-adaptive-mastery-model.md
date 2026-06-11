# 自适应掌握度模型证据

## 参考项目学习结论

- `adaptive-knowledge-graph` 类项目强调知识图谱、掌握度建模、BKT/IRT 与实时测评生成的闭环。
- OpenTutor / DeepTutor 类项目强调课程资料、学习会话、测评和学习状态共享，而不是孤立聊天。
- 本轮不引入新依赖或大规模图数据库，先把当前 assessment loop 中的 learner state 用起来。

## TDD RED

命令：

```bash
cd backend
mvn -Dtest=AssessmentControllerTest test
```

结果：失败，符合预期。

关键失败：

```text
JSON path "$.data.masteryUpdates[0].beforeMastery" expected:<0.86> but was:<0.42>
```

说明旧实现没有读取已有 `mastery_record`，仍返回固定演示值。

## GREEN 验证

命令：

```bash
cd backend
mvn "-Dtest=AssessmentFeedbackServiceTest,AssessmentControllerTest" test
```

结果：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 聚焦集成回归

命令：

```bash
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,ChatControllerTest,PromptVersionControllerTest,PromptVersionServiceTest,AnalyticsControllerTest,AssessmentControllerTest,AssessmentFeedbackServiceTest" test
```

结果：

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 全量后端验证

命令：

```bash
cd backend
mvn test
```

结果：

```text
Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 覆盖范围

- 有历史 `mastery_record` 时，`beforeMastery` 使用历史最新值。
- `TRANSFER_WEAKNESS` 动态计算为 `0.86 -> 0.78`，并触发路径重规划。
- 新的 mastery 被写入 `mastery_record`。
- 路径节点 mastery 被同步更新。
- 无历史 `mastery_record` 时，默认路径断言 `0.42 -> 0.58`，并验证持久化 mastery 与错题知识点。
- 服务级测试继续覆盖错因诊断和掌握度范围不越界。

## 代码审查处理

- 已采纳：补强无历史 mastery 场景的 API 断言和持久化断言。
- 暂不采纳：低到高跨越 `0.80` 的路径重规划测试。当前 BKT-lite 错因路径最高更新到 `0.78`，该测试更适合后续引入正确答案/满分 grading 后补充。

## 备注

测试输出包含 Mockito dynamic agent 的 JDK 未来兼容 warning，不影响本轮测试结果。
