# TASK-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix

## 1. 任务名称

P3-4 子任务：agent task cancel / course create role-confusion residual matrix

## 2. 任务类型

权限 / 安全测试矩阵残余补强。

## 3. 目标

在不改变生产代码、API 合同、数据库 schema 或依赖的前提下，补齐 P3-4 中两个残余高价值权限测试点：

- `POST /api/agent/tasks/{taskId}/cancel` 的 Bearer roles-first / spoofed header / subject-name role-confusion 矩阵。
- `POST /api/courses` 的 Bearer `USER sub=admin` / `TEACHER` spoofed header 矩阵。

当前业务语义：

- Agent task cancel 是 owner-only，不授予 admin / teacher 特权取消他人任务。
- Course create 只允许 explicit `ADMIN` 指定教师，explicit `TEACHER` 创建自己的课程；`USER sub=admin` / `USER sub=teacher_*` 不应升权。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有开发请求进入受控 S/M/L 工作流。 |
| `security-review` | 本任务验证 RBAC、IDOR、header spoofing、role-confusion 和拒绝无副作用。 |
| `object-scope-authorization` | 匹配 agent task / course create 对象及业务权限边界。 |
| `auth-context-boundary` | 匹配 Bearer 优先、`X-User-Id` spoofing 和 subject-name role-confusion 规则。 |
| `test-driven-development` | 若暴露 RED 且需改生产代码，必须先有测试证据。 |
| `verification-before-completion` | 完成前必须提供 focused / adjacent / full 验证证据。 |

缺失技能：无。

GitHub research：不需要。本任务是项目内安全矩阵回归补强，不新增依赖或外部框架模式。

新项目技能：暂不创建。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 预计只修改 2 个测试类和流程文档。
- 不改变 REST API path、请求/响应 DTO、数据库 schema、依赖、部署或前后端合同。
- 两个专家 subagent 均判断生产代码未见明显 role-confusion 提权路径，本轮优先补测试钉子。
- 若新增测试暴露真实生产缺陷，则停止并升级为 M，补 REQ / SPEC / PLAN / TASK / CONTEXT 后再修生产代码。

可跳过：

- PRD
- REQ
- SPEC
- PLAN
- standalone Context Pack

必须文档：

- 本 mini TASK，内嵌 Context Pack。
- 完成后创建 combined Evidence/Acceptance。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求专家 subagent 并行开发；本任务涉及权限与测试矩阵，适合 L1 并行专家分析。

Parallelism Level：L1 Parallel Analysis。

已使用专家：

- Security & Quality：只读审查 cancel/course create 授权路径、风险与升级条件。
- Test Engineer：只读审查现有测试覆盖并推荐最小 MockMvc 测试集合。

Implementation Mode：主 Codex 单任务集成实现。

不使用并行实现的原因：

- 本轮写入集中在两个测试类。
- 多个 worker 同时改同一测试类会增加冲突和重复风险。

专家报告：

- `docs/subagents/runs/RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-test.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本切片只补 agent task cancel 与 course create 残余权限渗透测试。除非测试暴露 RED 生产缺陷，否则不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/harness/TEST_COMMANDS.md`

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix-test.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 7.4 禁止修改文件

除非升级为 M 并更新 Context Pack，本切片不得修改：

- `backend/src/main/**`
- `backend/pom.xml`
- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- API / schema / dependency / deployment 配置

### 7.5 待补测试清单

优先补以下路径：

1. cancel：Bearer owner + spoofed `X-User-Id` 可以取消自己的 task。
2. cancel：Bearer `USER sub=admin` 不能取消 foreign task，且 task status / trace 数量不变。
3. cancel：Bearer `USER sub=teacher_*` 不能取消 foreign task，且 task status / trace 数量不变。
4. cancel：Bearer non-owner + spoofed owner header 不能取消 foreign task，且不追加 `task_cancelled` trace。
5. course create：Bearer `USER sub=admin` 不能创建课程，且不持久化 course。
6. course create：Bearer `TEACHER` + spoofed admin header 只能为 token subject 创建自己的课程，不能使用伪造 header 升权。

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,AgentRunRecorderTest,CourseKnowledgeControllerTest,CourseAccessServiceTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

- Backend layering：PASS，本切片只补测试；若升级修复，必须保持 Controller -> Service -> Repository 分层。
- Frontend rules：N/A，不改前端。
- Agent / RAG rules：PASS，不改变 Agent/RAG runtime。
- Security：PASS，不新增 secret / 依赖；权限仍由后端代码执行。
- API / Database：PASS，不改 API contract 或 schema。

## 8. Acceptance Criteria

- 新增测试覆盖专家指出的 cancel/course create 残余矩阵缺口。
- 如新增测试全部通过且不需要生产代码修复，明确记录为测试矩阵补强。
- 如新增测试失败且暴露生产代码缺陷，停止并升级任务到 M。
- focused、adjacent、full backend 验证完成，或清晰说明无法运行的环境限制。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

证据：

- `docs/evidence/EVIDENCE-20260610-p3-4-cancel-course-create-role-confusion-residual-matrix.md`
- Focused：`mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest test`，`57 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,AgentRunRecorderTest,CourseKnowledgeControllerTest,CourseAccessServiceTest test`，`80 run, 0 failures, 0 errors, 0 skipped`。
- Full backend：`mvn test`，`561 run, 0 failures, 0 errors, 1 skipped`。

验收：PASS。本切片仅补测试，未修改生产代码、API、DTO、DB schema、依赖、部署或前端；P3-4 父项仍保持 open。
