# Object Scope Authorization

## 使用场景

当实现或审查后端对象级权限、课程/班级/资源归属、teacher/student/admin 行为矩阵、IDOR 防护、越权响应、对象存在性探测防护，以及写入请求中的课程/章节元数据 scope 时使用。

## 核心规则

1. Controller 只读取当前用户和请求参数，不写对象归属规则。
2. Service 层必须在修改或返回对象详情前完成授权。
3. 对象授权应沿父资源归属链路判断，例如 `review -> generationTask -> course -> teacherId`。
4. 非管理员用户不得通过 `404` vs `403` 区分“对象不存在”和“对象存在但无权访问”。
5. 越权响应不得包含对象 id、父资源 id、标题、内容、traceId 等可枚举线索。
6. 管理员可保留运维需要的真实 `NOT_FOUND`，但必须显式测试普通用户路径。

## 推荐实现顺序

1. 先写 failing test：同一非管理员用户分别访问 missing id 和 foreign id，断言响应不可用于对象存在性探测。
2. 按父资源链路加载最少必要数据并授权，再加载可见详情。
3. 对 list 接口优先使用 scoped query；若先用内存过滤，必须记录 N+1 和无关对象加载风险。
4. 对 decision/update/delete 接口，在持久化前再次执行归属校验。
5. 对缺少 class/enrollment 模型的过渡期 course list，不要返回全量数据；应明确 student 空列表或最小安全 scope，并在 SPEC/TODO 中记录后续矩阵。
6. 对评估/治理类接口，若请求或 evaluation set 已具备 `courseId`，HTTP 入口必须强制 course scope；若暂时缺少 `courseId`，只能先做 teacher/admin gate，并在 SPEC/TODO 中记录后续收口点。

## 测试建议

- owner/admin/student 三类基础行为。
- own object 成功，foreign object 拒绝。
- missing id 与 foreign id 对非管理员返回同类安全错误。
- list 接口覆盖 admin all、teacher own-only、student no-foreign-data。
- 响应 `data` 不存在，body 不包含 foreign object id / parent id / resource id。
- 已有业务状态流转不因权限收口回退。

## 已知边界

- 该技能不替代完整 RBAC/JWT 设计。
- 对大列表场景，服务层内存过滤只是过渡方案，生产化应沉到仓储查询或批量加载。
# Course Enrollment Scope Pattern

- Treat `course_enrollment.status == ACTIVE` as the student course authorization source for course-bound reads/workflows.
- Centralize course read/manage/enrollment decisions in a service such as `CourseAccessService`; do not duplicate enrollment checks across controllers or agent tools.
- For transitional identities in this project:
  - `admin` can read/manage any existing course and sees missing course as `NOT_FOUND`.
  - `teacher` / `teacher_*` can manage only owned courses.
  - student users can read existing courses only through active enrollment.
- For non-admin course detail/graph reads, collapse missing/foreign/not-enrolled into safe `FORBIDDEN` according to current SPEC.
- Course-bound learning path/resource generation must check enrollment before creating durable task/path/trace/resource rows.
- Non-course template goals remain compatible: only enforce enrollment when `goalId` resolves to an existing course.
- Analytics membership must come from active enrollment; learning paths, wrong questions, and resource tasks are signals for enrolled learners, not class membership sources.

# Assessment Record Detail Scope Pattern

- For assessment answer / wrong-question detail reads:
  - `admin` can read any existing record and sees missing records as `NOT_FOUND`.
  - student users can read only records where `learnerId == currentUserId`.
  - `teacher` / `teacher_*` can read only records tied to their own course and only for learners with active course enrollment.
- When assessment rows do not store `courseId`, derive the parent scope through `questionId -> knowledgePointId -> KnowledgePoint.courseId`; for wrong-question rows, prefer the persisted `knowledgePointId`.
- If course derivation fails, do not grant teacher access.
- For non-admin users, collapse missing and foreign `answerId` / `wrongQuestionId` to the same `FORBIDDEN` response without `data`.
- Use dedicated detail DTOs for assessment record reads; never expose `requestId`, `requestHash`, `responseJson`, `payloadJson`, raw provider errors, or internal event payloads.
- Do not implement list/pagination by loading all records and filtering in memory for production paths; design scoped repository queries or add normalized course scope first.

