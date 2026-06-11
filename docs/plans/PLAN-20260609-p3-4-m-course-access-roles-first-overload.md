# PLAN-20260609 P3-4-M Course API / CourseAccessService roles-first overload

## 1. Skill Selection Report

### Task Type

Security hardening / backend authorization / Course API role-aware refactor.

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 Spec-first 闭环推进。 |
| `security-review` | 本切片修改授权行为，需要覆盖 IDOR、spoofing、anti-enumeration。 |
| `object-scope-authorization` | 定义 course owner、teacher/admin/student、enrollment 对象级授权。 |
| `auth-context-boundary` | Bearer roles 与 `X-User-Id` 兼容边界。 |
| `spring-boot-architecture` | 保持 Controller -> Service -> Repository 分层。 |
| `api-contract-design` | 确认不改 API path/DTO/error envelope。 |
| `test-driven-development` | 必须先写 RED，再最小 GREEN。 |
| `verification-before-completion` | 完成前必须有 fresh verification evidence。 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片沿用项目既有 auth context 和 course access service，不引入外部框架或依赖。

### New Project-Specific Skill To Create

暂不创建。现有 `object-scope-authorization` 与 `auth-context-boundary` 足够覆盖。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 用户要求使用专家 subagent；任务涉及 security、backend API、test matrix，且影响 Controller、Application Service、集中授权服务。

Parallelism Level: L1 Parallel Analysis / Design

Selected Subagents:

- Security & Quality：`docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-security.md`
- Backend Expert：`docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-backend.md`
- Test Engineer：`docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-test.md`

Implementation Mode: Single Codex implementation。原因：生产改动集中在 knowledge API/service，文件重叠明显，并行写入冲突风险高。

## 3. Implementation Steps

1. [x] 读取 memory、skills、architecture baseline、TODO。
2. [x] 子代理并行分析 security/backend/test。
3. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
4. [x] RED：新增 `CourseKnowledgeControllerTest` Bearer roles-first 测试。
5. [x] GREEN：最小修改 `CourseController` / `KnowledgePointController` / `KnowledgeCatalogService` / `CourseAccessService`。
6. [x] Focused verification。
7. [x] Adjacent regression。
8. [x] Full backend Maven verification。
9. [x] Evidence / Acceptance / Retrospective。
10. [x] 更新 Changelog / Memory / backend TODO。

## 4. Allowed Files

Production:

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/api/KnowledgePointController.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`

Tests:

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`

Docs:

- `docs/product/PRD-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/requirements/REQ-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/plans/PLAN-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/tasks/TASK-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/context/CONTEXT-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/retrospectives/RETRO-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-backend.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Forbidden Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/OCR/vector/model provider files。

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
mvn test
```

## 7. Risks

| Risk | Mitigation |
|---|---|
| 误把 `TEACHER` role 当作全课程读写权限 | 仍要求 `currentUserId == Course.teacherId`。 |
| 破坏 legacy test/dev header 行为 | 旧签名保留；Controller 使用 `CurrentUserService` 得到兼容后的 role facts。 |
| 扩大到全局 RBAC 导致回归面失控 | 本切片只迁移 Course API / KnowledgeCatalog 主路径。 |
| P3-4 被误标完成 | 文档明确 broader class/course、full RBAC、formal OAuth2/JWK 仍 open。 |

## 8. 实施结果

已完成。验证结果写入 Evidence / Acceptance，后续仍保留 broader class/course、full RBAC 与 formal OAuth2/JWK/Spring Security 作为独立切片。
