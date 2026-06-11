# TASK-20260610-p3-4-prompt-version-forged-id-object-oracle-guards

## 1. 任务名称

P3-4 子任务：PromptVersion forged-id object-oracle guards

## 2. 任务类型

权限 / 安全测试矩阵补强。

## 3. 目标

在不修改生产代码、API 合同、数据库 schema、依赖或前端的前提下，补齐 PromptVersion 管理接口的 forged-id / object existence oracle 测试钉子：

- Bearer `ADMIN` 即使携带 spoofed `X-User-Id`，missing prompt version detail 仍返回 admin 运维语义 `NOT_FOUND`。
- Bearer `TEACHER` 是当前规格允许的 PromptVersion metadata reader；本切片只固化其 missing detail 为既有规格语义 `NOT_FOUND`，且不泄漏 `promptText` 或 `data`。
- Bearer `STUDENT` 访问 missing prompt version detail 同样返回安全 `FORBIDDEN`。
- Bearer `USER sub=admin` 不能读取 PromptVersion list/detail 管理数据，防止 subject-name role-confusion。

若新增测试暴露生产缺陷，停止并升级为 M 后再修生产代码。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有需求进入受控 S/M/L 工作流。 |
| `security-review` | 本任务验证 RBAC、IDOR、object oracle、header spoofing 和 role-confusion。 |
| `object-scope-authorization` | 匹配 non-admin missing-vs-foreign 防枚举和响应去敏规则。 |
| `auth-context-boundary` | 匹配 Bearer 优先、spoofed `X-User-Id` 和 subject-name role-confusion 规则。 |
| `test-driven-development` | 若测试 RED 暴露生产缺陷，先记录证据再升级修复。 |
| `verification-before-completion` | 完成前必须提供 fresh verification 证据。 |

缺失技能：无。

GitHub research：不需要。本任务是项目内现有安全矩阵补强，不新增依赖或外部模式。

新项目技能：暂不创建。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 只影响 PromptVersion 管理接口测试矩阵。
- 预计只修改 1 个测试类和流程文档。
- 不改变 public REST API contract、DTO、数据库 schema、依赖、部署或前后端合同。
- 如果测试失败并暴露真实生产缺陷，则停止并升级为 M，补 REQ / SPEC / PLAN / TASK / CONTEXT 后再修改生产代码。

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

原因：用户明确要求专家 subagent 并行开发；本任务来自 P3-4 broader forged-id / business-object penetration matrix，适合 L1 并行专家分析。

Parallelism Level：L1 Parallel Analysis。

已使用专家：

- Analytics / Review / Evaluation / PromptVersion 安全专家：只读审查 forged-id、missing-vs-foreign oracle、Bearer + spoofed header 组合。

Implementation Mode：主 Codex 单任务集成实现。

限制说明：

- 新建 subagent 触达线程上限，已复用现有专家线程。
- 为避免多个 worker 同改 `PromptVersionControllerTest.java`，本轮实现由主线程完成。

## 7. Embedded Context Pack

### 7.1 当前边界

本切片只补 PromptVersion 管理接口 forged-id / object-oracle 测试。除非测试暴露 RED 生产缺陷，否则不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/harness/TEST_COMMANDS.md`
- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
- `docs/subagents/runs/RUN-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
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

1. `bearerAdminMissingPromptVersionReturnsNotFoundDespiteSpoofedUserIdHeader`
2. `teacherMissingPromptVersionKeepsAuthorizedNotFoundWithoutPromptText`
3. `studentMissingPromptVersionReturnsForbiddenWithoutOracle`
4. `bearerUserSubjectAdminCannotReadPromptVersionManagementData`

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest,SecurityFilterChainTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

- Backend layering：PASS，本切片默认只补测试；若升级修复，必须保持 Controller -> Service 分层。
- Frontend rules：N/A，不改前端。
- Agent / RAG rules：PASS，不改变 Agent/RAG runtime。
- Security：PASS，不新增 secret / 依赖；权限仍由后端代码执行。
- API / Database：PASS，不改 API contract 或 schema。

## 8. Acceptance Criteria

- 新增测试覆盖 PromptVersion missing forged-id / object-oracle 和 subject-name role-confusion 缺口。
- 如新增测试全部通过且不需要生产代码修复，明确记录为测试矩阵补强。
- 如新增测试失败且暴露生产代码缺陷，停止并升级任务到 M。
- focused、adjacent、full backend 验证完成，或清晰说明无法运行的环境限制。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

证据：

- `docs/evidence/EVIDENCE-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
- Focused：`mvn --% -Dtest=PromptVersionControllerTest test`，`13 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`mvn --% -Dtest=PromptVersionControllerTest,SecurityFilterChainTest test`，`20 run, 0 failures, 0 errors, 0 skipped`。
- Full backend：`mvn test`，`572 run, 0 failures, 0 errors, 1 skipped`。

验收：PASS。本切片仅补测试，未修改生产代码、API、DTO、DB schema、依赖、部署或前端；P3-4 父项仍保持 open。