# Assessment Record List Scope Pattern

- For assessment answer / wrong-question list reads:
  - `admin` can page globally and may filter by `learnerId` / `courseId`; explicit missing course can return `NOT_FOUND`.
  - student users default to `learnerId == currentUserId`; explicit foreign `learnerId` returns `FORBIDDEN` without `data`.
  - `teacher` / `teacher_*` must provide `courseId`, must own that course, and can only see active enrolled learners for that course.
  - teacher filtering by a learner not actively enrolled in the course should return an empty page, not data and not a learner-existence oracle.
- List responses should use summary DTOs, not detail DTOs. Batch responses should omit answer text, request snapshots, raw payloads, long diagnosis text, and internal linkage fields that are not required for list rendering.
- Always bound list pagination (`page >= 0`, small max `size`) and keep sort fields fixed/allowlisted.
- When `AnswerRecord` lacks `courseId`, derive course filtering through existing knowledge conventions (`courseId -> KnowledgePoint ids -> questionIds`) or add a reviewed schema slice before changing persistence.

# Grading Evaluation Course Scope Pattern

- For `POST /api/assessment/grading-evaluations` and similar offline evaluation/governance HTTP APIs:
  - Require `courseId` once the request contract supports it.
  - Deny student/ordinary users before course lookup or sample validation so they cannot probe course or sample existence.
  - `teacher` / `teacher_*` can evaluate only own courses through centralized course authorization such as `CourseAccessService.requireCourseRead(currentUserId, courseId)`.
  - For teachers, collapse missing and foreign course to safe `FORBIDDEN` without `data`.
  - `admin` can evaluate any existing course and may receive real `NOT_FOUND` for missing course.
- Treat sample object ids as consistency inputs, not authorization anchors. For grading samples, non-blank `knowledgePointId` must belong to the request `courseId`.
- Return one generic validation message for missing/foreign sample knowledge points, for example `Sample knowledge points must belong to request course`; do not echo the offending id, course id, or parent title.
- Legacy metric modes may remain algorithm-compatible in pure unit tests, but HTTP paths must not keep an unscoped legacy bypass after course scope exists.

# RAG Document Upload Metadata Scope Pattern

- Treat KB write permission and course metadata write permission as separate authorization boundaries.
- For `POST /api/knowledge-bases/{kbId}/documents` and similar upload APIs, validate normalized `courseId/chapterId` before requestId hashing/replay, object storage, document persistence, and index task creation.
- Non-empty `courseId` should use centralized course authorization such as `CourseAccessService.requireCourseRead(...)` plus `requireCourseManage(...)`; teachers can write only own-course metadata, admins can write existing courses, and students cannot spoof course metadata.
- Non-empty `chapterId` must require `courseId`; never accept a chapter id without a course scope.
- Missing or foreign chapter should return a generic `VALIDATION_ERROR` without echoing the chapter id, course id, title, or teacher id.
- RequestId payload hashing should include normalized course/chapter metadata so replay and conflict semantics remain deterministic.
- If `KnowledgeBase` is not course-bound in schema, do not pretend KB-course consistency is solved; record the remaining schema/task boundary explicitly.

# RAG Query Runtime Scope Pattern

- Query-time KB read checks must run before retrieval, query-log writes, or citation writes.
- Runtime RAG callers should pass explicit role facts into the query service: `currentUserId`, `currentUserAdmin`, and `currentUserTeacher`.
- RequestId replay checks are still authorization checks. A matching old response must not replay until the current caller's role facts can still read every requested KB.
- Legacy query overloads may remain for compatibility, but they must default to non-admin/non-teacher semantics and must not infer roles from literal subjects such as `admin`.
- Orchestrator or workflow wrappers must pass the same role facts to replay precheck and to actual query execution.
- Forbidden RAG runtime requests should leave no `kb_query_log` or `source_citation` success artifacts; if a workflow task already exists, persist only safe failure evidence.
