# PLAN: Prompt Version 管理基础

## 实施步骤

1. 读取项目记忆、架构基线、P2-1 TODO 和迁移表结构。
2. 编写 `PromptVersionServiceTest` 与 `PromptVersionControllerTest`，先运行并确认失败。
3. 实现 `PromptVersion`、`PromptVersionRepository`、DTO、`PromptVersionService`、`PromptVersionController`。
4. 运行 `mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test`。
5. 生成 Evidence 和 Acceptance，更新 Changelog 与 Memory。

## 文件边界

允许修改：

- `backend/src/main/java/com/learningos/agent/domain/PromptVersion.java`
- `backend/src/main/java/com/learningos/agent/repository/PromptVersionRepository.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`
- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/dto/*PromptVersion*.java`
- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/PromptVersionServiceTest.java`
- 本任务工作流文档、证据、验收、changelog、memory。

不修改：

- `AgentRunRecorder`
- migration
- `model_call_log`
- frontend

## 风险

- 表中没有 `description/hash/is_active` 独立列：按实际迁移列实现，使用 `status` 表达激活状态。
- 用户提到 evaluation set，但责任范围没有相关文件：本轮只做 Prompt Version 基础，evaluation set 作为后续任务。

## 架构漂移前置结果

- 后端分层符合基线。
- 无前端变更。
- 无 Agent/RAG 执行链路变更。
- 无新依赖和数据库变更。
