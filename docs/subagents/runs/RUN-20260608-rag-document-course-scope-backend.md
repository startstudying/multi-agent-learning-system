# RUN-20260608 RAG Document Course Scope - Backend/RAG Expert

## 结论

当前上传链路已接收并保存 `courseId` / `chapterId`，但 `DocumentService.upload(...)` 只校验 KB 写权限，未在写对象存储、创建 `KbDocument`、创建 `KbIndexTask` 前验证课程管理权限和章节归属。本切片应只收口上传入口，不改 RAG parser、chunk、embedding、vector、retrieval、DTO、schema 或 frontend。

## 关键证据

- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`：上传接口接收 `courseId`、`chapterId`、`requestId` 并委托 Service。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`：现有顺序为 load KB -> `ensureCanWrite(...)` -> requestId/hash/replay -> `storageService.store(...)` -> 保存文档。
- `backend/src/main/java/com/learningos/rag/domain/KbDocument.java`：已有 `courseId` / `chapterId` 字段。
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`：已有 `requireCourseRead(...)` 和 `requireCourseManage(...)`，可复用。
- `backend/src/main/java/com/learningos/knowledge/repository/ChapterRepository.java`：继承 `JpaRepository`，可直接 `findById(...)`。

## 推荐实现

1. 在 `DocumentService.upload(...)` 内，`ensureCanWrite(...)` 之后先归一化 `courseId` / `chapterId`。
2. 在 `normalize requestId`、`requestHash(...)`、`storageService.store(...)` 之前执行 `validateCourseChapterScope(...)`。
3. `courseId` 非空时先 `CourseAccessService.requireCourseRead(userId, courseId)`，再 `requireCourseManage(userId, course)`；不能只检查 read，否则 enrolled student 会通过。
4. `chapterId` 非空但 `courseId` 为空时返回 `VALIDATION_ERROR`：`courseId is required when chapterId is provided`。
5. `chapterId` 非空时用 `ChapterRepository.findById(...)`，并检查 `chapter.getCourseId().equals(courseId)`；缺失和 foreign chapter 统一返回 `VALIDATION_ERROR`：`chapterId must belong to request course`。
6. 保存和 request hash 均使用归一化后的 `courseId` / `chapterId`。

## 非目标

- 不新增 `KnowledgeBase.courseId` 或迁移。
- 不新增 repository 方法或依赖。
- 不新增响应字段。
- 不改文档 detail/list/reindex 合同。
- 不处理历史已污染文档。

## 测试重点

- teacher own course + valid chapter 上传成功并持久化元数据。
- teacher foreign/missing course 拒绝，且不创建文档和索引任务。
- student 带 `courseId` 拒绝，证明 KB write 不等于 course metadata write。
- admin missing course 返回 `NOT_FOUND`。
- `chapterId` without `courseId` 返回 `VALIDATION_ERROR`。
- foreign/missing chapter 返回通用 `VALIDATION_ERROR` 且不回显 offending id。
- 无 course metadata 的上传、requestId replay、requestId payload conflict 保持兼容。
