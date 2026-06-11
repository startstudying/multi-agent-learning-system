# RUN-20260610 P3-4 KB-course binding governance 文档一致性最终复核

## 角色

Documentation Consistency Expert。

## 范围

对 P3-4 语义子任务 `KB-course binding governance` 做只读文档一致性复核。

检查对象：

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/planning/backend-architecture-todolist.md`
- `KB-course binding schema/lifecycle governance` 相关 stale 文案

## 发现

初始复核发现 1 个当前文档不一致点：

- `docs/memory/PROJECT_MEMORY.md` 在 2026-06-10 RAG query runtime 更新中仍把 `KB-course binding schema/lifecycle governance` 列为 P3-4 open follow-up。

主 Codex 已修正：

- 在 Completed Features 增加 `RAG KB-course binding governance`。
- 在 Recent Planning Updates 增加 2026-06-10 本子任务完成记录。
- 从当前 P3-4 open 列表移除 KB-course binding，同时保留 P3-4 父项 open。

## 最终一致性结论

PASS，附历史文档说明。

- 当前 memory、changelog、planning 文件不再把本子任务列为 open。
- `docs/planning/backend-architecture-todolist.md` 只勾选 KB-course binding 子项，没有关闭 P3-4 父项。
- 较早的 RAG query runtime 切片文档仍可能把 KB-course binding 记录为 future work；这些属于本子任务完成前的历史记录，不作为当前状态来源。

## 当前 P3-4 剩余 open 项

- broader class/course matrix
- answer-record expansion
- SSE production auth strategy
- dev/test legacy fallback cleanup
- 可选：`CONFLICTED` KB repair/split workbench
