# RUN-20260609 P3-4-N 下一轮 RBAC 最小 RED 测试矩阵建议

## 1. 任务定位

- 角色：P3-4 测试专家。
- 约束：只读分析；不修改生产代码或测试代码。
- 本次唯一写入：`docs/subagents/runs/RUN-20260609-p3-4-n-next-test.md`。
- 背景：P3-4-M 已完成 Course API / CourseAccessService roles-first overload，`CourseKnowledgeControllerTest` 20/20，adjacent 63/63，full 403/0/0/1。
- 当前剩余 TODO：broader class/course/full RBAC/formal auth。

## 2. Skill Selection Gate

| 项 | 结论 |
|---|---|
| Task type | 测试覆盖分析 / 下一轮最小 RED 矩阵设计 |
| Selected skills | `test-generator`、`security-review`、`object-scope-authorization`、`auth-context-boundary`、`architecture-drift-check` |
| 选择原因 | 本任务核心是 RBAC 回归测试切片选择，需要覆盖对象级授权、Bearer roles-first 边界、架构漂移风险和测试命令范围 |
| Missing skills | 无 |
| GitHub research needed | No。本轮比较的是仓库内既有端点和测试覆盖，不需要外部参考 |
| New project-specific skill | 暂不需要；若后续连续迁移多个 Controller，可沉淀“roles-first RBAC controller test matrix” |

## 3. 候选切片比较

| 候选面 | 当前证据 | RED 强度 | 切片大小 | 推荐度 |
|---|---|---:|---:|---:|
| Evaluation endpoints | `EvaluationSetController` / `EvaluationRunController` 已接入 `CurrentUserService.currentUserId()`，但 `EvaluationSetService` / `EvaluationRunService` 仍用 `"admin"`、`"teacher"`、`teacher_` 字符串推断角色；已有 ControllerTest 和 course-scoped service 逻辑 | 高 | 小 | 最高 |
| PromptVersion | `PromptVersionController` 没有 `CurrentUserService`，API 当前完全无 RBAC；文档曾留下 `promptText` 暴露策略后续项 | 高 | 中 | 第二 |
| RAG KB management | `KnowledgeBaseService` / `PermissionService` 仍是 owner/public/kb permission 权限域，和 course roles-first 不是同一最小模型；KB create/list 夹具少 | 中 | 中 | 后置 |
| legacy CourseAccessService callers | `AssessmentService`、`GradingEvaluationService`、`AnalyticsService`、`LearningWorkflowService`、`DocumentService` 等仍有旧签名调用或间接旧判断 | 高 | 大 | 不适合下一刀 |
| formal OAuth2/JWK/Spring Security | 当前 `DevAuthFilter` 已覆盖 HS256 Bearer、header fallback、prod/staging unauthorized；正式 OAuth2 Resource Server/JWK 需要依赖/配置/安全设计 | 高 | 大 | 不适合 RED 小切片 |

## 4. 推荐下一切片

推荐下一刀：**P3-4-N Evaluation endpoints roles-first RBAC matrix**。

范围只覆盖：

- `POST /api/evaluation-sets`
- `GET /api/evaluation-sets`
- `GET /api/evaluation-sets/{setId}`
- `POST /api/evaluation-runs`
- `GET /api/evaluation-runs/comparison`

核心断言：

- Bearer `ADMIN` 不依赖 `sub=admin`，且忽略 spoofed `X-User-Id`。
- Bearer `TEACHER` 不依赖 `sub=teacher` / `teacher_` 前缀；只要 owns course 即可管理/读取该课程 evaluation set/run。
- Bearer `STUDENT` 即使带 spoofed `X-User-Id: admin` 也不能创建、读取或比较管理型 evaluation 数据。
- Bearer `USER sub=admin` / `USER sub=teacher_1` 不应因 subject 字符串获得 admin/teacher 权限。

为什么这一刀最合适：

