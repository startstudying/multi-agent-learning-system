# TASK-20260610-p3-4-course-class-resource-broader-business-permission-matrix

## 1. 任务名称

P3-4 子任务：course-class-resource broader business permission matrix

## 2. 任务类型

权限 / 安全测试矩阵补强。

## 3. 目标

在不修改生产代码、REST API 合同、数据库 schema、依赖、部署配置或前端代码的前提下，继续补齐 `course/class/resource` 更宽业务权限矩阵的高价值回归测试，固定以下行为：

- Bearer `TEACHER` 列课时必须忽略伪造的 `X-User-Id: admin`，只返回 token subject 拥有的课程。
- Bearer `STUDENT` 列课时必须忽略伪造的 `X-User-Id: teacher_*`，只返回 ACTIVE enrollment 课程，不返回 DROPPED enrollment 课程。
- 课程绑定资源生成必须要求目标 learner 对课程有 ACTIVE enrollment；DROPPED 或 never-enrolled learner 即使是 Bearer owner 也不能创建任务，且不能留下任务、资源、review、trace、model、token、citation 副作用。
- 教师读取自己课程下学生 summary 时，目标 learner 必须仍为 ACTIVE enrollment；DROPPED learner 返回安全 `FORBIDDEN`，不泄漏课程、知识点、学习路径、错题或资源信号。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有开发请求进入受控 S/M/L 工作流。 |
| `security-review` | 本任务验证 RBAC、IDOR、header spoofing、role-confusion 和副作用边界。 |
| `object-scope-authorization` | 匹配课程、班级、资源、active enrollment 和防枚举规则。 |
| `auth-context-boundary` | 匹配 Bearer roles-first、`X-User-Id` spoofing 和 subject-name role-confusion 规则。 |
| `test-driven-development` | 本切片先补测试；若 RED 暴露生产缺陷，则升级为 M 后再改生产代码。 |
| `multi-agent-coder` / `dispatching-parallel-agents` | 用户明确要求专家 subagent 并行开发；本切片可按测试文件拆为无重叠写入。 |
| `verification-before-completion` | 完成前必须提供 focused / adjacent / full 验证证据。 |

缺失技能：无。

GitHub research：不需要。本任务是项目内安全矩阵补强，不新增依赖或外部框架。

新项目技能：暂不创建；若后续形成通用 RBAC 矩阵模板，再单独沉淀。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 预计只修改 3 个测试类和工作流文档。
- 不改变 REST API path、请求/响应 DTO、数据库 schema、依赖、部署或前后端合同。
- 本轮目标是补齐测试矩阵；若测试暴露真实生产越权，立即停止并升级为 M。

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

原因：用户明确要求专家 subagent 并行开发；当前 3 个测试文件可互不重叠并行修改。

Parallelism Level：L3 bounded parallel implementation。

专家与写入边界：

- Course/Knowledge Expert：只改 `CourseKnowledgeControllerTest.java`，并写自己的 subagent report。
- Resource Generation Security Expert：只改 `ResourceGenerationControllerTest.java`，并写自己的 subagent report。
- Analytics Authorization Expert：只改 `AnalyticsControllerTest.java`，并写自己的 subagent report。

主 Codex 责任：

- 创建 TASK / Evidence。
- 集成检查 subagent 改动。
- 运行 focused / adjacent / full backend 测试。
- 更新 changelog / memory / TODO。

## 7. Embedded Context Pack

### 7.1 当前边界

本切片只补 `course/class/resource` 更宽业务权限矩阵测试。除非新增测试暴露 RED 生产缺陷，否则不得修改生产代码。

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

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-course-class-resource-broader-business-permission-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-course.md`
- `docs/subagents/runs/RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-resource.md`
- `docs/subagents/runs/RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-analytics.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-course-class-resource-broader-business-permission-matrix.md`
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

1. `CourseKnowledgeControllerTest.courseListBearerTeacherIgnoresSpoofedAdminHeaderAndReturnsOnlyOwnedCourses`
2. `CourseKnowledgeControllerTest.courseListBearerStudentWithSpoofedTeacherHeaderReturnsOnlyActiveEnrollments`
3. `ResourceGenerationControllerTest.courseBoundResourceGenerationCreateRejectsBearerOwnerWithDroppedEnrollmentWithoutSideEffects`
4. `ResourceGenerationControllerTest.courseBoundResourceGenerationCreateRejectsBearerOwnerWithNoEnrollmentWithoutSideEffects`
5. `AnalyticsControllerTest.bearerTeacherStudentSummaryRejectsDroppedLearnerInOwnCourseWithoutLeakingScope`

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

- Backend layering：PASS，本切片只补测试；若升级修复，必须保持 Controller -> Service -> Repository 分层。
- Frontend rules：N/A，不改前端。
- Agent / RAG rules：PASS，不改 Agent/RAG runtime。
- Security：PASS，不新增 secret / 依赖；权限仍由后端代码执行。
- API / Database：PASS，不改 API contract 或 schema。

## 8. Acceptance Criteria

- 新增测试覆盖待补 5 个高价值矩阵点。
- 如新增测试全部通过且不需要生产代码修复，明确记录为测试矩阵补强。
- 如新增测试失败并暴露生产代码缺陷，停止并升级任务为 M。
- focused、adjacent、full backend 验证完成，或清晰说明无法运行的环境限制。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

完成说明：

- 已补齐 5 个 `course/class/resource` 更宽业务权限矩阵 MockMvc 回归测试。
- 未修改生产代码、REST API 合同、DTO、DB schema、依赖、部署配置或前端代码。
- 复用既有专家线程时，Course 与 Analytics 专家受旧只读角色约束无法直接写文件；其分析结果由主 Codex 集成，Resource 专家直接完成了资源测试文件改动。
- focused、adjacent、full backend 验证均通过。
- P3-4 父项仍保持 open，后续继续处理 dev/test legacy fallback cleanup、frontend production streaming client / sensitive SSE URL cleanup，以及更宽 forged-id / 业务对象矩阵。

证据：

- `docs/evidence/EVIDENCE-20260610-p3-4-course-class-resource-broader-business-permission-matrix.md`
