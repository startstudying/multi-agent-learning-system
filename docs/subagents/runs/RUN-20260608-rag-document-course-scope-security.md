# RUN-20260608 RAG Document Course Scope - Security & Quality

## 结论

风险级别：HIGH。当前 `POST /api/knowledge-bases/{kbId}/documents` 存在 Broken Access Control / IDOR 风险：客户端提交的 `courseId` / `chapterId` 被当作可信元数据保存，而服务端只校验 KB 写权限。KB write 不能替代课程管理权限，也不能证明章节属于请求课程。

## 风险说明

- 拥有某个 KB 写权限的用户可伪造课程/章节元数据，污染 Course RAG、引用展示和后续分析。
- 若失败请求在校验前已进入对象存储或创建 `kb_document` / `kb_index_task`，会留下持久副作用。
- 若错误响应区分 missing chapter 与 foreign chapter，可能形成章节枚举信号。
- 若只调用 `requireCourseRead(...)`，active enrolled student 会获得课程 read，但不应获得课程资料上传管理能力。

## 安全语义建议

| 场景 | 结果 |
|---|---|
| 无 KB 写权限 | `FORBIDDEN`，无 `data` |
| teacher foreign/missing course | `FORBIDDEN`，无 `data` |
| student / ordinary user 带 `courseId` | `FORBIDDEN`，无 `data` |
| admin missing course | `NOT_FOUND`，无 `data` |
| `chapterId` without `courseId` | `VALIDATION_ERROR` |
| missing/foreign chapter | 通用 `VALIDATION_ERROR`，固定消息，不回显 id |
| requestId 相同但 course/chapter 变化 | `CONFLICT` |

## 测试矩阵

- teacher own course + own chapter：200。
- teacher own KB + foreign course：403，且不创建文档/索引任务。
- teacher own KB + missing course：403，响应不包含请求 courseId。
- student own KB + enrolled course：403，证明 enrolled read 不等于 manage。
- admin own KB + missing course：404。
- `chapterId` 无 `courseId`：400。
- chapter 属于其他 course：400，响应不包含 foreign chapterId/courseId。
- 相同 `requestId` + 相同 course/chapter：replay。
- 相同 `requestId` + 不同 course/chapter：409。

## 安全非目标

- 不在本切片实现 JWT/RBAC。
- 不新增 KB-course 绑定 schema；该缺口应作为后续 schema 任务记录。
- 不改变 RAG 检索、索引 worker、parser 或模型 provider。
- 不处理历史污染数据清理。
