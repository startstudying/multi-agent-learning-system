# Retrospective - P3-4-C 权限矩阵安全前置

## 1. Feature Summary

完成 P3-4-C 安全前置：课程 list/detail/knowledge-graph 读取按过渡 admin/teacher/student scope 收口，非 admin missing/foreign course 读取不再形成枚举差异，grading evaluation 改为 teacher/admin only。

## 2. What Went Well

- 先通过 Security & Quality、Integration Reviewer、RAG Parser、Model Gateway 专家报告把 P3 剩余项拆分，避免把 RBAC、provider、parser/OCR 混在同一切片。
- 当前切片范围足够小：只改 course read/graph 和 grading evaluation，不引入 schema、依赖、前端或真实 JWT。
- TDD 覆盖了 course list admin/teacher/student、course detail missing vs foreign、knowledge graph scope、student grading evaluation forbidden。
- 聚焦、相邻回归和全量后端 Maven 测试均通过。

## 3. What Didn't Go Well

- 交接时 Model Gateway Expert 已完成但未落盘，需要接手后补写报告。
- 项目 registry 中有 `retrospective-skill-extraction`，但本地没有对应 skill 文件，只能按模板执行。
- 当前身份仍是 `X-User-Id` 字符串模型，权限收口只是安全前置，不能代表完整生产 RBAC。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| transitional course list/detail/graph scope：admin all、teacher own-only、student no-foreign-data；non-admin missing/foreign 统一 `FORBIDDEN`；缺少 courseId 的评估接口先做 teacher/admin gate | Yes | `docs/skills/project-specific/object-scope-authorization.md`（已扩展） |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Subagent evidence | 专家可能只返回通知而未落盘 | 集成前检查 `docs/subagents/runs/RUN-*.md` 是否齐全；缺失立即补写摘要报告 |
| Skill extraction | registry 中技能文件可能不存在 | 若本地 skill 缺失，按模板执行并在 retrospective 中记录缺口 |
| Security slicing | P3-4 容易被理解为一次性完整 RBAC | 继续拆成真实 JWT/RBAC、class/enrollment、answer record matrix、course-scoped evaluation 等小切片 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计 P3-3-B `model_call_log.provider` schema/provider observability 切片 | 后续开发者 | 新建 PRD/REQ/SPEC/PLAN/TASK |
| 设计 class/enrollment 数据模型与 course/class 权限矩阵 | 后续开发者 | 后续 P3-4 切片 |
| 设计 answer record list/detail 权限矩阵 | 后续开发者 | 后续 P3-4 切片 |
| 为 grading evaluation 增加 course/evaluation set scope | 后续开发者 | 后续 evaluation/security 切片 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] API_MEMORY.md
- [x] CHANGELOG.md
- [x] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md（本切片无基线规则变更）
