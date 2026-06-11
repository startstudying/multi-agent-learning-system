# P3-4 子任务：RAG KB-course binding governance 测试最终审查

## 结论

当前失败 `DocumentControllerTest.rejectsSameRequestIdWhenCourseOrChapterMetadataChanges` 不建议改成期望 `400 VALIDATION_ERROR`。

建议修实现顺序：当同一 `createdBy + requestId` 已存在时，应在完成 KB 写权限和 `requestId` 基础规范化后，按既有幂等契约计算当前 payload hash 并返回 replay 或 `409 CONFLICT`。不应让后续 KB-course/document-course consistency 校验把同一幂等键的 payload 冲突误报为普通 `VALIDATION_ERROR`。

理由：

- 既有文档上传幂等契约明确规定：同一 `createdBy + requestId` 但 payload 不同返回 `409 CONFLICT`。
- 当前失败场景正是第一次上传已把 KB 绑定到 first course，第二次复用同一 `requestId` 且换成 second course/chapter。它首先是幂等键冲突，其次才会触发绑定课程不一致。
- KB-course binding 治理新增的 `BOUND KB` 显式 course 不一致返回 `VALIDATION_ERROR` 应覆盖普通新请求或无 idempotency 命中的请求；不应覆盖已命中的 requestId conflict。
- `RagQueryService` 相邻语义也保持：requestId replay 前重新做权限校验，但同 requestId payload hash 不同最终是 `CONFLICT`，且不新增成功业务副作用。

## 证据

- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java:362`
  - `rejectsSameRequestIdWhenCourseOrChapterMetadataChanges` 首次用 first course/chapter 成功上传，第二次同 `requestId` 换 second course/chapter，期望 `409 CONFLICT`，并断言 document/index task count 仍为 1。

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:99`
  - 当前实现先 resolve effective course。

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:109`
  - 当前实现先 validate course/chapter scope。

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:111`
  - 当前实现之后才 normalize `requestId`。

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:126`
  - 当前实现之后才按 `createdBy + requestId` 查 existing document。

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:206`
  - BOUND KB 显式 request course 与 KB course 不一致时抛 `VALIDATION_ERROR`，这解释了当前实际 400。

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:372`
  - `replayExistingUpload` 在 request hash 不同才抛 `CONFLICT`，但当前失败没有走到这里。

- `docs/requirements/REQ-20260606-rag-document-upload-idempotency.md:4`
  - 文档上传幂等需求要求不同 payload 复用同一 `requestId` 返回 `409 CONFLICT`。

- `docs/specs/SPEC-20260606-rag-document-upload-idempotency.md:20`
  - 服务流程要求 `requestId` 非空时计算 payload hash，查找 `createdBy + requestId`，hash 不同返回 409。

- `docs/specs/SPEC-20260610-p3-4-kb-course-binding-governance.md:70`
  - 新治理文档写明 BOUND KB course mismatch 返回 `VALIDATION_ERROR`，但没有废止既有 requestId conflict 契约。

- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java:402`
  - `rejectsSameRequestIdWhenPayloadHashDiffers` 明确断言 RAG query 同 requestId payload 不同为 `ErrorCode.CONFLICT`。

- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:485`
  - replay context 先执行 `permissionService.requireReadableKbIds(...)`，再计算 request hash。

- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:514`
  - `replayExistingQuery` 中 hash 不同返回 `CONFLICT`。

- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java:109`
  - PermissionService 已覆盖 active enrolled student 对 BOUND private KB 可读。

- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java:120`
  - PermissionService 已覆盖 dropped student 对 BOUND public KB 不可读。

- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java:132`
  - PermissionService 已覆盖 teacher own-course BOUND KB 可读写，以及 subject-name role-confusion 不提权。

- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java:145`
  - PermissionService 已覆盖 CONFLICTED KB 不走 owner/public/course-derived read，admin 仍可治理。

## 建议实现方向

不改测试期望。建议调整 `DocumentService.upload` 的分支顺序：

1. load active KB。
2. `ensureCanWrite(...)` 保持在 replay 前，避免无权限用户通过 requestId 探测历史上传。
3. normalize course/chapter/requestId。
4. 若 `requestId` 非空且存在同 `createdBy + requestId` 记录：
   - 计算 hash 时应使用与首次上传一致的有效 course scope 语义。
   - 对 BOUND KB，若 request course 与 KB course 不一致，可用于 hash 差异判断，但不要先抛 validation。
   - hash 相同 replay，hash 不同返回 `409 CONFLICT`。
5. 若不存在 existing requestId，再执行 KB-course/document-course consistency 校验、chapter scope 校验、storage/save/index。

需要注意：如果第二次请求省略 `courseId`，BOUND KB 会使用 KB course，这应仍能 replay 首次响应；如果第二次显式传不同 `courseId`，应是 409。

## 建议新增/调整断言

保留并强化当前失败用例：

- `rejectsSameRequestIdWhenCourseOrChapterMetadataChanges`
  - 继续期望 HTTP 409。
  - 增加 `jsonPath("$.code").value("CONFLICT")` 已有。
  - 建议增加 `jsonPath("$.message").value("requestId already used with different document upload payload")`，确保不是其他 409。
  - 保留 `documentRepository.count() == 1`。
  - 保留 `indexTaskRepository.count() == 1`。
  - 建议增加响应 body 不包含 second course/chapter id，避免冲突响应泄露新 payload 元数据。

建议补一个独立行为测试：

- `replaysSameRequestIdWhenCourseBoundKnowledgeBaseRequestOmitsCourseMetadata`
  - 首次上传带 matching course/chapter。
  - 第二次同 `requestId`、同文件、同 chapter，可省略 `courseId` 或显式传同 course。
  - 期望 200 replay，同 documentId/indexTaskId，count 不增加。

建议补一个普通 validation 测试，避免修复后误放行无幂等命中的 mismatch：

- `rejectsNewUploadWhenCourseMetadataConflictsWithBoundKnowledgeBase`
  - KB 已 BOUND 到 course A。
  - 新 `requestId` 或无 `requestId` 上传显式 course B。
  - 期望 400 `VALIDATION_ERROR`，message 为 `Document course must match knowledge base course`，document/index count 不变。

## 建议测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest#rejectsSameRequestIdWhenCourseOrChapterMetadataChanges test
```

Document adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest test
```

P3-4 KB-course binding adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,RagQueryServiceTest,PermissionServiceTest,SchemaConvergenceMigrationTest test
```

Runtime/query adjacent, if DocumentService changes touch shared permission helpers:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,OrchestratorWorkflowControllerTest,RagQueryServiceTest test
```

Full backend before acceptance:

```powershell
cd D:\多元agent\backend
mvn test
```

## 风险

- 如果直接把测试改为 400，会破坏既有 document upload idempotency 契约，并与 RAG query/answer submission/requestId 系列契约不一致。
- 如果实现调整时把 permission check 放到 existing requestId replay 后，会引入 requestId 探测风险；必须保留 KB write permission 在 replay 前。
- 如果 hash 计算继续只接受 resolved effective course，BOUND mismatch 会在 hash 前失败；需要让 existing requestId conflict 分支能够计算“当前请求 payload hash”或等价比较，避免 validation 抢先返回。
