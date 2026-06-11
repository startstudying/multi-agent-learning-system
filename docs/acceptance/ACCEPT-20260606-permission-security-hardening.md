# Acceptance - P3-4 权限与安全加固

## 1. 验收结论

通过。

本轮已完成 P3-4 的最小高风险权限收口切片。完整生产认证、课程/班级 RBAC、资源与答题记录全量权限矩阵仍保留为 P3-4 后续项。

## 2. 验收清单

| 项目 | 状态 | 证据 |
|---|---|---|
| Profile owner 校验 | 通过 | `LearningWorkflowControllerTest.profileExtractRejectsCrossLearnerAccessAndDoesNotPersist` |
| Learning Path 创建 owner 校验 | 通过 | `LearningWorkflowControllerTest.learningPathCreateRejectsCrossLearnerAccessAndDoesNotPersist` |
| Learning Path 查询 owner 校验 | 通过 | `LearningWorkflowControllerTest.learningPathGetRejectsNonOwnerWithoutExposingPathData` |
| analytics overview admin-only | 通过 | `AnalyticsControllerTest.overviewRejectsNonAdminAccess` 与 admin happy path |
| Health 敏感输出收敛 | 通过 | `HealthControllerTest.healthReturnsPhaseOneDependencyShape` 检查原始响应体不含敏感配置和拓扑指纹 |
| RAG mixed `kbIds` strict 拒绝 | 通过 | `RagQueryServiceTest.rejectsMixedAllowedAndForbiddenKnowledgeBasesWithoutPersistingArtifacts` |
| `GET /api/rag/query` strict 查询入口 | 通过 | `ChatControllerTest.getRagQueryUsesSameStrictQueryServicePath` |
| 拒绝时无 RAG 伪成功证据 | 通过 | 同上，断言 `kb_query_log` 与 `source_citation` 均为 0 |
| 聚焦后端回归 | 通过 | `mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test`：38 run, 0 failures, 0 errors |
| 全量后端回归 | 通过 | `mvn test`：217 run, 0 failures, 0 errors, 1 skipped |

## 3. 残余风险

- `X-User-Id` 仍是开发期身份替代，不能等同生产认证。
- 教师课程/班级授权仍是临时或局部规则，完整 RBAC 未纳入本切片。
- 资源、答题记录、课程、taskId/resourceId 等全量权限渗透测试仍需后续扩展。
- 本地配置中仍存在开发默认值，例如 `learning_os` / `minioadmin`，生产必须通过环境变量或 secret manager 覆盖。
- 本地 hardcoded-secret skill 脚本自身语法错误，已用 `rg` 替代扫描并在 Evidence 中记录限制。

## 4. 验收人

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
