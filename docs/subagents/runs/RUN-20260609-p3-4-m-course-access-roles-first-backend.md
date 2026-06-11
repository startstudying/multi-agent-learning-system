# P3-4-M Course API / CourseAccessService roles-first overload 后端架构报告

## 1. 结论

P3-4-M 最小设计应只做 Course API / Knowledge Catalog 的 roles-first overload 局部迁移，保留旧签名，避免牵动 assessment、RAG、learning、resource generation 等已验收切片。

## 2. 推荐设计

新增 `CourseAccessService` role-aware overload：

- `requireCourseRead(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId)`
- `requireCourseManage(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, Course course)`
- `listCoursesForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher)`

旧签名继续保留，并委托到新签名的 legacy 推断路径，避免一次性改动所有调用方。

## 3. 优先迁移入口

- `CourseController` 的 `create/get/list/createChapter/getKnowledgeGraph`
- `KnowledgePointController` 的 `createKnowledgePoint/createDependency`
- `KnowledgeCatalogService` 对课程读、列表、写、知识图谱写路径的内部调用

## 4. 暂不迁移入口

以下调用方属于不同已验收或后续 RBAC 切片，本轮不扩大范围：

- `AssessmentService`
- `GradingEvaluationService`
- `DocumentService`
- `LearningWorkflowService`
- `ResourceGenerationService`

## 5. 允许修改文件建议

- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/api/KnowledgePointController.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`

## 6. 架构漂移风险

- 当前 `CurrentUserService` 已支持 Bearer roles-first，但 Course API 仍只传 `currentUserId()`，导致 Bearer `ADMIN/TEACHER` 与 spoofed `X-User-Id` 场景存在迁移缺口。
- 最小迁移应保持 Controller -> Application Service -> Repository 分层，不新增依赖、不改 schema、不改 API path。

## 7. 主要证据

- `CourseController` 只传 `currentUserId()`：`backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `CourseAccessService` 仍用 `"admin"` / `"teacher_"` 推断角色：`backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `CurrentUserService` 已优先使用 roles：`backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- P3-4-L 明确留下 full `CourseAccessService` role-aware migration：`docs/memory/BACKEND_MEMORY.md`
- 课程读/知识图谱 enrollment scope 已存在：`docs/memory/BACKEND_MEMORY.md`
