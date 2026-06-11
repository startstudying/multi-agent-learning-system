# PRD - P3-4 KB-course binding schema and lifecycle governance

## 背景

当前 RAG KB 已完成 management roles-first RBAC 和 query runtime roles-first RBAC，但 KB 本身没有课程绑定字段。`kb_document.course_id` 只能说明单个文档上传时的课程元数据，不能保证整个 KB 的课程范围一致。

如果一个 KB 混入多个课程文档，RAG query 只按 `kb_id` 检索 chunk，可能让 `PUBLIC`、owner 或 explicit permission 绕过课程 teacher/enrollment 权限。

## 目标

建立最小生产级 KB-course binding 治理：

1. KB 可显式绑定到一个 Course。
2. 课程绑定 KB 的读写权限必须通过 `CourseAccessService`。
3. `PUBLIC` 和 KB owner 不能绕过课程范围。
4. 文档上传必须与 KB 课程绑定保持一致。
5. 历史数据通过 migration 自动回填或标记冲突。

## 非目标

- 不新增前端治理 UI。
- 不新增 VectorDB / parser / OCR / reranker 能力。
- 不实现多课程 KB。
- 不实现完整绑定修复工作台。
- 不新增依赖。

## 用户影响

- 教师可以创建绑定自己课程的 KB，并上传该课程资料。
- 已选课学生可以查询绑定课程的 KB。
- 未选课学生不能通过 PUBLIC KB 或 KB owner 权限读取课程资料。
- 历史混合课程 KB 默认进入冲突治理状态，不自动扩大访问范围。

## 成功指标

- course-bound KB query 越权请求在 retrieval/log/citation 前被拒绝。
- course-bound KB document upload 不允许混入其他 course。
- full backend test 通过，或明确说明无法运行的环境限制。

