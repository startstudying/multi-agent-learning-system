# TASK-20260610-p3-4-course-knowledge-forged-business-object-http-matrix

## 1. 任务名称

P3-4 子任务：Course/Knowledge forged business-object HTTP matrix

## 2. 任务类型

后端权限 / IDOR / 业务对象一致性渗透测试补强。

## 3. 目标

在不修改生产代码、REST API 合同、数据库 schema、依赖、部署或前端的前提下，补齐 Course/Knowledge 写路径中高价值 forged business-object HTTP 回归测试：

- 已授权教师或管理员不能把 `courseId` 与其他课程的 `chapterId` 拼接后创建知识点。
- 已授权教师不能把本课程 `knowledgePointId` 与其他课程 `prerequisiteId` 拼接后创建知识依赖。
- 拒绝响应不泄露 forged id、课程/章节/知识点标题或请求标题。
- 拒绝请求不产生 `KnowledgePoint` 或 `KnowledgeDependency` 持久化副作用。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有开发请求走 S/M/L 工作流、文档、验证、Evidence、Memory 闭环。 |
| `subagent-driven-development` | 用户明确要求专家 subagent 并行开发；本轮使用专家并行只读审查，主线程单文件实现，避免冲突。 |
| `object-scope-authorization` | 本任务验证 course/chapter/knowledge-point 父对象一致性、IDOR 防护和响应去敏。 |
| `auth-context-boundary` | 新测试覆盖 Bearer 身份优先、spoofed `X-User-Id` 不提升权限。 |
| `test-driven-development` | 本任务为测试优先的安全回归补洞；如 RED 暴露生产缺陷再升级任务。 |
| `Confidence Check` | 实现前确认无重复覆盖、架构合规、根因和边界清晰。 |

缺失技能：无。

GitHub research：不需要。该任务只验证项目内已有权限/一致性规则，不引入新模式或依赖。

新项目技能：暂不创建。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 只影响 Course/Knowledge 权限测试矩阵。
- 预计只修改 1 个测试类和工作流文档。
- 不改变生产 REST API、DTO、数据库 schema、依赖、部署、前端或 Agent/RAG runtime。
- 可用 focused / adjacent / full backend Maven 测试验证。

可跳过：PRD、REQ、SPEC、PLAN、standalone Context Pack、独立 Acceptance、Retrospective。

升级触发：如果测试 RED 暴露现有生产代码未拒绝 forged business object，停止并升级为 M 任务，补 REQ/SPEC/PLAN/CONTEXT 后再改生产代码。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求专家 subagent 并行开发；本任务涉及权限/安全矩阵，适合 Security & Quality / Course-Knowledge 专家并行只读审查。

Parallelism Level：L1 - Parallel Analysis。

Implementation Mode：Single Codex implementation。

说明：

- 当前 subagent 线程达到上限，无法新建专家；已复用既有 Course/Knowledge 专家线程 `019eb022-0a2c-7ac0-8d64-0ca3ebd2a81e` 排队只读审查。
- 为避免同一测试文件冲突，主线程负责唯一代码修改；subagent 只输出测试设计/风险/验收建议。

专家报告文件：

- `docs/subagents/runs/RUN-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本子任务只补 Course/Knowledge forged business-object HTTP 测试，不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/knowledge/api/KnowledgePointController.java`

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 7.4 禁止修改文件

除非升级为 M 并补齐对应文档，本子任务不得修改：

- `backend/src/main/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- API / schema / dependency / deployment 配置

### 7.5 计划新增测试

1. `knowledgePointRejectsForgedForeignChapterIdForAuthorizedWritersWithoutSideEffects`
2. `knowledgeDependencyRejectsCrossCourseForgedPrerequisiteWithoutSideEffects`

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 测试-only；权限仍由 Service 层执行。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime。 |
| Security | PASS | 补强业务对象一致性与响应去敏测试；不新增 secret。 |
| API / Database | PASS | 不改 API contract 或 schema。 |

## 8. Acceptance Criteria

- 新增测试覆盖 `courseId` + foreign `chapterId` 创建知识点的拒绝路径。
- 新增测试覆盖跨课程 `knowledgePointId` + `prerequisiteId` 创建依赖的拒绝路径。
- 拒绝响应不包含 forged id、课程/章节/知识点标题或请求标题，且无 `data`。
- 拒绝请求不新增 `KnowledgePoint` / `KnowledgeDependency`。
- focused、adjacent、full backend 验证完成。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

新增测试：

- `knowledgePointRejectsForgedForeignChapterIdForAuthorizedWritersWithoutSideEffects`
- `knowledgeDependencyRejectsCrossCourseForgedPrerequisiteWithoutSideEffects`

验证：

- Focused：`mvn --% -Dtest=CourseKnowledgeControllerTest test`，`29 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest test`，`35 run, 0 failures, 0 errors, 0 skipped`。
- Full backend：`mvn test`，`574 run, 0 failures, 0 errors, 1 skipped`。

验收：PASS。未修改生产代码、REST API、DTO、DB schema、依赖、部署或前端。

P3-4 父项保持 open；仍需继续 Evaluation/Review forged-id、Assessment submit foreign-questionId、dev/test legacy fallback cleanup、frontend SSE sensitive URL cleanup 等剩余项。
