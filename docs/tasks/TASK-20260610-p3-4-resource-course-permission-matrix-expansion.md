# TASK-20260610-p3-4-resource-course-permission-matrix-expansion

## 1. 任务名称

P3-4 子任务：resource/course permission matrix expansion

## 2. 任务类型

权限 / 安全测试矩阵扩展。

## 3. 目标

在不改变生产代码、API 合同、数据库 schema 或依赖的前提下，补齐 P3-4 中 resource/course/class 权限矩阵的高价值回归测试，固定以下行为：

- `learner-resources` 只能由资源所属 learner 在审核发布后读取；Bearer `ADMIN` 不应绕过 learner-only 视图。
- Bearer 身份必须优先于伪造的 `X-User-Id`。
- Bearer `USER sub=admin` / `USER sub=teacher_*` 不得获得 admin / teacher 语义。
- 课程绑定的资源生成只能由目标 learner 在 ACTIVE enrollment 下创建；教师即便拥有课程，也不能代学生创建 learner-owned resource task。
- course/knowledge dependency 写路径不能因为 token subject 形如 `teacher_*` 而获得 teacher 权限。
- analytics/review list 输出不得泄露 foreign course/resource/review 信号。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有开发请求进入受控 S/M/L 工作流。 |
| `security-review` | 本任务验证 RBAC、IDOR、header spoofing 和 role-confusion。 |
| `object-scope-authorization` | 匹配对象详情、防枚举、课程/资源归属授权规则。 |
| `auth-context-boundary` | 匹配 Bearer roles-first、`X-User-Id` spoofing 和 subject-name role-confusion 规则。 |
| `test-generator` | 本任务主要交付 MockMvc 权限矩阵测试。 |
| `verification-before-completion` | 完成前必须提供 focused / adjacent / full 验证证据。 |

缺失技能：无。

GitHub research：不需要。本任务是项目内安全矩阵回归补强，不新增依赖或外部框架模式。

新项目技能：暂不创建；若后续发现可复用 RBAC 矩阵模板，再单独沉淀。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 主要修改测试文件，预计不超过 3 个测试类。
- 不改变 REST API path、请求/响应 DTO、数据库 schema、依赖、部署或前后端合同。
- 两个专家 subagent 均判断当前 HTTP 主路径未发现必须先修的生产越权；本轮优先补测试钉子。
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

- Security Reviewer：只读审查 RBAC 授权路径和安全缺口。
- Test Engineer：只读审查现有测试矩阵并推荐最小补洞集合。

Implementation Mode：主 Codex 单任务集成实现。

不使用并行实现的原因：

- 本轮写入高度集中在少数测试文件。
- 多个 worker 同时改同一测试类会增加冲突和重复风险。

专家报告：

- `docs/subagents/runs/RUN-20260610-p3-4-resource-course-permission-matrix-expansion-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-resource-course-permission-matrix-expansion-test.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本切片只补 resource/course/class 权限渗透测试。除非测试暴露 RED 生产缺陷，否则不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
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
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-resource-course-permission-matrix-expansion.md`
- `docs/subagents/runs/RUN-20260610-p3-4-resource-course-permission-matrix-expansion-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-resource-course-permission-matrix-expansion-test.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-resource-course-permission-matrix-expansion.md`
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

1. `learner-resources`：Bearer owner + spoofed `X-User-Id` 在发布后可读 learner-safe DTO。
2. `learner-resources`：Bearer admin 不能读取 foreign learner released resources，且响应不泄露内容。
3. `learner-resources`：Bearer student 对 foreign task 与 missing task 返回同类安全拒绝。
4. course-bound resource create：Bearer owner + ACTIVE enrollment + spoofed header 可以创建。
5. course-bound resource create：Bearer teacher own-course 不能为学生代创建，且无持久化副作用。
6. course dependency write：Bearer `USER sub=teacher_*` 不能在 subject-owned course 中创建 dependency。
7. analytics student summary：Bearer student + spoofed admin header 仍 owner-only / active enrollment only。
8. review list：Bearer teacher no-prefix 列表只返回 own-course review，不泄露 foreign review。

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,AgentTraceControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,CourseAccessServiceTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

- Backend layering：PASS，本切片只补测试，不改变 Controller / Service / Repository 分层。
- Frontend rules：N/A，不改前端。
- Agent / RAG rules：PASS，不改 Agent/RAG runtime；resource review/trace 行为只通过现有接口验证。
- Security：PASS，不新增 secret / 依赖；权限仍由后端代码执行。
- API / Database：PASS，不改 API contract 或 schema。

## 8. Acceptance Criteria

- 新增测试覆盖专家指出的 resource/course/class 高价值矩阵缺口。
- 如新增测试全部通过且不需要生产代码修复，明确记录为测试矩阵补强。
- 如新增测试失败且暴露生产代码缺陷，停止并升级任务到 M。
- focused、adjacent、full backend 验证完成，或清晰说明无法运行的环境限制。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

完成说明：

- 已新增 resource/course/class 高价值权限矩阵 MockMvc 回归测试。
- 未修改生产代码、API、DTO、DB schema、依赖、部署配置或前端代码。
- focused、adjacent、full backend 验证均通过。
- `agent task cancel` 与 course create `USER sub=admin` residual matrix 不纳入本 S 切片，留后续任务。
- P3-4 父项仍保持 open。

证据：

- `docs/evidence/EVIDENCE-20260610-p3-4-resource-course-permission-matrix-expansion.md`
