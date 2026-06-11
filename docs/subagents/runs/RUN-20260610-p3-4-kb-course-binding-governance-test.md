# P3-4 子任务：kb-course-binding-governance 并行测试规划

## 1. Skill Selection Gate

| 项目 | 结论 |
|---|---|
| Task type | 并行测试规划 / 覆盖缺口分析 / TDD RED 切入点设计 |
| Selected skills | `test-generator`, `rag-project-review`, `object-scope-authorization`, `database-design`, `architecture-drift-check` |
| Why | 本任务覆盖 RAG KB 与 Course binding 的 schema、生命周期、权限、查询防越权；需要沿用现有 JUnit/MockMvc/SpringBootTest/Flyway smoke 测试形态，并把授权断言放在 service/controller/query 层 |
| Missing skills | 无。现有项目技能足够；不需要 GitHub research |
| GitHub research needed | No |
| New project-specific skill | 暂不需要；若后续形成稳定 KB-course binding 治理模式，可沉淀 `kb-course-binding-governance` |

## 2. 只读检查摘要

已重点检查：

- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `scripts/mysql-migration-smoke.ps1`
- 相关生产代码：`PermissionService`, `DocumentService`, `RagQueryService`, `CourseAccessService` 的授权调用面。

当前测试结构：

- 后端主路径使用 JUnit 5 + SpringBootTest + MockMvc + H2 `ddl-auto=create-drop`，Flyway 在 test profile 中关闭。
- RAG 管理面已有 KB owner/public/explicit permission/admin role 测试。
- `DocumentControllerTest` 已覆盖 document upload 的 `courseId/chapterId` metadata scope、teacher/admin/student 行为、requestId payload conflict、无副作用断言。
- `RagQueryServiceTest` 已覆盖 strict `kbIds` 读权限、roles-first admin query、subject-name role-confusion deny、混合 allowed/forbidden KB 时不写 `kb_query_log/source_citation`。
- `SchemaConvergenceMigrationTest` 是静态 SQL 内容断言；`MysqlMigrationSmokeTest` 是 opt-in MySQL 8 实迁移 smoke，目前最新版本常量为 `19` / `19`。

关键缺口：

- 目前没有独立 KB-Course binding schema，也没有 binding lifecycle 状态机测试。
- `PermissionService` 只按 KB owner/public/explicit permission/admin 计算 KB 读写；没有按 CourseAccess / enrollment / teacher-owned-course 计算 course-bound KB 可读写。
- `RagQueryService` 当前只接收 `requestedKbIds`，在 retrieval 前按 KB 权限过滤；没有 course-bound KB 查询时必须通过 course access 的测试。
- `DocumentService` 仅在 document upload metadata 上校验 `courseId/chapterId`；没有 KB 级绑定创建、停用、解绑后阻断查询的生命周期测试。

## 3. 最小 RED->GREEN 测试切入点

建议按“schema -> service -> controller -> query”递进，不先铺大矩阵。

### RED 1：migration/schema 先失败

新增静态迁移测试，证明需要一个独立绑定表和关键约束：

- `SchemaConvergenceMigrationTest#v20MigrationAddsKbCourseBindingGovernanceTable`

期望断言：

- 存在 `V20__kb_course_binding_governance.sql`。
- 包含表 `kb_course_binding`。
- 必备列：`id`, `kb_id`, `course_id`, `binding_status`, `created_by`, `created_at`, `updated_at`, `deleted_at`。
- 关键约束/索引：`uk_kb_course_binding_kb_course_active` 或等价唯一活跃绑定约束，`idx_kb_course_binding_course_status`, `idx_kb_course_binding_kb_status`。
- FK：绑定到 `kb_knowledge_base(id)` 与 `course(id)`，如果当前 course 表名不是 `course`，以真实表名为准。

这是最小首个 RED，因为当前迁移最高到 V19，必然失败且不依赖业务实现。

### RED 2：PermissionService course-bound read 行为失败

新增服务测试，先验证最核心防越权语义：

- `PermissionServiceTest#allowsActiveEnrolledStudentToReadCourseBoundPrivateKnowledgeBase`
- `PermissionServiceTest#deniesDroppedStudentFromReadingCourseBoundPrivateKnowledgeBase`

期望：

