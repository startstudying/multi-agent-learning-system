# TASK - 权限与安全加固

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260606-permission-security-hardening.md`
- SPEC：`docs/specs/SPEC-20260606-permission-security-hardening.md`
- 任务编号：TASK-20260606-permission-security-hardening

## 2. 目标

收紧 P3-4 中高风险的权限边界，补齐 owner/admin/strict KB 过滤测试，并避免 health 与 analytics 暴露不必要的敏感信息。

## 3. 范围

### 纳入范围

- Profile owner 校验
- Learning Path owner 校验
- analytics overview admin-only
- health 敏感字段收敛
- RAG mixed `kbIds` strict 拒绝
- `GET /api/rag/query` 复用 strict 查询入口
- 安全测试与 evidence

### 排除范围

- 不重做生产认证体系
- 不引入 Spring Security 新依赖
- 不新增 RBAC 数据库结构
- 不改 RAG 索引和模型接入

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/learning/api/ProfileController.java`
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/health/application/HealthService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/test/java/**` 中对应测试
- `docs/context/CONTEXT-20260606-permission-security-hardening.md`
- `docs/evidence/EVIDENCE-20260606-permission-security-hardening.md`
- `docs/acceptance/ACCEPT-20260606-permission-security-hardening.md`
- `docs/retrospectives/RETRO-20260606-permission-security-hardening.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`

## 5. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- 与本任务无关的模块

## 6. 实施步骤

1. 先补文档和 Context Pack。
2. 再补 owner/admin/strict KB 的测试缺口。
3. 按测试驱动方式收敛实现。
4. 运行验证命令。
5. 生成 evidence / acceptance / retro。
6. 更新 TODO、memory、changelog。

## 7. 测试命令

```bash
cd backend && mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test
```

## 8. 完成标准

- [x] Profile owner 校验通过
- [x] Learning Path owner 校验通过
- [x] analytics overview admin-only 通过
- [x] health 敏感字段收敛通过
- [x] RAG mixed `kbIds` strict 拒绝通过
- [x] `GET /api/rag/query` handler 覆盖通过
- [x] 安全证据和验收完成

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 2026-06-06 |

## 10. 证据

- Evidence：`docs/evidence/EVIDENCE-20260606-permission-security-hardening.md`
- Acceptance：`docs/acceptance/ACCEPT-20260606-permission-security-hardening.md`
- Retrospective：`docs/retrospectives/RETRO-20260606-permission-security-hardening.md`
