# PRD-20260608 P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. 背景

当前 RAG 文档上传接口：

```text
POST /api/knowledge-bases/{kbId}/documents
```

支持可选 `courseId` / `chapterId` 元数据，但现有实现只校验 KB 写权限，未校验上传者是否能管理该课程，也未校验 `chapterId` 是否属于 `courseId`。这会让有 KB 写权限的用户把文档标记到无权课程或错误章节，污染后续 Course RAG、引用和 analytics 元数据。

## 2. 目标

- 上传文档时，对非空 `courseId` 执行课程管理权限校验。
- 上传文档时，对非空 `chapterId` 执行 `chapter.courseId == request.courseId` 一致性校验。
- 保留无 `courseId` 的通用 KB 文档上传能力。
- 保留现有文档上传幂等、索引任务创建、reindex、detail/list 行为。

## 3. 用户故事

| Actor | Story |
|---|---|
| teacher | 作为课程教师，我只能把 RAG 文档标记到自己管理的课程和该课程章节。 |
| admin | 作为管理员，我可以把 RAG 文档标记到任意存在课程及其章节。 |
| student / ordinary user | 作为普通用户，即使拥有个人 KB 写权限，也不能伪造课程/章节元数据。 |

## 4. 非目标

- 不新增真实 JWT/RBAC。
- 不新增 `KnowledgeBase.courseId` schema。
- 不新增 DB migration。
- 不新增 OCR/parser/model/vector 依赖。
- 不改变 RAG 检索排序、chunk、citation 结构。
- 不改 frontend。

## 5. 成功标准

- teacher own-course + own-course chapter 上传成功。
- teacher foreign/missing course 上传返回安全 `FORBIDDEN`，且不创建 document/index task。
- student 带 `courseId` 上传返回 `FORBIDDEN`，且不创建 document/index task。
- admin existing course 上传成功；admin missing course 返回 `NOT_FOUND`。
- `chapterId` 非空但缺少 `courseId` 返回 `VALIDATION_ERROR`。
- `chapterId` 不属于 `courseId` 返回通用 `VALIDATION_ERROR`。
- 旧无 course 元数据上传、requestId replay、payload conflict 仍保持现有行为。