- 私有 KB 不属于学生，且没有 `kb_permission` 显式授权。
- 当 KB 与 course 存在 active binding，且学生对 course 有 ACTIVE enrollment 时，`canReadKnowledgeBase(student, false, false, kbId)` 为 `true`。
- 同一学生 enrollment 为 `DROPPED` 或无 enrollment 时为 `false`。

这是最小 GREEN 实现的服务合约：`PermissionService` 需要接入 binding repository + `CourseAccessService` 或等价课程访问事实。

### RED 3：query 防越权无副作用

新增查询服务测试，证明 binding 参与 retrieval 前权限过滤：

- `RagQueryServiceTest#studentCanQueryActiveCourseBoundKnowledgeBaseWhenActivelyEnrolled`
- `RagQueryServiceTest#droppedStudentCannotQueryCourseBoundKnowledgeBaseOrPersistArtifacts`

期望：

- ACTIVE enrolled student 查询 course-bound private KB 可拿到 citation。
- DROPPED student 查询同一 KB 返回 `FORBIDDEN`。
- 失败路径 `queryLogRepository.count()` 与 `sourceCitationRepository.count()` 都保持 0。

这是最关键的安全 RED：防止实现只在 controller/list 层做绑定，而 retrieval runtime 仍可被直接 `kbIds` 越权。

### RED 4：controller/lifecycle 行为

在 service RED 后再加 HTTP 层最小集：

- teacher/admin 可创建 binding。
- foreign teacher/student 不可创建 binding，且响应不泄露 foreign `courseId/kbId`。
- deactivate 后 query 失效。

优先通过 `KnowledgeBaseControllerTest` 新增绑定管理端点测试，避免把生命周期混入 `DocumentControllerTest` 的 upload metadata scope。

## 4. 必须新增/修改的测试类和用例名建议

### 必须修改：`SchemaConvergenceMigrationTest`

- `v20MigrationAddsKbCourseBindingGovernanceTable`

原因：schema 是本子任务的第一验收面；现有 migration smoke 只到 V19。

### 必须修改：`MysqlMigrationSmokeTest`

- 更新 `LATEST_MIGRATION_VERSION = "20"`。
- 更新 `LATEST_MIGRATION_COUNT = 20`。
- 在 `verifyCurrentMigrationObjects` 增加：
  - `tableExists(connection, "kb_course_binding")`
  - `columnExists(..., "kb_id" / "course_id" / "binding_status" / "deleted_at")`
  - `indexExists(..., "idx_kb_course_binding_course_status")`
  - `indexExists(..., "idx_kb_course_binding_kb_status")`
  - `foreignKeyExists(..., "fk_kb_course_binding_kb")`
  - `foreignKeyExists(..., "fk_kb_course_binding_course")`

原因：静态 SQL 不能证明 MySQL 8 方言、FK、索引真实可迁移。

### 必须修改：`PermissionServiceTest`

建议用例：

- `allowsTeacherToReadAndWriteOwnCourseBoundKnowledgeBase`
- `allowsActiveEnrolledStudentToReadCourseBoundPrivateKnowledgeBase`
- `deniesDroppedStudentFromReadingCourseBoundPrivateKnowledgeBase`
- `deniesForeignTeacherFromReadingCourseBoundPrivateKnowledgeBase`
- `ignoresInactiveCourseBindingWhenAuthorizingKnowledgeBaseRead`
- `adminRoleCanReadAndWriteCourseBoundKnowledgeBaseWithoutSubjectNameInference`
- `literalTeacherSubjectDoesNotGainCourseBoundKnowledgeBaseAccess`

每个测试只断言一个行为。优先前两到三个作为最小 RED；其余可在 GREEN 后补齐矩阵。

### 必须修改：`KnowledgeBaseControllerTest`

如果实现新增 KB binding 管理端点，建议用例：

- `teacherCanBindOwnPrivateKnowledgeBaseToOwnedCourse`
- `teacherCannotBindKnowledgeBaseToForeignCourse`
- `teacherCannotBindForeignKnowledgeBaseToOwnedCourse`
- `studentCannotBindKnowledgeBaseToCourse`
- `bearerAdminCanBindForeignKnowledgeBaseToExistingCourseDespiteSpoofedHeader`
- `bearerUserSubjectAdminCannotBindKnowledgeBaseToCourse`
- `deactivateBindingReturnsOkAndHidesKnowledgeBaseFromCourseScopedReaders`
- `nonAdminCannotDistinguishMissingBindingFromForeignBinding`

