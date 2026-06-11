# P3-4 子任务：KB-course binding governance 架构专家报告

## 结论

本子任务应按 **L 级后端安全/数据治理切片** 执行。原因是它同时涉及 DB schema、RAG KB 生命周期、CourseAccess 权限事实、RAG query 前置过滤和迁移兼容。

推荐采用最小可控方案：**不新增独立绑定表，直接在 `kb_knowledge_base` 增加 KB 级课程绑定字段**，把课程绑定变成 KB 本身的权威事实。

## 数据模型建议

在 `kb_knowledge_base` 增加：

- `course_id varchar(80) null`
- `binding_status varchar(40) not null default 'UNBOUND'`
- `bound_by varchar(120) null`
- `bound_at datetime(6) null`

状态语义：

- `UNBOUND`：个人/通用 KB，沿用 owner / visibility / explicit permission。
- `BOUND`：课程 KB，`course_id` 是权威课程范围，读写必须通过 `CourseAccessService`。
- `CONFLICTED`：迁移发现历史文档课程不一致或无效，默认不启用课程派生权限。

索引/约束建议：

- `idx_kb_course_binding(course_id, binding_status, deleted_at)`
- `idx_kb_document_kb_course_deleted(kb_id, course_id, deleted_at)`
- `ck_kb_binding_status`
- `ck_kb_binding_course_consistency`
- `fk_kb_course_binding_course`

## Service/API 建议

- `CreateKnowledgeBaseRequest` 增加可选 `courseId`。
- `KnowledgeBaseResponse` 返回 `courseId` 和 `bindingStatus`。
- `KnowledgeBaseService.create(...)` 在 `courseId` 非空时调用 `CourseAccessService.requireCourseRead(...)` 和 `requireCourseManage(...)`。
- `PermissionService` 对 `BOUND` KB 使用 `CourseAccessService` 判定读写权限，`PUBLIC` 和 owner 不再绕过课程范围。
- `DocumentService.upload(...)` 对 `BOUND` KB 强制文档课程与 KB 课程一致；对 `UNBOUND` KB 拒绝新的 course metadata 写入，避免继续产生混合课程 KB。
- RAG query 无需改检索算法；只要 `PermissionService.requireReadableKbIds(...)` 统一接入 course-bound 权限，Chat/Tutor/Orchestrator 会继承治理语义。

## 风险与兼容策略

- 历史 KB 可能包含多个 `kb_document.course_id`，不能自动绑定到任意课程，否则会造成跨课程资料泄漏。
- `Visibility.PUBLIC` 对 `BOUND` KB 只能理解为课程内可见，不能作为全站公开。
- KB owner 对 `BOUND` KB 不应绕过课程教师/管理员权限。
- 迁移只自动绑定“所有活跃文档都属于同一个有效课程”的 KB；混合或无效课程数据标记为 `CONFLICTED`。

## 建议测试矩阵

- Migration：V20 字段、索引、约束、MySQL smoke。
- Permission：active enrolled student 可读，dropped/never enrolled 不可读；teacher own-course 可写，foreign teacher 不可写。
- Query：course-bound KB 在 retrieval/log/citation 前拒绝越权。
- Document：bound KB 自动使用/校验 KB course；unbound KB 拒绝 course metadata。
- Controller：Bearer `TEACHER` 无 `teacher_` 前缀可管理自有课程 KB；Bearer `USER sub=teacher_1/admin` 不提权。

