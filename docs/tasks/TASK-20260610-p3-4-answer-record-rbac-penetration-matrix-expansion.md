# TASK-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion

## 1. 任务名称

P3-4 子任务：Answer record RBAC penetration matrix expansion

## 2. 任务类型

权限 / 安全测试矩阵扩展。

## 3. 目标

在不改变生产代码和 API 合同的前提下，补齐 `answer record` 与 `wrong-question` 读取路径的高价值 RBAC 渗透测试，固定以下行为：

- Bearer `ADMIN` 角色优先于伪造的 `X-User-Id`。
- Bearer `TEACHER` 可以在无 `teacher_` subject 前缀时访问自己课程内的有效班级数据。
- Bearer `USER sub=admin` / `USER sub=teacher_*` 不得获得 admin / teacher 权限语义。
- Bearer 普通学生身份必须忽略伪造的 `X-User-Id`，只能访问自己的答题记录和错题记录。
- 教师详情读取必须同时满足课程归属和 active enrollment，不能只凭题目/知识点属于自己的课程放行。
- 越权响应不得泄露 answer/wrong-question id 或内部 payload。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有需求走 S/M/L 受控工作流。 |
| `security-review` | 本任务验证 Broken Access Control、角色混淆和 header spoofing。 |
| `test-driven-development` | 本任务以回归测试为交付物，先补测试矩阵，再判断是否需要生产修复。 |
| `verification-before-completion` | 完成前必须提供 focused / adjacent / full 验证证据。 |
| `object-scope-authorization` | 匹配 answer/wrong-question 对象级授权、IDOR 和防枚举规则。 |
| `auth-context-boundary` | 匹配 Bearer roles-first、`X-User-Id` spoofing 和 subject-name role-confusion 规则。 |

缺失技能：无。

GitHub research：不需要。该任务是项目内 RBAC 测试矩阵补强，不新增依赖/API，也不需要外部实现参考。

新项目技能：暂不创建；若收尾复盘发现可复用 RBAC 矩阵模板，再另行抽取。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 预计只影响一个测试文件：`AssessmentControllerTest`。
- 不改变 REST API 路径、请求 DTO、响应 DTO、数据库 schema、依赖、部署拓扑或前后端合同。
- 专家只读分析未发现必须先修的生产代码 RBAC 漏洞。
- 若新增测试暴露真实生产缺陷，再升级为 M 并补齐 REQ / SPEC / PLAN / TASK / CONTEXT。

可跳过：

- PRD
- REQ
- SPEC
- PLAN
- standalone Context Pack

必需文档：

- 本 mini TASK，内嵌 Context Pack。
- 完成后创建 combined Evidence/Acceptance。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求使用专家 subagent 并行开发；本任务涉及权限安全和测试矩阵，适合 L1 并行分析。

Parallelism Level：L1 Parallel Analysis。

已使用专家：

- Security Reviewer：只读审查 RBAC 授权路径和缺口。
- Test Engineer：只读审查现有测试矩阵和推荐最小补洞集。

Implementation Mode：主 Codex 单文件集成实现。

不使用并行实现的原因：

- 目标代码修改集中在同一个测试文件。
- 多个 worker 同时改 `AssessmentControllerTest` 会增加冲突和重复风险。

专家报告：

- `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-test.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本切片只补 answer/wrong-question RBAC 渗透测试。除非测试暴露 RED 生产缺陷，否则不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `.cursor/rules/00-project-memory-rule.mdc`
- `.cursor/rules/01-skill-selection-rule.mdc`
- `.cursor/rules/02-subagent-parallel-rule.mdc`
- `.cursor/rules/10-context-pack-rule.mdc`
- `.cursor/rules/11-execution-rule.mdc`
- `.cursor/rules/12-acceptance-rule.mdc`
- `.cursor/rules/13-security-dependency-rule.mdc`
- `.cursor/rules/14-architecture-drift-rule.mdc`
- `.cursor/rules/16-backend-rule.mdc`
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

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md`
- `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-test.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 7.4 禁止修改文件

除升级为 M 并更新上下文包外，本切片不得修改：

- `backend/src/main/**`
- `backend/pom.xml`
- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- API / schema / dependency / deployment 配置

### 7.5 待补测试清单

优先补以下高价值路径：

1. answer list：Bearer admin + spoofed `X-User-Id` 成功。
2. answer detail：Bearer `USER sub=teacher_*` role-confusion 拒绝。
3. answer list/detail：Bearer student + spoofed `X-User-Id` 只能访问本人。
4. wrong-question detail：Bearer admin + spoofed `X-User-Id` 成功。
5. wrong-question detail：Bearer `USER sub=admin` role-confusion 拒绝。
6. wrong-question detail：Bearer student + spoofed `X-User-Id` 只能访问本人。
7. wrong-question list：Bearer teacher no-prefix 成功。
8. wrong-question list：Bearer `USER sub=admin` / `USER sub=teacher_*` role-confusion 拒绝。
9. teacher detail：own course 但 learner 非 ACTIVE enrollment 时拒绝。

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,CourseAccessServiceTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

- Backend layering：PASS，本切片只补测试，不改变 Controller / Service / Repository 分层。
- Frontend rules：N/A，不改前端。
- Agent / RAG rules：N/A，不改 Agent/RAG 运行时。
- Security：PASS，不新增 secret / 依赖；权限仍由后端代码验证。
- API / Database：PASS，不改 API contract 或 schema。

## 8. Acceptance Criteria

- 新增测试覆盖专家指出的 answer/wrong-question 高价值矩阵缺口。
- 如果新增测试全部通过且不需要生产代码修复，明确记录为测试矩阵补强。
- 如果新增测试失败且暴露生产代码缺陷，停止并升级任务到 M。
- focused、adjacent、full backend 验证完成，或清楚说明无法运行的环境限制。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

完成内容：

- 已归档 Security Reviewer 与 Test Engineer 专家报告。
- 已在 `AssessmentControllerTest` 中补齐 answer/wrong-question Bearer、spoofed header、subject-name role-confusion、student owner-only、teacher active enrollment detail 等矩阵。
- 未修改生产代码；新增测试首次 focused 运行即通过，说明当前生产授权路径已满足本切片期望。
- 已完成 focused、adjacent、full backend 验证。

证据：

- `docs/evidence/EVIDENCE-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md`
