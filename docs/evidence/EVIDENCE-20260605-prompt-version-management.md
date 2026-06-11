# Evidence: Prompt Version 管理基础

## 1. 追踪

- TASK: `docs/tasks/TASK-20260605-prompt-version-management.md`
- SPEC: `docs/specs/SPEC-20260605-prompt-version-management.md`
- 日期: 2026-06-05

## 2. 实现内容

本次补齐 `prompt_version` 表的后端管理基础：JPA entity、Repository、Service、Controller、DTO，以及服务层和 Controller 层测试。字段严格按 `V2__learning_agent_loop.sql` 中实际列映射，没有新增 migration。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/agent/domain/PromptVersion.java` | 新增 | 映射 `prompt_version` 表，自动生成 `prv_` ID 和 `createdAt` |
| `backend/src/main/java/com/learningos/agent/repository/PromptVersionRepository.java` | 新增 | 支持 `code/version`、`code`、active 查询 |
| `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java` | 新增 | 支持 upsert、get、list、`findActiveByCode` |
| `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java` | 新增 | 暴露 Prompt Version 管理 API |
| `backend/src/main/java/com/learningos/agent/dto/PromptVersionUpsertRequest.java` | 新增 | Upsert 请求 DTO |
| `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java` | 新增 | 响应 DTO |
| `backend/src/test/java/com/learningos/agent/application/PromptVersionServiceTest.java` | 新增 | 覆盖创建、重复 upsert、查询、active 查找 |
| `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java` | 新增 | 覆盖 API 创建、查询、列表、重复 upsert、404 envelope |

## 4. TDD 记录

先新增测试后运行：

```powershell
cd backend
mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test
```

首次 RED 运行未进入本测试编译阶段，被当时工作区中的无关主代码编译错误阻塞：

- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:70`
- 错误：`List<KbDocChunk>` 无法转换为 `int`

随后实现 Prompt Version 生产代码并运行目标测试。

## 5. 测试结果

### 执行的命令

```powershell
cd backend
mvn clean "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test
mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test
```

### 结果

| 命令 | 结果 | 备注 |
|---|---|---|
| `mvn clean "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test` | 通过 | 7 tests, 0 failures, 0 errors, 0 skipped |
| `mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test` | 通过 | 7 tests, 0 failures, 0 errors, 0 skipped |

测试报告：

- `backend/target/surefire-reports/com.learningos.agent.api.PromptVersionControllerTest.txt`
- `backend/target/surefire-reports/com.learningos.agent.application.PromptVersionServiceTest.txt`

## 6. 架构漂移检查

- Controller 只处理 HTTP 与响应包装，业务逻辑在 Service：通过。
- Repository 只做 JPA 查询：通过。
- 未新增数据库 migration：通过。
- 未新增依赖：通过。
- 未修改 `AgentRunRecorder` 和 `model_call_log`：通过。
- 无前端、RAG、Agent 执行链路变更：通过。

## 7. 已知限制

- 当前 `prompt_version` 表没有 `description`、`hash`、`is_active` 独立列，本轮按实际列实现，用 `status=ACTIVE` 表达激活。
- evaluation set 管理未实现，后续需要独立任务补表、服务、API 和指标对比。
