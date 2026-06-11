# Review Gate 强约束 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/dto/GeneratedResourceResponse.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/subagents/runs/RUN-20260606-review-gate-hardening.md`

## 不允许修改

- 前端代码。
- RAG 生产代码。
- Orchestrator 生产代码。
- 数据库迁移。
- 依赖配置。

## 当前任务边界

只做 P0-4 的最小安全切片：未审核资源正文不从学生可访问的任务详情和创建响应泄漏。资源状态枚举扩展、审核结构化字段、教师详情页进入后续任务。

## 测试命令

```bash
cd backend
mvn "-Dtest=ResourceGenerationControllerTest" test
mvn test
```
