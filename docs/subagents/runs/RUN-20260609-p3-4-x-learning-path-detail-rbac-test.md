# RUN - P3-4-X LearningPath Detail Roles-First RBAC Test

## 1. 任务分类与技能选择

- 任务类型：权限/RBAC 测试方案设计，范围限定为 `GET /api/learning-paths/{pathId}` 详情读取。
- 选用技能：
  - `test-driven-development`：需要制定 RED/GREEN 测试顺序，先用失败测试证明 roles-first 缺口。
  - `object-scope-authorization`：该接口是对象详情读取，必须覆盖 owner、foreign、missing、防枚举语义。
  - `auth-context-boundary`：核心风险是 Bearer JWT role facts、spoofed `X-User-Id`、`USER sub=admin` role-confusion。
- GitHub research：不需要。项目内 P3-4-L 至 P3-4-W 已有同类 roles-first RBAC 测试模式。
- 子代理决策：本报告即测试专家输出；不建议再拆分。影响单一 backend learning 模块，且用户要求只分析。

## 2. 现状证据

- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java:32-35`：`POST /api/learning-paths` 已从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts 并传入 service。
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java:41-42`：`GET /api/learning-paths/{pathId}` 仍只调用 `currentUserService.currentUserId()`，没有传 role facts。
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:207-218`：`getPathForUser(String currentUserId, String pathId)` 读取路径后用本地 `isAdmin(currentUserId)` 决定 admin foreign/missing 语义。
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:1056`：本地 `isAdmin` 只识别 literal `"admin"`，不会识别 Bearer `roles=["ADMIN"]` 的 `sub=ops_admin`。
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java:193`、`:217`：已有 legacy `X-User-Id` owner/foreign/missing GET 覆盖。
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java:539`、`:565`、`:685`：同一测试类已经有 Bearer admin create、Bearer `USER sub=admin` create role-confusion、`jwt(...)` helper，可直接复用。

## 3. 推荐新增/修改测试文件与具体测试名

推荐只修改：

`backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`

新增 6 个 focused integration tests：

1. `learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
   - Arrange：用 legacy owner `X-User-Id: alice` 创建 path。
   - Act：`GET /api/learning-paths/{pathId}`，携带 `Authorization: Bearer(sub=ops_admin, roles=[ADMIN])` 和 spoofed `X-User-Id: bob`。
   - Assert：`200 OK`，`$.code == "OK"`，`$.data.pathId == pathId`，`$.data.learnerId == "alice"`。

2. `learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion`
   - Arrange：创建 `alice` path。
   - Act：Bearer `sub=admin, roles=[USER]` 读取该 foreign path。
   - Assert：`403 FORBIDDEN`，`$.code == "FORBIDDEN"`，`$.data` 不存在，响应 body 不包含 `pathId`。

3. `learningPathDetailBearerAdminMissingPathReturnsNotFound`
   - Act：Bearer `sub=ops_admin, roles=[ADMIN]` 读取 `path_missing_roles_first`。
   - Assert：`404 NOT_FOUND`，`$.code == "NOT_FOUND"`，`$.data` 不存在。

4. `learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden`
   - Act：Bearer `sub=admin, roles=[USER]` 读取 `path_missing_subject_admin`。
   - Assert：`403 FORBIDDEN`，`$.code == "FORBIDDEN"`，`$.data` 不存在，body 不包含 missing id。

5. `learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader`
   - Arrange：创建 `alice` path。
   - Act：Bearer `sub=alice, roles=[USER]` 或 `roles=[STUDENT]`，同时带 spoofed `X-User-Id: bob`。
   - Assert：`200 OK`，返回 `alice` 的 path。
   - 价值：证明 Bearer subject 优先，owner 仍可读，不被 spoofed header 降权。

6. `learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden`
   - 建议拆成两个独立断言测试，避免 mega-test：
     - `learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden`
     - `learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden`
   - 身份：Bearer `sub=bob, roles=[USER]`。
   - Assert：foreign 与 missing 均为 `403 FORBIDDEN`，无 `data`，body 不包含 foreign `pathId` 或 missing id。
   - 若控制测试数量，可保留现有 `learningPathGetDoesNotRevealMissingVersusForeignPathToNonAdmin` 的 legacy header 覆盖，并只新增 Bearer non-owner 版本。

## 4. RED 期望失败点

预期当前 RED：

1. `learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
   - 期望：`200 OK`。
   - 当前大概率：`403 FORBIDDEN`。
   - 原因：valid Bearer 建立 `currentUserId=ops_admin`，controller 只把 userId 传给 service；service 的 `isAdmin("ops_admin") == false`，将 admin 当普通用户处理。

2. `learningPathDetailBearerAdminMissingPathReturnsNotFound`
   - 期望：`404 NOT_FOUND`。
   - 当前大概率：`403 FORBIDDEN`。
   - 原因：missing branch 只有 literal `"admin"` 保留 `NOT_FOUND`；Bearer `ADMIN sub=ops_admin` 被降级为 non-admin，触发安全 `FORBIDDEN`。

