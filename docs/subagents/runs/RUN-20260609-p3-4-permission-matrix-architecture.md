# P3-4-K 权限渗透测试矩阵架构审查报告

## Summary

P3-4-K 不应做“大而全”的权限重建，而应补齐跨模块权限测试矩阵，用测试证明当前过渡权限架构的核心不变量：

- 认证上下文可信。
- Controller 不写对象归属规则。
- Service 层完成对象级授权。
- 非 admin 防枚举。
- 课程/答题/RAG/资源/trace/analytics 均按 owner/course/enrollment/RBAC scope 收敛。

当前 P3-4 已完成多轮切片，但项目记忆仍明确标记为 `Permission/security minimum hardening = Partial`，后续范围是 broader class/course、正式 OAuth2/JWK/Spring Security 和 broader penetration tests，而不是在 P3-4-K 混入新安全框架或重建 domain/class 模型。

## 1. 权限架构当前核心不变量

1. 认证上下文只负责建立可信 `UserContext`，对象级权限留在业务 Service 层。
   - `DevAuthFilter` 在 Bearer token 存在时先校验 token，失败直接返回 `UNAUTHORIZED`，不会 fallback 到 `X-User-Id`。
   - `CurrentUserService.isAdmin()` / `isTeacherUser()` 优先使用 roles，仅 dev/test 允许旧 userId 字符串推断。

2. Controller 只传当前用户和请求参数，不承载对象归属判断。
   - `CourseController` 只把 `currentUserId` 和 `courseId` 传给 `KnowledgeCatalogService`。
   - `AssessmentController` 只传 `currentUserId`、`learnerId/courseId/page/size` 或对象 id 给 service。

3. 课程 scope 的当前事实源是 `CourseAccessService`：admin 全局、teacher own-course、student active enrollment。
   - 非 admin missing course 被收敛为 `FORBIDDEN`，admin missing 为 `NOT_FOUND`。
   - active enrollment 是 student course 授权源。

4. RAG KB 权限与课程元数据权限是两个边界。
   - `PermissionService.requireReadableKbIds` 对 requested `kbIds` 做 strict deny，只要有不可读 KB 就返回 `FORBIDDEN`。
   - `DocumentService.upload` 先校验 KB 写权限，再在存储、document 持久化、index task 创建前校验 `courseId/chapterId` scope。

5. 非 admin 对象详情必须防枚举：missing 与 foreign 同类响应，且无 `data`。
   - 越权响应不得包含对象 id、父资源 id、标题、内容、traceId。
   - 已完成 learning path、resource task、agent trace、document、reindex、index task 详情防枚举切片。

## 2. 适合通过 controller/service 测试矩阵证明的不变量

P3-4-K 的最小测试矩阵应覆盖“跨域不变量”，不是重复每个端点所有排列。

1. **Auth Context 矩阵**
   - dev/test 无 Bearer 时允许 `X-User-Id` fallback。
   - valid Bearer 覆盖 spoofed `X-User-Id`。
   - invalid Bearer 不 fallback。
   - production/staging 无 Bearer 返回 `UNAUTHORIZED`。
   - P3-4-K 应补 controller 路径，证明 token roles 能驱动 `currentUserService.isAdmin()` / `isTeacherUser()`。

2. **Course / Knowledge Graph 矩阵**
   - admin 读全部，teacher 只读 own course，student 只读 active enrollment course。
   - 补同一矩阵断言 course detail 与 knowledge graph 行为一致，避免一个入口绕过 `CourseAccessService`。

3. **Assessment answer / wrong-question 矩阵**
   - student owner-only；teacher own-course + active enrolled learner；admin global。
   - list 必须有 bounded pagination 和 summary DTO。
   - detail 不暴露 request/replay/raw payload 字段。
   - P3-4-K 应补跨端点一致性。

4. **RAG Query / KB 矩阵**
   - mixed `kbIds` 必须 strict deny，而不是过滤后继续回答。
   - `POST /api/rag/query` 与 `GET /api/rag/query` 都应复用 strict 入口。

5. **RAG Document upload / reindex / index task 矩阵**
   - KB write 权限不足不能上传。
   - public KB 可读但不可由他人写。
   - course/chapter metadata 必须在副作用前失败。
   - 非 admin document/index missing/foreign 同为 `FORBIDDEN`。

