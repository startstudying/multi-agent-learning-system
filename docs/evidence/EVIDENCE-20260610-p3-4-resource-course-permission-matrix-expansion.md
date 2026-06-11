# EVIDENCE-20260610-p3-4-resource-course-permission-matrix-expansion

## 1. 任务

P3-4 子任务：resource/course permission matrix expansion。

## 2. 结论

验收结论：PASS。

本切片作为 S Fast Lane 完成，性质是权限矩阵测试补强：

- 只新增 / 调整 MockMvc 权限回归测试。
- 未修改生产代码。
- 未修改 REST API path、请求 DTO、响应 DTO、数据库 schema、依赖、部署配置或前端代码。
- 新增测试均通过，未暴露需要升级为 M 的生产越权缺陷。
- P3-4 父项仍保持 open，不标记整体完成。

## 3. 覆盖点

本轮固定以下 resource/course/class 高价值权限行为：

1. `learner-resources` 由 Bearer owner 在资源审核发布后读取，并忽略伪造 `X-User-Id`。
2. Bearer `ADMIN` 不能绕过 learner-only 视图读取 foreign learner released resources。
3. Bearer student 对 foreign task 与 missing task 保持同类安全拒绝语义。
4. course-bound resource create 允许 Bearer owner + ACTIVE enrollment，即使请求头伪造其他用户。
5. course-bound resource create 拒绝 Bearer teacher 为 own-course student 代创建资源，且无持久化副作用。
6. course knowledge dependency 写路径拒绝 Bearer `USER sub=teacher_*` 的 subject-name role-confusion。
7. analytics student summary 在 Bearer student + spoofed admin header 下仍保持 owner-only / active-enrollment 语义。
8. resource review list 对 Bearer teacher no-prefix 只返回 own-course review，不泄露 foreign course review。

## 4. 变更文件

测试文件：

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`

流程文档：

- `docs/tasks/TASK-20260610-p3-4-resource-course-permission-matrix-expansion.md`
- `docs/subagents/runs/RUN-20260610-p3-4-resource-course-permission-matrix-expansion-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-resource-course-permission-matrix-expansion-test.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-resource-course-permission-matrix-expansion.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 专家 subagent 并行复核

已使用专家 subagent 并行分析 / 复核：

- Security Reviewer：识别 RBAC、IDOR、header spoofing、role-confusion 高价值缺口，建议本切片保持 S，只补测试。
- Test Engineer：复核现有测试矩阵并推荐最小补洞集合。
- 收尾阶段并行复核采纳点：Evidence/Acceptance 必须补齐；TASK 状态必须从 In Progress 改为 Done；`agent task cancel` 与 `course create USER sub=admin` 不纳入本切片，作为后续 residual matrix 处理。

## 6. 验证命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest test
```

结果：

```text
Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,AgentTraceControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,CourseAccessServiceTest test
```

结果：

```text
Tests run: 139, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 555, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

说明：Maven 输出包含 Mockito dynamic agent / ByteBuddy 运行时 warning，不影响本次测试结果；未出现测试失败或编译失败。

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 本切片只补测试，不改变 Controller / Service / Repository 分层。 |
| Frontend rules | N/A | 未修改前端。 |
| Agent / RAG rules | PASS | 未改变 Agent/RAG runtime；只通过现有 HTTP 行为验证权限边界。 |
| Security | PASS | 权限行为仍由后端代码执行；未新增 secret 或依赖。 |
| API / Database | PASS | 未修改 API contract 或数据库 schema。 |

## 8. Acceptance Criteria

| Criteria | Verdict | Evidence |
|---|---|---|
| 新增测试覆盖专家指出的 resource/course/class 高价值矩阵缺口 | PASS | 已覆盖 learner-resources、course-bound resource create、knowledge dependency、analytics student summary、review list。 |
| 新增测试全部通过且无需生产代码修复 | PASS | focused / adjacent / full backend 均 GREEN。 |
| 如暴露生产缺陷则升级 M | PASS | 未暴露生产 RED，无需升级。 |
| focused、adjacent、full backend 验证完成 | PASS | `106/106`、`139/139`、`555 run, 0 failures, 0 errors, 1 skipped`。 |
| Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成 | PASS | 本文件合并 Evidence/Acceptance，并同步更新相关文档。 |

## 9. Out of Scope / Remaining Risks

本切片不关闭 P3-4 父项。仍保留：

- `agent task cancel` roles-first / role-confusion residual matrix。
- course create `USER sub=admin` residual matrix。
- dev/test legacy fallback cleanup。
- frontend production SSE client / sensitive URL cleanup。
- 更宽的业务级 course/class/resource 权限矩阵。

