# RUN-20260608 RAG Document Course Scope - Integration Review

## 合并结论

采纳 Backend/RAG Expert 与 Security & Quality 的共同建议：本切片只在 `DocumentService.upload(...)` 上传入口收口 `courseId` / `chapterId` 元数据 scope，且校验必须发生在 requestId/hash/replay/store/save/index 之前。

## 决策

1. 不改 schema，不新增 DB migration，不绑定 `KnowledgeBase.courseId`。
2. 不改 `DocumentDtos`，不新增响应字段。
3. 不改 frontend、RAG parser、chunk、embedding、vector、retrieval 或 model provider。
4. `courseId` 非空时使用 `CourseAccessService.requireCourseRead(...)` 加 `requireCourseManage(...)`，避免 student 只凭 read/enrollment 上传课程资料。
5. `chapterId` 校验使用 `ChapterRepository.findById(...)`，判断 `chapter.courseId == request.courseId`。
6. `chapterId` 非空但无 `courseId` 返回 `VALIDATION_ERROR`，消息固定为 `courseId is required when chapterId is provided`。
7. missing/foreign chapter 统一返回 `VALIDATION_ERROR`，消息固定为 `chapterId must belong to request course`，不回显 offending id。
8. teacher/student missing or foreign course 使用 `CourseAccessService` 既有安全语义：`FORBIDDEN`；admin missing course：`NOT_FOUND`。
9. request hash 继续包含归一化后的 `courseId` / `chapterId`，相同 `requestId` 不同 course/chapter 仍 `CONFLICT`。

## 冲突处理

Security & Quality 建议非 admin chapter missing/foreign 可返回 `FORBIDDEN`。集成决策采用 SPEC 中的通用 `VALIDATION_ERROR`，理由：

- 章节参数是 course metadata consistency 输入，而不是独立授权对象入口。
- 固定 `VALIDATION_ERROR` message 不区分 missing/foreign，避免枚举信号。
- 与当前 `SPEC-20260608-rag-document-course-scope.md` 保持一致，减少合同漂移。

## 实施边界

允许修改：

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- 本切片 workflow / evidence / acceptance / memory / changelog / skill 文档

禁止修改：

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- unrelated Agent/model/parser/vector/assessment files