若 endpoint 放在独立 controller，建议新增 `KnowledgeBaseCourseBindingControllerTest`，不要继续扩大 `KnowledgeBaseControllerTest`。

### 必须修改：`RagQueryServiceTest`

建议用例：

- `studentCanQueryActiveCourseBoundKnowledgeBaseWhenActivelyEnrolled`
- `droppedStudentCannotQueryCourseBoundKnowledgeBaseOrPersistArtifacts`
- `studentCannotQueryInactiveCourseBoundKnowledgeBaseOrPersistArtifacts`
- `rejectsMixedCourseBoundAllowedAndForbiddenKnowledgeBasesWithoutPersistingArtifacts`
- `teacherCanQueryOwnCourseBoundKnowledgeBaseWithoutKbOwnerPermission`
- `foreignTeacherCannotQueryCourseBoundKnowledgeBaseOrPersistArtifacts`
- `bearerUserSubjectTeacherPrefixCannotQueryCourseBoundKnowledgeBase`

原因：query runtime 是最终防越权点，必须证明 binding access 在 retrieval/query-log/citation 前执行。

### 建议修改：`DocumentControllerTest`

已有 upload metadata scope 覆盖较多，不建议把所有 binding lifecycle 都塞进这里。只补一个交叉行为即可：

- `uploadingDocumentToCourseBoundKnowledgeBaseRequiresBindingCourseManageScope`

或如果设计要求“绑定 KB 后 document metadata courseId 必须等于绑定 courseId”：

- `rejectsDocumentUploadWhenCourseMetadataConflictsWithActiveKnowledgeBaseBinding`

原因：DocumentControllerTest 已经验证 course/chapter metadata，新增测试只覆盖 KB-level binding 与 document metadata 的交界。

### 建议修改：`CourseKnowledgeControllerTest`

只在产品设计要求 course graph response 暴露绑定 KB 时修改。否则不应为纯绑定治理改它。

可选用例：

- `courseKnowledgeGraphReturnsOnlyReadableBoundKnowledgeBases`
- `courseKnowledgeGraphDoesNotExposeForeignBoundKnowledgeBaseIdsToStudent`

## 5. 测试 fixture 数据建议

建议统一命名，避免 subject-name role-confusion 与角色事实混淆：

| Fixture | 建议值 | 用途 |
|---|---|---|
| teacher owner | `instructor_1` | Bearer `TEACHER`，不依赖 `teacher_` 前缀 |
| foreign teacher | `instructor_2` | 验证 own-course scope |
| legacy subject teacher | `teacher_1` + role `USER` | 验证 subject-name 不授予 teacher |
| admin | `ops_admin` + role `ADMIN` | 验证 roles-first admin |
| legacy subject admin | `admin` + role `USER` | 验证 subject-name 不授予 admin |
| active student | `student_active` | 有 ACTIVE enrollment |
| dropped student | `student_dropped` | 有 DROPPED enrollment |
| never enrolled student | `student_none` | 无 enrollment |
| course A | `course_binding_a` 或由 API 创建 | instructor_1 own course |
| course B | `course_binding_b` | instructor_2 own course |
| private KB | `kb_course_private` | owner 不是 student，visibility PRIVATE |
| public KB | `kb_public_control` | 控制 public read 不被 binding 误伤 |
| active binding | `kb_course_binding(kb, course, ACTIVE)` | 核心允许路径 |
| inactive binding | `kb_course_binding(kb, course, INACTIVE/DELETED)` | 生命周期阻断 |

数据构造原则：

- service 测试直接 repository seed，速度快且便于制造 binding 状态。
- controller 测试优先通过 API 创建 course/KB，再调用 binding endpoint。
- query 测试直接 seed `KnowledgeBase`, binding, `CourseEnrollment`, `KbDocument`, `KbDocChunk`，沿用 `RagQueryServiceTest#seedIndexedChunk` 风格。
- 所有拒绝路径都断言无副作用：document/index/queryLog/sourceCitation/binding count 不变化。
- 所有非 admin missing/foreign 路径都断言响应 body 不包含目标 `kbId/courseId/bindingId`。

## 6. focused / adjacent / full test 命令

