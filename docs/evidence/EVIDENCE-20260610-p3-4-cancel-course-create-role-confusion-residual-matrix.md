# EVIDENCE-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix

## 1. 任务

P3-4 子任务：agent task cancel / course create role-confusion residual matrix。

## 2. 结论

验收结论：PASS。

本切片作为 S Fast Lane 完成，性质是权限矩阵测试补强：

- 只新增 MockMvc 权限回归测试。
- 未修改生产代码。
- 未修改 REST API path、请求 DTO、响应 DTO、数据库 schema、依赖、部署配置或前端代码。
- 新增测试均通过，未暴露需要升级为 M 的生产越权缺陷。
- P3-4 父项仍保持 open，不标记整体完成。

## 3. 覆盖点

本轮固定以下残余权限行为：

1. `POST /api/agent/tasks/{taskId}/cancel`：Bearer owner 即使带 spoofed `X-User-Id` 也能取消自己的 task。
2. `POST /api/agent/tasks/{taskId}/cancel`：Bearer `USER sub=admin` 不能取消 foreign task，且 task 状态与 trace 数量不变。
3. `POST /api/agent/tasks/{taskId}/cancel`：Bearer `USER sub=teacher_*` 不能取消 foreign task，且 task 状态与 trace 数量不变。
4. `POST /api/agent/tasks/{taskId}/cancel`：Bearer non-owner 即使 spoof owner header 也不能取消 foreign task，且不追加 `task_cancelled` trace。
5. `POST /api/courses`：Bearer `USER sub=admin` 不能通过 subject-name role-confusion 创建课程，且不落库。
6. `POST /api/courses`：Bearer `TEACHER` 即使 spoof admin header，也只能以 token subject 创建自己的课程，不能使用伪造 header 升权或指定他人教师身份。

## 4. 变更文件

测试文件：

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`

流程文档：

- `docs/tasks/TASK-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-test.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 专家 subagent 并行复核

已使用专家 subagent 并行分析 / 复核：

- Security & Quality：只读审查 cancel/course create 授权路径、role-confusion 风险与 TODO 收口边界，结论为当前 S 切片足够关闭 TODO 中明确点名的 `agent task cancel` 与 `course create role-confusion` 残余描述，但不能关闭 P3-4 父项或更宽业务权限矩阵。
- Test Engineer：只读审查现有测试覆盖并推荐最小 MockMvc 测试集合，结论为保持 S，仅补测试；若 RED 暴露生产缺陷再升级 M。
- 主 Codex：集成测试实现、验证、Evidence/Acceptance、Changelog、Memory 与 TODO 更新。

## 6. 验证命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest test
```

结果：

```text
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,AgentRunRecorderTest,CourseKnowledgeControllerTest,CourseAccessServiceTest test
```

结果：

```text
Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 561, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

说明：

- 首次 `mvn test` 使用 120 秒超时被工具中止，不计为测试失败；随后使用 600 秒超时重跑并通过。
- Maven 输出包含 Mockito dynamic agent / ByteBuddy 运行时 warning，不影响本次测试结果；未出现测试失败或编译失败。

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
| 新增测试覆盖专家指出的 cancel/course create 残余矩阵缺口 | PASS | 已覆盖 owner cancel、foreign cancel、spoofed header、`USER sub=admin` / `USER sub=teacher_*` role-confusion、course create role-confusion。 |
| 新增测试全部通过且无需生产代码修复 | PASS | focused / adjacent / full backend 均 GREEN。 |
| 如暴露生产缺陷则升级 M | PASS | 未暴露生产 RED，无需升级。 |
| focused、adjacent、full backend 验证完成 | PASS | `57/57`、`80/80`、`561 run, 0 failures, 0 errors, 1 skipped`。 |
| Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成 | PASS | 本文件合并 Evidence/Acceptance，并同步更新相关文档。 |

## 9. Out of Scope / Remaining Risks

本切片不关闭 P3-4 父项。仍保留：

- dev/test legacy fallback cleanup。
- frontend production SSE client / sensitive URL cleanup。
- `course/class/resource` 更宽业务权限矩阵。
- `ResourceGenerationService` 仍存在 legacy `isAdmin(userId)` helper 风险面；当前 roles-first controller 路径已由测试压住，高风险入口未复现越权，但未来新增 caller 仍需避免误用 legacy helper。
