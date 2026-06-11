# Context Pack - P3-4 权限与安全加固

## 当前任务

收紧 P3-4 的高风险权限边界：Profile owner 校验、Learning Path owner 校验、analytics overview admin-only、health 敏感信息收敛、RAG mixed `kbIds` strict 拒绝，并补充对应安全测试。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`

## 关联文档

- `docs/planning/backend-architecture-todolist.md` 中 P3-4
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

## 已选 Skills

- `feature-development-workflow`
- `security-review`
- `java-security-review`
- `systematic-debugging`
- `test-driven-development`
- `verification-before-completion`

## Subagent 计划

### 是否启用 Subagent

是，已完成并行分析。

### 原因

P3-4 涉及 learning、analytics、health、rag 四个模块，且存在明显安全风险，需要并行分析后再实施。当前并行分析已由 Security & Quality、Backend / Spec Architect 汇总完成，后续只做单 Codex 收敛实现。

### 任务复杂度

| 影响模块数 | 涉及安全 | 涉及 RAG |
|---|---|---|
| 4 | 是 | 是 |

### 选中的专家

- Backend Expert
- Security & Quality
- Integration Reviewer

### 并行级别

- [x] L1 - 仅分析
- [x] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

单 Codex 收敛实现。

### 文件归属

| 领域 | 负责专家 | 允许修改的文件 |
|---|---|---|
| 学生画像与学习路径 | Backend Expert | `backend/src/main/java/com/learningos/learning/**` |
| analytics / health | Backend Expert | `backend/src/main/java/com/learningos/analytics/**`、`backend/src/main/java/com/learningos/health/**` |
| RAG 权限边界 | Security & Quality | `backend/src/main/java/com/learningos/rag/**`、`backend/src/test/java/com/learningos/rag/**` |
| 交付证据 | Integration Reviewer | `docs/context/**`、`docs/evidence/**`、`docs/acceptance/**` |

## 关联代码区域

- `backend/src/main/java/com/learningos/learning/api/ProfileController.java`
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/health/application/HealthService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/test/java/com/learningos/**`

## 允许修改的文件

- 见 TASK 文档

## 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`

## 测试命令

```bash
cd backend && mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test
```

## 任务边界

仅完成 P3-4 的最小安全收口，不重做生产认证、不新增依赖、不新增数据库结构。重点是 owner/admin/strict KB 的可验证收敛。

## 执行结果

- 已完成 Profile owner 校验、Learning Path owner 校验、analytics overview admin-only、health 敏感输出收敛、RAG mixed `kbIds` strict 拒绝。
- 代码审查后补齐 `GET /api/rag/query` handler，并由 `ChatControllerTest` 覆盖。
- 聚焦测试通过：`cd backend && mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test`，38 tests, 0 failures, 0 errors。
- 全量后端测试通过：`cd backend && mvn test`，217 tests, 0 failures, 0 errors, 1 skipped。
- Evidence：`docs/evidence/EVIDENCE-20260606-permission-security-hardening.md`。
- Acceptance：`docs/acceptance/ACCEPT-20260606-permission-security-hardening.md`。
