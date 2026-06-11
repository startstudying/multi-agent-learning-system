# TASK - Orchestrator Workflow 查询与状态上下文收敛

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260605-orchestrator-workflow-query.md`
- SPEC：`docs/specs/SPEC-20260605-orchestrator-workflow-query.md`
- 任务编号：TASK-20260605-orchestrator-workflow-query

## 2. 目标

为 Orchestrator Workflow 增加可查询状态上下文，并用控制器测试验证 create 后 get 和 missing workflow。

## 3. 范围

### 纳入范围

- 扩展 create response。
- 新增 GET 查询接口。
- 组装 trace steps、recentFailedStep、traceSummary、nextActions。
- 统一 `NOT_FOUND` 错误。

### 排除范围

- 不新增数据库表或迁移。
- 不实现完整状态机、取消、重试执行。
- 不改前端。
- 不改其他业务模块。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowResponse.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowStepResponse.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowTraceSummary.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java`（仅允许新增按 owner 与 inputJson marker 查询的方法）
- 本任务相关 docs：PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT、memory、changelog、api contract

## 5. 禁止修改的文件

- `frontend/**`
- 数据库迁移脚本
- 非 Orchestrator 的业务 Service/Controller
- build 配置与依赖文件

## 6. 实施步骤

1. 更新 `OrchestratorWorkflowControllerTest`，先写失败断言。
2. 运行目标测试，确认因 GET endpoint/响应字段缺失失败。
3. 扩展 DTO 与 Service。
4. 新增 Controller GET 映射。
5. 运行目标测试并修复失败。
6. 更新 Evidence、Acceptance、Changelog、Memory。

## 7. 测试命令

```bash
cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test

Completion update:

- Status: DONE
- Completed at: 2026-06-05
- TDD: RED confirmed, then GREEN.
- Test command: `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test`
- Test result: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0.
- Scope: Only Context Pack allowed backend files and related workflow docs were modified.
```

## 8. 完成标准

- [ ] 测试先失败后通过
- [ ] create response 有 `steps`、`traceSummary`、`recentFailedStep`、`nextActions`
- [ ] GET 查询返回同一 workflow 状态上下文
- [ ] missing workflow 返回 404 + `NOT_FOUND`
- [ ] Controller 只做 HTTP 委托
- [ ] Evidence 与 Acceptance 已创建

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 进行中 |
| 负责人 | 后端 worker |
| 开始日期 | 2026-06-05 |
| 完成日期 |  |