- 与 P3-4-M 的缺口同构：认证层已能提供 roles，但业务服务仍按 `currentUserId` 字符串推断角色。
- 测试夹具已存在：`EvaluationSetControllerTest`、`EvaluationRunControllerTest`、`EvaluationSetServiceTest`、`EvaluationRunServiceTest` 可复用创建 set、run、course 的模式。
- 比 PromptVersion 更贴近 class/course/full RBAC，因为 Evaluation Set/Run 已有 `courseId` 与 course teacher scope。
- 比 RAG KB management 更小，因为无需同时定义 KB owner/public/course metadata 的完整权限模型。
- 比 legacy CourseAccessService callers 更可控，不会一刀切入 Assessment/RAG/Learning/ResourceGeneration 多模块。
- 比 formal OAuth2/JWK 更符合当前 P3-4 的“业务 RBAC RED”节奏，不新增依赖、不触碰认证架构。

## 5. 建议新增 RED 测试方法名

建议优先放在 `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java` 与 `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`，共 6 个方法。

1. `evaluationSetCreateUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
2. `evaluationSetCreateAllowsBearerTeacherWhenSubjectOwnsCourseWithoutTeacherPrefix`
3. `evaluationSetListAndDetailUseBearerTeacherRoleForOwnedCourse`
4. `evaluationRunRecordUsesBearerTeacherRoleForOwnedEvaluationSet`
5. `evaluationRunComparisonRejectsBearerStudentWithSpoofedAdminHeader`
6. `evaluationEndpointsDoNotGrantAdminOrTeacherFromBearerUserSubjectName`

可选第 7 个，如果实现方希望覆盖 admin missing-vs-forbidden 语义：

7. `bearerAdminSeesMissingEvaluationSetAsNotFound`

## 6. 当前为何会 RED

### 6.1 `evaluationSetCreateUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`

- 请求：`POST /api/evaluation-sets`
- 身份：`Authorization: Bearer(sub=ops_admin, roles=[ADMIN])` + `X-User-Id: student_1`
- 期望：`200 OK`，`$.code == "OK"`，可创建带 `courseId` 的 evaluation set。
- 当前 RED 原因：`EvaluationSetController.upsert` 只传 `currentUserService.currentUserId()`；`EvaluationSetService.isAdmin(currentUserId)` 只认 `"admin"`。`ops_admin` 会被当作非 admin，且不满足 legacy teacher 字符串规则，预期返回 `403 FORBIDDEN`。

### 6.2 `evaluationSetCreateAllowsBearerTeacherWhenSubjectOwnsCourseWithoutTeacherPrefix`

- 请求：`POST /api/evaluation-sets`
- 身份：`Authorization: Bearer(sub=instructor_1, roles=[TEACHER])`
- 数据：课程 `teacherId=instructor_1`
- 期望：`200 OK`。
- 当前 RED 原因：`EvaluationSetService.assertCanManage` 只把 `"teacher"` 或 `teacher_` 前缀当作 teacher。`instructor_1` 虽然有 Bearer `TEACHER` role 且 owns course，但会被旧字符串逻辑降级。

### 6.3 `evaluationSetListAndDetailUseBearerTeacherRoleForOwnedCourse`

- 请求：`GET /api/evaluation-sets`、`GET /api/evaluation-sets/{setId}`
- 身份：`Authorization: Bearer(sub=instructor_1, roles=[TEACHER])`
- 数据：由 legacy seed 或 service 创建一个 `courseId` 属于 `instructor_1` 的 set。
- 期望：list 包含 own-course set；detail 返回 `200 OK`。
- 当前 RED 原因：`EvaluationSetService.assertCanUseManagementApi` 不读取 roles，只按 legacy userId 判断；`instructor_1` 不满足 `"teacher"` / `teacher_`，进入 `403 FORBIDDEN`。

### 6.4 `evaluationRunRecordUsesBearerTeacherRoleForOwnedEvaluationSet`

- 请求：`POST /api/evaluation-runs`
- 身份：`Authorization: Bearer(sub=instructor_1, roles=[TEACHER])`
- 数据：`evaluationSet.courseId` 属于 `instructor_1`
- 期望：`200 OK`，写入 run metrics。
- 当前 RED 原因：`EvaluationRunController.record` 只传 userId；`EvaluationRunService.loadReadableEvaluationSet` 的 `canRead` 依赖 `isTeacherUser(currentUserId)` 或 course teacher 字符串路径。若 current user 不满足 legacy teacher 规则，own-course teacher role 不能稳定通过。

### 6.5 `evaluationRunComparisonRejectsBearerStudentWithSpoofedAdminHeader`

- 请求：`GET /api/evaluation-runs/comparison`
- 身份：`Authorization: Bearer(sub=student_1, roles=[STUDENT])` + `X-User-Id: admin`
- 期望：`403 FORBIDDEN`，响应不包含 evaluation set id、metric payload、course id 等枚举信息。
- 当前是否 RED：大概率 GREEN，作为防回归负向边界保留。
- 价值：锁定 Bearer 优先于 spoofed header，防止迁移 Evaluation endpoints 时重引入 header-based 提权。

### 6.6 `evaluationEndpointsDoNotGrantAdminOrTeacherFromBearerUserSubjectName`

- 请求：可以拆成两个断言或两个测试；若保持 6 个方法，可在同一方法中覆盖同一行为族。
- 身份 A：`Authorization: Bearer(sub=admin, roles=[USER])`
- 身份 B：`Authorization: Bearer(sub=teacher_1, roles=[USER])`
- 期望：不能创建/list/detail/compare 管理型 evaluation 数据；返回 `403 FORBIDDEN`。
- 当前 RED 原因：现有 `EvaluationSetService.isAdmin("admin")` 和 `isTeacherUser("teacher_1")` 会从 subject 字符串推断权限，导致 `USER sub=admin` 或 `USER sub=teacher_1` 发生 role confusion。

### 6.7 `bearerAdminSeesMissingEvaluationSetAsNotFound`

- 请求：`GET /api/evaluation-sets/evs_missing` 或 comparison with missing set。
- 身份：`Authorization: Bearer(sub=ops_admin, roles=[ADMIN])`
- 期望：`404 NOT_FOUND`。
- 当前 RED 原因：当前 admin 判断只认 `sub=admin`，`ops_admin` 进入 management API gate 时可能先被拒为 `403`，无法表达 admin 可见的 missing 语义。

## 7. Focused / Adjacent / Full 命令

Focused：

```powershell
cd backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
```

Adjacent：

```powershell
cd backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
```

Full：

```powershell
cd backend
mvn test
```

可选编译预检：

```powershell
cd backend
mvn compile -q
```

## 8. 不应测的范围

- 不测 formal OAuth2 Resource Server / JWK / Spring Security filter chain；这是独立认证架构任务。
- 不新增依赖，不引入 `spring-security-oauth2-resource-server` 或 Nimbus/JWK 相关依赖。
- 不测 PromptVersion 的完整 promptText 可见性策略；那应作为单独安全切片。
- 不测 RAG KB management 的 owner/public/kb permission 全矩阵；该模型需要单独定义 KB 与 course scope 的关系。
- 不测全仓 legacy `CourseAccessService` caller 迁移；Assessment、RAG Document、Learning、ResourceGeneration 应按各自业务切片推进。
- 不测前端页面、路由、状态管理。
- 不测数据库 schema、migration、分页性能、排序稳定性。
- 不把 Evaluation metrics 算法正确性放入本轮；已有 `EvaluationRunServiceTest` 覆盖加权聚合、sample count、duplicate metrics 等行为。

## 9. 结论

下一刀最适合切 **Evaluation endpoints roles-first RBAC**。它能用 5-7 个 Controller-level RED 测试暴露当前最典型的残余风险：认证层已经建立 `UserContext.roles()`，但 Evaluation 业务层仍用 `currentUserId` 字符串推断 admin/teacher。该切片足够小，能复用现有测试夹具，并且直接推进 broader class/course/full RBAC，而不会过早进入 formal OAuth2/JWK 或 RAG KB 全权限模型。