### Focused RED/GREEN

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SchemaConvergenceMigrationTest#v20MigrationAddsKbCourseBindingGovernanceTable test
mvn --% -Dtest=PermissionServiceTest test
mvn --% -Dtest=RagQueryServiceTest#studentCanQueryActiveCourseBoundKnowledgeBaseWhenActivelyEnrolled+droppedStudentCannotQueryCourseBoundKnowledgeBaseOrPersistArtifacts test
```

如果新增独立 controller：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseCourseBindingControllerTest test
```

如果复用现有 controller test：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest test
```

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,RagQueryServiceTest,PermissionServiceTest,CourseKnowledgeControllerTest,SchemaConvergenceMigrationTest test
```

如果 binding 影响 Orchestrator RAG_QA runtime，应追加：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,OrchestratorWorkflowControllerTest,RagQueryServiceTest test
```

### Full

```powershell
cd D:\多元agent\backend
mvn test
```

### MySQL Flyway smoke

默认 Maven 会跳过 `MysqlMigrationSmokeTest`，schema 变更必须运行 opt-in smoke：

```powershell
cd D:\多元agent
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1
```

本机 3306 占用时：

```powershell
cd D:\多元agent
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

## 7. 哪些行为不应测试或留后续

本子任务不建议测试：

- 不测试 parser/OCR/chunking/vector/reranker 的质量；只需要 seed chunk 证明权限过滤发生在 retrieval 前。
- 不测试真实 LLM 答案质量；`RagQueryServiceTest` 当前 buildGroundedAnswer 是可控本地逻辑。
- 不测试 frontend 展示；本任务是 backend schema/lifecycle/governance。
- 不测试 formal OAuth2/JWK 第三方 IdP 兼容性；已有 auth slice 覆盖基础，binding 只复用 Bearer role fixture。
- 不测试 course creation / enrollment 本身的全矩阵；只构造必要 course access facts。
- 不测试复杂并发 binding 创建，除非唯一约束实现出现明显 race 风险；首轮用 migration unique/index + 单线程 service/controller 即可。
- 不测试迁移从“带脏历史数据的生产库”自动修复；如果需要 backfill/cleanup，应单独建迁移数据治理任务。
- 不测试 course graph 暴露绑定 KB，除非 SPEC 明确新增 API response 字段。
- 不测试 AI-generated resource citation/resource-level citation linkage；这是 resource citation governance 后续，不是 KB-course binding 核心。

## 8. 建议执行顺序

1. 先写 `SchemaConvergenceMigrationTest#v20MigrationAddsKbCourseBindingGovernanceTable`，确认 RED。
2. 写 `PermissionServiceTest` 最小 active/dropped student 用例，确认 RED。
3. 写 `RagQueryServiceTest` active/dropped query 用例，确认 RED 且失败前无 query/citation 副作用。
4. 再写 controller binding lifecycle 用例，确认 HTTP 权限/anti-enumeration RED。
5. 实现后按 focused -> adjacent -> full -> MySQL smoke 顺序验证。

## 9. 风险与测试重点

- 高风险：只在 controller 或 document upload metadata 上做绑定校验，`RagQueryService` 仍可通过直接 `kbIds` 查询越权。必须用 service-level query RED 锁住。
- 高风险：binding inactive/deleted 后仍因 KB owner/public/explicit permission 或 cached allowed list 泄露 course material。需要 inactive binding 测试。
- 中风险：admin missing 返回 `NOT_FOUND`，非 admin foreign/missing 返回 `FORBIDDEN` 的 anti-enumeration 语义被破坏。controller 测试要覆盖响应 body 不泄露 id。
- 中风险：H2 `create-drop` 会掩盖 MySQL FK/索引/部分唯一约束差异。必须更新 opt-in MySQL smoke。
- 低风险：CourseKnowledgeController 是否展示 bound KB 属于产品 API 行为，未明确前不应强行测试。

## 10. 结论

最小 RED 切入点应从 `SchemaConvergenceMigrationTest` 的 V20 schema 断言开始，然后在 `PermissionServiceTest` 锁住 course-bound KB 访问语义，再在 `RagQueryServiceTest` 锁住 retrieval 前防越权和无副作用。Controller 测试应只覆盖 binding lifecycle API、roles-first、anti-enumeration，不要把 parser/vector/model 或 frontend 行为纳入本子任务。
