# Evaluation Set 管理证据

## RED

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,SchemaConvergenceMigrationTest" test
```

结果：失败，原因是 `com.learningos.evaluation` 的 DTO、Service、Repository 尚未实现，符合 RED 预期。

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest#teacherCannotBindEvaluationSetToForeignCourse" test
```

结果：失败，原因是教师绑定外部 `courseId` 未抛出异常，确认课程越权边界缺失。

## GREEN

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest#teacherCannotBindEvaluationSetToForeignCourse" test
```

结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,SchemaConvergenceMigrationTest" test
```

结果：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,PromptVersionServiceTest,PromptVersionControllerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport,SchemaConvergenceMigrationTest" test
```

结果：`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`。

## 说明

- 未运行完整 `mvn test`，本轮只跑 evaluation set、prompt version、RAG evaluation、grading evaluation 和 migration 相关回归。
- 测试输出包含 Mockito 动态 agent 的 JDK 未来兼容 warning，非本轮新增问题。
