# PLAN-20260609 P3-4-K 权限渗透测试矩阵补齐

## 1. Skill Selection Report

### Task Type

Security hardening / test matrix / backend authorization verification.

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 Spec-first 流程推进 P3-4-K。 |
| `security-review` | 覆盖 RBAC、IDOR、防枚举、敏感响应泄露。 |
| `object-scope-authorization` | 定义 owner/course/enrollment/admin/teacher/student 对象级授权矩阵。 |
| `auth-context-boundary` | 验证 Bearer JWT、`X-User-Id` spoofing、prod/staging fallback。 |
| `test-driven-development` | 权限矩阵必须先 RED，再最小 GREEN。 |
| `verification-before-completion` | 完成前必须有 fresh Maven evidence。 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片使用项目已有安全规则和测试风格，不引入外部依赖或框架。

### New Project-Specific Skill To Create

暂不创建。完成后在 retrospective 判断是否抽取“权限矩阵测试模板”。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 用户要求使用专家 subagent 并行开发；本切片涉及 security、testing、architecture 三个独立分析维度。

Parallelism Level: L1 Parallel Analysis

Selected Subagents:

- Security & Quality：`RUN-20260609-p3-4-permission-matrix-security.md`，第一个专家按用户要求使用 `gpt-5.5`。
- Test Engineer：`RUN-20260609-p3-4-permission-matrix-test.md`。
- Backend Architecture Reviewer：`RUN-20260609-p3-4-permission-matrix-architecture.md`。

Implementation Mode: Single Codex implementation。原因：测试矩阵会集中修改 test fixtures，生产代码仅在 RED 后最小修复，不适合多 agent 并行写同一测试上下文。

## 3. Implementation Steps

1. [x] 读取项目 memory、skills、architecture baseline。
2. [x] 启动并收敛三份专家分析报告。
3. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
4. [x] RED：新增 P3-4-K 权限矩阵测试，优先覆盖 Bearer roles、student write deny、teacher foreign deny、student foreign learner deny、anti-enumeration、admin missing semantics。
5. [x] GREEN：RED 暴露 analytics admin-only 入口仍依赖 `"admin"` 字符串身份判断，已最小修复为 role-derived admin gate。
6. [x] 运行 focused tests。
7. [x] 运行 adjacent regression。
8. [x] 运行 full backend Maven tests。
9. [x] 创建 Evidence / Acceptance / Retrospective。
10. [x] 更新 Changelog / Memory / backend architecture TODO。

## 4. Allowed Files

- `backend/src/test/java/com/learningos/security/api/PermissionMatrixControllerTest.java`
- `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `docs/product/PRD-20260609-p3-4-permission-matrix.md`
- `docs/requirements/REQ-20260609-p3-4-permission-matrix.md`
- `docs/specs/SPEC-20260609-p3-4-permission-matrix.md`
- `docs/plans/PLAN-20260609-p3-4-permission-matrix.md`
- `docs/tasks/TASK-20260609-p3-4-permission-matrix.md`
- `docs/context/CONTEXT-20260609-p3-4-permission-matrix.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-permission-matrix.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-permission-matrix.md`
- `docs/retrospectives/RETRO-20260609-p3-4-permission-matrix.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-*.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

Conditional allowed only if RED proves a production defect:

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`

## 5. Forbidden Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- unrelated parser/model/vector implementation files
- production auth/security/business files not explicitly added after RED evidence

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=DevAuthFilterTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DocumentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest test
mvn test
```

## 7. Risks

| Risk | Mitigation |
|---|---|
| Bearer roles Controller 级测试暴露旧字符串身份判断 | 先记录 RED，再最小改相关 Controller 到 `CurrentUserService.isAdmin()` / role helper。 |
| 新 matrix test 过大、fixture 脆弱 | 每个攻击模型只覆盖最关键端点；业务细节仍由模块测试覆盖。 |
| teacher class summary missing vs foreign 现有语义可能不一致 | 本切片只记录为后续安全修复，不在测试-only 矩阵中强行改业务语义。 |
| P3-4 TODO 被误标为完全完成 | 文档明确 P3-4-K 只完成 penetration matrix transitional scope；broader class/course 和 formal OAuth2/JWK/Spring Security 仍保留。 |

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 默认测试-only；若修复则 Controller 只用 role helper，不写对象级授权。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；只验证已有边界。 |
| Security | PASS | 权限在后端代码，测试覆盖 anti-enumeration。 |
| API / Database | PASS | 不改 API path，不改 schema。 |

## 9. Completion Summary

- P3-4-K 当前 transitional permission matrix 已完成。
- 新增测试先 RED，证明 analytics admin-only 入口没有使用 Bearer roles-first admin gate。
- 已最小修复 `AnalyticsController` / `AnalyticsService`，改为 `CurrentUserService.isAdmin()` 派生的 admin 判断。
- 未新增依赖、schema、API path、前端或正式 OAuth2/JWK/Spring Security。
- Focused / adjacent / full backend Maven verification 均通过。

验证结果：

```text
Focused:  Tests run: 65,  Failures: 0, Errors: 0, Skipped: 0
Adjacent: Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
Full:     Tests run: 367, Failures: 0, Errors: 0, Skipped: 1
```
