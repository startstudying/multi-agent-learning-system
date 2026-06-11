# TASK-20260611 P3-4 子任务：user detail object-scope RBAC

## 1. Objective

补齐 `GET /api/users/{userId}` 用户详情读取的对象级权限抽样复核：用户详情响应包含 `email`，不能继续作为任意用户可读的公开目录接口。

## 2. Task Type

Bug fix / Security hardening / P3-4 business permission matrix sampling。

## 3. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求所有需求和 TODO 切片走 S/M/L 工作流。 |
| `object-scope-authorization` | 本任务是典型对象详情权限与 missing/foreign 防枚举收口。 |
| `auth-context-boundary` | 需要验证 Bearer role facts、spoofed `X-User-Id`、subject-name role-confusion。 |
| `security-review` | 涉及 OWASP A01 Broken Access Control。 |
| `test-driven-development` | 先补 RED 权限矩阵，再做最小修复。 |

Missing Skills: 无。

GitHub Research Needed: No。现有项目 RBAC 规则和测试模式足够。

New Project-Specific Skill To Create: 不需要。

## 4. Size Classification

- Size: S - Small Slice / Fast Lane
- Reason: 单一后端 user 模块权限收口；不改变 REST path、DTO 字段、DB schema、依赖、前后端合同或 Agent/RAG 编排。
- Required Documents: 本 mini TASK，内嵌 Context Pack。
- Can Skip: PRD / REQ / SPEC / PLAN / standalone Context Pack / standalone Acceptance。
- Upgrade Trigger: 若需要新增用户角色表、组织/班级成员模型、用户搜索/目录产品语义、DTO 拆分或前端联动，则停止并升级为 M。

## 5. Subagent Decision

- Use Subagents: Yes
- Reason: 用户要求专家 subagent 并行开发；本轮已启动安全、测试、架构专家做只读复核。
- Parallelism Level: L1 parallel analysis。
- Implementation Mode: Single Codex integration。
- Note: 专家线程等待超时，主线根据当前代码证据继续推进；若专家后续返回冲突结论，再重新评估。

## 6. Embedded Context Pack

### Related Memory / Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`

### Current Evidence

- `backend/src/main/java/com/learningos/user/api/UserController.java`
  - `GET /api/users/{userId}` 当前直接调用 `userService.get(userId)`。
  - Controller 不读取 `CurrentUserService`，没有 owner/admin 边界。
- `backend/src/main/java/com/learningos/user/dto/UserDtos.java`
  - `UserResponse` 包含 `email`。
- `backend/src/test/java/com/learningos/user/api/UserControllerTest.java`
  - 当前仅覆盖 create + read persisted user by id，没有 foreign / Bearer / spoofed header / role-confusion 测试。

### Files Allowed To Modify

- `backend/src/main/java/com/learningos/user/api/UserController.java`
- `backend/src/main/java/com/learningos/user/application/UserService.java`
- `backend/src/test/java/com/learningos/user/api/UserControllerTest.java`
- `docs/tasks/TASK-20260611-p3-4-user-detail-object-scope-rbac.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-user-detail-object-scope-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/common/auth/**`
- Frontend files
- Agent/RAG/assessment/analytics implementation files

### Test Commands

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=UserControllerTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=UserControllerTest,SecurityFilterChainTest,SecurityJwtAuthenticationTest,CurrentUserServiceTest,DevAuthFilterTest test
```

Full backend if feasible:

```powershell
cd D:\多元agent\backend
mvn test
```

### Current Boundary

- Only harden user detail read.
- `POST /api/users` remains unchanged.
- No new DTO field; `email` remains visible only to authorized reads.
- Non-admin foreign/missing user detail should collapse to safe `FORBIDDEN` without `data` and without echoing the target id.

## 7. Acceptance Criteria

- [ ] Bearer owner can read own user detail even with spoofed `X-User-Id`.
- [ ] Bearer admin can read another existing user even with spoofed `X-User-Id`.
- [ ] Bearer `USER sub=admin` cannot read another user's detail.
- [ ] Non-admin foreign user detail returns safe `FORBIDDEN` without `data` and without leaking target user id/email.
- [ ] Non-admin missing user detail returns safe `FORBIDDEN` without `data` and without leaking missing id.
- [ ] `POST /api/users` create behavior remains compatible.
- [ ] Focused and adjacent verification pass, or limitations are documented.

## 8. Done Definition

- Mini TASK exists before implementation.
- RED/GREEN or focused regression evidence recorded.
- Code changes are limited to allowed files.
- Evidence file exists with verification output.
- Changelog, project/backend/RAG memory, and P3-4 TODO status are updated without marking P3-4 parent complete.
