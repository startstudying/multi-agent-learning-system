# RUN-20260609 P3-4 LearningPath create legacy overload cleanup - Test

## 测试设计结论

在 `LearningWorkflowServiceTest` 中新增反射 guard，避免删除 API 后测试无法编译，同时证明 legacy surface 被移除。

## 推荐测试点

- public `createPathForUser(String, CreateLearningPathRequest)` 不再暴露。
- private `isAdmin(String)` 不再存在。
- roles-first overload 仍允许 explicit admin 为其他 learner 创建 path。

## 推荐命令

RED:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest#createPathForUserLegacyOverloadIsRemoved+learningWorkflowServiceSubjectNameAdminHelperIsRemoved test
```

GREEN focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest#createPathForUserLegacyOverloadIsRemoved+learningWorkflowServiceSubjectNameAdminHelperIsRemoved+createPathForUserRolesFirstOverloadAllowsExplicitAdminCrossLearnerCreation test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest,LearningWorkflowControllerTest test
```
