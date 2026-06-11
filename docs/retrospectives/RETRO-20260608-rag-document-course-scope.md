# Retrospective - P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. Feature Summary

收口 RAG 文档上传入口的 course/chapter 元数据写入权限：teacher 只能写自己课程，student 不能伪造课程元数据，admin 对 missing course 保持真实 `NOT_FOUND`，chapter 必须属于请求 course，且所有校验都发生在对象存储之前。

## 2. What Went Well

- Spec-first + TDD 路径清晰，范围没有扩到 parser/vector/model/provider。
- `CourseAccessService` 复用得足够直接，没引入新权限分叉。
- 代码审查把遗漏的 missing chapter 用例及时补上了，避免只测 foreign chapter。
- 失败请求在 `storageService.store(...)` 之前被拦截，副作用边界干净。

## 3. What Didn't Go Well

- 初版测试只覆盖了 foreign chapter，没有单独覆盖 missing chapter，验证矩阵不完整。
- `DocumentControllerTest` 里为了覆盖权限矩阵，前置数据种子比较多，读起来略长。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| RAG document upload 的 course/chapter 元数据 scope 必须前移到存储前，并用 teacher / student / admin + missing / foreign chapter 的矩阵拆开验证。 | Yes | `docs/skills/project-specific/object-scope-authorization.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| 测试 | 先写一组矩阵测试，但容易漏掉 missing vs foreign 的对称项。 | 每次写权限矩阵时，按 missing / foreign / no-parent 三种状态分别写独立用例。 |
| 验证 | 先跑 focused，再跑 adjacent，再跑 full backend。 | 保持不变。 |
| 文档 | 只更新 slice 相关文档。 | 保持不变。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 将 RAG document metadata scope pattern 沉淀到 object-scope-authorization 技能。 | Main Codex | 本切片完成 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md
- [x] ARCHITECTURE_BASELINE.md
