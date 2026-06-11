# TASK: Prompt Version 管理基础

## 当前任务

实现 TODO P2-1 中 Prompt Version 的最小后端管理基础。

## 验收项

- [x] `PromptVersion` JPA entity 映射现有 `prompt_version` 表。
- [x] `PromptVersionRepository` 支持 `code/version`、`code`、active 查询。
- [x] `PromptVersionService` 支持 upsert、查询、列表、active 查找。
- [x] `PromptVersionController` 暴露创建/更新、查询和列表 API。
- [x] TDD 红灯记录完成。
- [x] 指定 Maven 测试通过。

## 测试命令

```powershell
cd backend
mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test
```

## 限制

- 不新增 migration。
- 不新增依赖。
- 不修改 `AgentRunRecorder`。
- 不实现 evaluation set 表和 API。
