# Evaluation Set 管理开发计划

## 实施阶段

1. 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT 和子任务记录。
2. 写 RED 测试：
   - `EvaluationSetServiceTest`
   - `EvaluationSetControllerTest`
   - `SchemaConvergenceMigrationTest` V13 断言
3. 实现 `com.learningos.evaluation` 模块。
4. 新增 V13 migration。
5. 运行 focused 测试和相关回归。
6. 更新 TODO、证据、验收、memory、changelog、retro。

## 文件变更

- 新增 `backend/src/main/java/com/learningos/evaluation/**`
- 新增 `backend/src/test/java/com/learningos/evaluation/**`
- 修改 `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- 新增 `backend/src/main/resources/db/migration/V13__evaluation_set_management.sql`
- 更新规划和记忆文档。

## 风险

- 样本字段如果开放为任意 JSON，会扩大敏感数据面。
- 权限如果只放 Controller，会出现 Service 绕过。
- 如果本轮引入 run/compare，会扩大范围并拖慢第 142 行落地。

## 回滚策略

- 代码回滚新增 evaluation 包和测试。
- V13 是新增表迁移，不自动 drop；生产回滚需手工处理新增表。

## 验证命令

```powershell
cd backend
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,SchemaConvergenceMigrationTest" test
mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,PromptVersionServiceTest,PromptVersionControllerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,GradingEvaluationServiceTest,SchemaConvergenceMigrationTest" test
```
