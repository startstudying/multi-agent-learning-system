# P3-4 子任务：KB-course binding governance 集成评审

## 输入

- 架构专家报告：`docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-architect.md`
- 安全专家报告：`docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-security.md`
- 测试专家报告：`docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-test.md`

## 决策

采用“KB 表内绑定字段”方案，而不是新增 `kb_course_binding` 表。

理由：

1. 当前系统检索锚点是 `kb_id`，一 KB 对一课程是最小可验证治理边界。
2. 新表会引入绑定生命周期查询复杂度，但当前没有多课程 KB 的产品需求。
3. 字段方案能直接让 `PermissionService.requireReadableKbIds(...)` 成为统一授权门，覆盖 RAG runtime、Chat、Tutor 和 Orchestrator。

## 冲突处理

| 议题 | 专家意见差异 | 集成结论 |
|---|---|---|
| schema 形态 | 测试规划建议可测新表；架构建议字段 | 使用字段方案，并在 migration/static smoke 中验证字段、索引、约束 |
| BOUND KB 上传不带 `courseId` | 安全报告偏 fail closed；架构建议可自动填充 | 允许自动使用 KB `courseId`，前提是写权限已通过 course manage；显式不一致则拒绝 |
| UNBOUND KB 上传 course metadata | 历史行为允许；安全报告要求治理 | 新行为拒绝，防止继续生成混合课程 KB；历史数据通过 V20 回填/CONFLICTED 兼容 |
| PUBLIC 语义 | 原行为全站公开 | BOUND KB 下 PUBLIC 不绕过 CourseAccess；UNBOUND PUBLIC 保持原行为 |

## 实施边界

本次实现：

- V20 migration。
- `KnowledgeBase` entity / DTO。
- `KnowledgeBaseService` create-time binding。
- `PermissionService` course-bound read/write。
- `DocumentService` KB-course/document-course 一致性。
- Migration / Permission / RAG query / KB controller / Document controller 测试。

本次不做：

- 新增绑定修复 API。
- 前端治理 UI。
- VectorDB / parser / reranker 改造。
- 完整 class/course/answer record 权限矩阵。

## 评审结论

可以进入 TDD 实现。需要先写 RED：migration、PermissionService、RAG query、controller/document 交叉行为。

