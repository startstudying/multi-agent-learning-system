# RUN - 20260609 P3-4-P RAG KB roles-first RBAC Security Review

## 1. 总体风险

RAG KB management 的主要风险是 Broken Access Control：

- Bearer `USER/STUDENT sub=admin` 可能在 document/index missing 语义中获得 admin 风格 `NOT_FOUND`。
- Bearer `USER sub=teacher_1` 可能在 RAG document course metadata 校验中被 legacy `teacher_*` 推断为 teacher。
- Bearer `ADMIN sub=ops_admin` 可能因为不是 literal `admin` 而无法获得 admin 全局 KB/document/index 管理语义。

## 2. 关键风险点

| 风险 | 位置 | 影响 |
|---|---|---|
| Course scope legacy inference | `DocumentService.validateCourseChapterScope(...)` -> `CourseAccessService.requireCourseRead/Manage(userId, ...)` | `USER sub=teacher_1` 可能被当成 teacher。 |
| Missing oracle uses subject | `DocumentService.scopedMissing(userId, ...)` | `USER sub=admin` 可能看到 `NOT_FOUND`；`ADMIN sub=ops_admin` 反而看到 `FORBIDDEN`。 |
| KB admin management not role-aware | `KnowledgeBaseService.listAccessible(...)` / `PermissionService` | Bearer admin 不能全局 list/read/write private KB。 |

## 3. 建议 roles-first 规则

| Role facts | KB create | KB list | document upload/list/detail/reindex/index-task |
|---|---:|---:|---:|
| `ADMIN` | allow personal/admin-owned KB | all active KBs | read/write all active KBs；missing document/index task returns `NOT_FOUND` |
| `TEACHER` | allow personal KB | owner/public/explicit permission | owner/public/explicit read；owner/write permission write；course metadata only own course |
| `STUDENT` / `USER` | 保留个人 KB 创建能力 | owner/public/explicit permission | owner/public/explicit read；owner/write permission write；带 course metadata 仍不得伪造课程 |

说明：本切片不移除学生端个人 KB 创建能力，因为前端学生工作台当前使用 `POST /api/knowledge-bases` 创建个人 KB。

## 4. 推荐测试矩阵

- Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id`：
  - `GET /api/knowledge-bases` 可看到 foreign private KB。
  - `POST /api/knowledge-bases/{kbId}/documents` 可写 foreign private KB。
  - missing document/index task 返回 `NOT_FOUND`。
- Bearer `TEACHER sub=instructor_1`：
  - 可上传 own-course metadata，不需要 `teacher_` prefix。
- Bearer `USER sub=teacher_1`：
  - 即使 course.teacherId = `teacher_1`，也不能上传 course metadata。
- Bearer `USER/STUDENT sub=admin`：
  - missing document/index task 返回 safe `FORBIDDEN`，不形成 admin oracle。

## 5. 响应字段约束

权限失败响应不得包含：

- `documentId`
- `indexTaskId`
- `kbId`
- `courseId`
- `chapterId`
- document name / course title
- raw parser/index/storage error

## 6. 后续项

本切片不应声称完成：

- KB 与课程/班级 schema 绑定。
- KB role/group permission 模型。
- full OAuth2/JWK/Spring Security。
- broader class/course 权限模型。
- RAG query runtime 全量 roles-first 管理语义。