6. **Resource generation / review / trace 矩阵**
   - resource task create 是 learner owner-only。
   - course goal 只在 existing course 时要求 enrollment。
   - detail/trace 对非 owner/admin 防枚举。
   - review list/decision 是 admin global、teacher own-course、student denied。

7. **Analytics 矩阵**
   - overview/ops alerts admin-only。
   - student summary owner 或 course-scoped teacher/admin。
   - token-budget governance admin-only。
   - 应特别补 token-role admin 访问 admin-only analytics 的测试，暴露仍用 `admin` 字符串判断的过渡风险。

## 3. 不应混入 P3-4-K 的后续范围

- 正式 OAuth2/JWK/Spring Security 资源服务器。
- broader class/course domain 建模。
- 新依赖。
- 新 schema。
- 新前端页面。
- 生产权限大重构。

若测试暴露生产代码缺陷，应开后续最小修复切片并更新 Context Pack，不在测试矩阵切片中无边界扩大生产代码修改范围。

## 4. Context Pack 允许修改文件建议

建议允许修改：

- `docs/product/PRD-20260609-p3-4-permission-matrix.md`
- `docs/requirements/REQ-20260609-p3-4-permission-matrix.md`
- `docs/specs/SPEC-20260609-p3-4-permission-matrix.md`
- `docs/plans/PLAN-20260609-p3-4-permission-matrix.md`
- `docs/tasks/TASK-20260609-p3-4-permission-matrix.md`
- `docs/context/CONTEXT-20260609-p3-4-permission-matrix.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-permission-matrix.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-permission-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `backend/src/test/java/com/learningos/security/api/PermissionMatrixControllerTest.java`
- 必要时少量相邻测试类：`DevAuthFilterTest`、`AnalyticsControllerTest`、`CourseKnowledgeControllerTest`、`AssessmentControllerTest`、`DocumentControllerTest`、`ResourceGenerationControllerTest`、`ResourceReviewControllerTest`

建议不允许修改：

- `backend/src/main/java/**`，除非 P3-4-K 测试先暴露明确的生产缺陷，并由后续 Context Pack 单独授权。
- `backend/pom.xml`。
- `backend/src/main/resources/db/migration/**`。
- `frontend/**`。
- `docs/superpowers/**`。

## 5. 风险和验收标准

### 风险

1. 角色判断仍存在过渡字符串身份路径。
2. 多处 list 仍有内存过滤或 repository scope 不完全下沉。
3. broader class/course 权限模型尚未完成。

### 验收标准

1. Auth context matrix 通过。
2. Controller/service boundary 通过。
3. Non-admin anti-enumeration 通过。
4. Role/course/enrollment matrix 通过。
5. RAG / document boundary 通过。
6. Analytics / trace / resource boundary 通过。
7. 范围控制通过：不新增依赖、不改 schema、不改前端、不引入 OAuth2/JWK/Spring Security、不宣称 broader class/course 完成。

## Root Cause

P3-4-K 的根因不是“权限逻辑完全缺失”，而是当前权限体系处在过渡状态：多个高风险对象边界已经通过 P3-4-A 到 P3-4-J 收口，但仍分散在 `CurrentUserService`、`CourseAccessService`、RAG `PermissionService` 以及各业务 service 的字符串身份判断和 course/enrollment scope 中。

因此 P3-4-K 的最小价值是用 controller/service 测试矩阵证明这些过渡不变量在跨模块 HTTP 入口上一致成立，并暴露仍依赖字符串身份或未统一 role 判断的入口，而不是扩大为新安全框架或新权限领域模型。

## Recommendations

1. 补齐跨模块 controller 权限矩阵测试，覆盖 auth、course、assessment、RAG、document、resource/review/trace、analytics 的最小不变量。
2. 增加 token-role 驱动的 controller 测试，检查 admin/teacher role 是否真的被业务入口识别。
3. 在 evidence/acceptance 中明确 P3-4-K 只验收 transitional scope，不验收 formal OAuth2/JWK/Spring Security 或 broader class/course model。
4. 若 P3-4-K 暴露生产缺陷，另开最小修复切片并更新 Context Pack。