3. `learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion`
   - 期望：`403 FORBIDDEN`。
   - 当前大概率：`200 OK`。
   - 原因：Bearer `roles=[USER]` 但 `sub=admin`，`currentUserId` 是 `"admin"`；service 的 `isAdmin("admin") == true`，错误获得 admin foreign read。

4. `learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden`
   - 期望：`403 FORBIDDEN`。
   - 当前大概率：`404 NOT_FOUND`。
   - 原因：`sub=admin` 被 legacy 字符串逻辑误判为 admin，missing path 走 admin 运维可见 `NOT_FOUND`，形成 role-confusion。

预期当前 GREEN 或用于防回归：

- `learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader` 应已通过，因为 Bearer 优先时 `currentUserId=alice`，owner 匹配。
- Bearer `sub=bob, roles=[USER]` foreign/missing safe forbidden 应已通过；保留是为了证明修复 admin roles-first 后不破坏非 admin 防枚举。

## 5. GREEN 实现验收方向

测试驱动的最小 GREEN 应要求：

- `LearningPathController.get(...)` 像 `create(...)` 一样读取 `UserContext currentUser = currentUserService.currentUser()`。
- 新增或重载 `LearningWorkflowService.getPathForUser(String currentUserId, boolean currentUserAdmin, String pathId)`。
- `GET` 详情路径只使用 explicit `currentUserAdmin` 判断 admin 语义，不再通过 `currentUserId == "admin"` 提权。
- 保留旧签名时，旧签名不应作为 HTTP roles-first 主路径；如果必须保留兼容，需有单元/反射测试约束它不被新 controller 使用，或在后续 legacy cleanup 切片处理。

## 6. focused / adjacent / full 命令

Focused RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest#learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion+learningPathDetailBearerAdminMissingPathReturnsNotFound+learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden+learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader+learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden+learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest#resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion+resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound,AgentTraceControllerTest#traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceDetailRejectsBearerUserSubjectAdminRoleConfusion+traceDetailBearerAdminMissingTaskReturnsNotFound test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

如需编译快速检查：

```powershell
cd D:\多元agent\backend
mvn compile -q
```

## 7. 夹具/数据注意事项

- 直接复用 `LearningWorkflowControllerTest.jwt(sub, name, roles)`，不要引入新 JWT helper 或真实 secret。
- 测试类已有 `@SpringBootTest`、`@AutoConfigureMockMvc`、`@ActiveProfiles("test")`、`@Transactional`、`learning-os.auth.jwt-secret=unit-test-secret`，新增测试应保持同类模式。
- 创建 path 优先用现有 `POST /api/learning-paths` + `X-User-Id: alice`，避免手写 entity 造成 nodes/profileSnapshot 缺失或缓存/DB 双路径不一致。
- 为防 stable id 冲突，新增测试里的 `learnerId` / `goalId` 建议唯一，例如 `detail_admin_alice`、`goal_detail_admin`；当前 path id 由 learner + goal 稳定生成。
- 不需要创建 course/enrollment。该 detail RBAC 只验证已存在 path 的 owner/admin 读取与 missing/foreign 语义；用 template goal 能减少夹具噪音。
- 对 forbidden 响应除 `$.data` 不存在外，建议用 `assertThat(responseBody).doesNotContain(pathId)` / `doesNotContain("path_missing...")` 锁定 anti-enumeration。
- Bearer + spoofed header 场景必须同时携带二者，且 spoofed header 要选择会改变结论的值：
  - Admin 正向：Bearer `ops_admin ADMIN` + `X-User-Id: bob`，证明不是 header owner。
  - Owner 正向：Bearer `alice USER/STUDENT` + `X-User-Id: bob`，证明 spoofed header 不覆盖 owner。

## 8. 不建议覆盖的范围

- 不建议在本切片覆盖 `POST /api/learning-paths` create；P3-4-S 已覆盖 direct create roles-first。
- 不建议覆盖 course-bound path planning、Knowledge DAG 排序、mastery remediation、profile snapshot、node recommendation metadata；这些是学习路径业务行为，不是 detail RBAC 缺口。
- 不建议新增 repository/service 纯单元测试替代 HTTP 测试。该缺口发生在认证上下文到 controller/service 的 role facts 传递，MockMvc 集成测试价值更高。
- 不建议覆盖 Spring Security OAuth2/JWK、token 过期/签名失败、staging/prod header-only 策略；这些由 `DevAuthFilterTest` / auth context 切片承担。
- 不建议引入新依赖、新 DB migration、前端测试或 E2E 浏览器测试。
- 不建议把 foreign 与 missing、owner 与 admin 合并为一个 mega-test；每个测试只证明一个行为，便于 RED 定位。

## 9. 结论

最小 RED 矩阵应落在 `LearningWorkflowControllerTest`，以 4 个必红用例证明当前 `GET /api/learning-paths/{pathId}` 仍依赖 `currentUserId == "admin"`：Bearer `ADMIN sub=ops_admin` 被错误拒绝，Bearer `USER sub=admin` 被错误提权。再加 owner/spoofed header 与 non-owner safe forbidden 防回归用例，可完整锁定 P3-4-X detail roles-first RBAC 的 GREEN 验收边界。

本报告只做分析和测试方案设计，未修改源码或测试源码，未运行测试。
