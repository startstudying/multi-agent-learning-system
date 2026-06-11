# Review Gate 强约束证据

## 范围

本轮完成 P0-4 的最小安全切片：资源生成任务在 Critic/教师审核通过前，不允许通过创建响应或任务详情响应泄漏 `markdownContent`。正式学生资源读取接口继续由 `ReviewGovernanceService.canReleaseToLearner(taskId)` 兜底。

## 代码证据

- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
  - `toResponse(...)` 在 Service 层计算 `releasedToLearner`。
  - `toResourceResponse(...)` 仅在 `releasedToLearner == true` 时填充 `markdownContent`。
  - `getLearnerResources(...)` 保持 `canReleaseToLearner(taskId)` 403 门禁。
- `backend/src/main/java/com/learningos/agent/dto/GeneratedResourceResponse.java`
  - 增加 `@JsonInclude(JsonInclude.Include.NON_NULL)`，未发布正文以字段缺失表达。
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
  - 覆盖创建响应、任务详情、幂等重放响应的未审核正文不可见。
  - 覆盖全部审核通过后任务详情和正式学生资源接口可见正文。

## TDD 过程

### RED

先补测试后运行聚焦测试，出现 3 个失败：

```text
cd backend
mvn "-Dtest=ResourceGenerationControllerTest" test
```

失败原因：`POST /api/resources/generation-tasks`、`GET /api/resources/generation-tasks/{taskId}`、重复 `requestId` 重放响应仍返回 `markdownContent`。

### GREEN：聚焦测试

实现 Service 层正文屏蔽后重新运行：

```text
cd backend
mvn "-Dtest=ResourceGenerationControllerTest" test
```

结果：

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：全量后端测试

完成后运行：

```text
cd backend
mvn test
```

结果：

```text
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T01:41:41+08:00
```

## Subagent 审查证据

详见 `docs/subagents/runs/RUN-20260606-review-gate-hardening.md`：

- Fermat 指出未审核资源正文曾通过任务创建响应和任务详情泄漏，本轮已修复。
- Sartre 指出 RAG 上传、索引和查询幂等仍未统一，本轮记录为后续 P0-3 切片。
- Epicurus 指出 Orchestrator 还没有完全统一下游 workflow context，本轮记录为后续 P0-1 切片。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | 审核门禁位于 `ResourceGenerationService`，Controller 仍只处理 HTTP。 |
| Frontend rules | PASS | 未修改前端，没有新增 LLM/API key 暴露面。 |
| Agent / RAG rules | PASS | 资源仍保留 review status，正式读取继续走 Review Gate。 |
| Security | PASS | 未新增依赖或密钥；正文可见性由后端 Service 控制。 |
| API / Database | PASS | API 行为符合本轮 SPEC；未修改数据库 schema。 |
