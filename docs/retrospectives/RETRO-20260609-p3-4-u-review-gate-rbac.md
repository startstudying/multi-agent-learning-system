# RETRO - P3-4-U Review Gate ResourceReview roles-first RBAC

## What Worked

- TDD RED 准确暴露三类旧行为：Bearer admin 被拒、`USER sub=admin` 被放行、`USER sub=teacher_1` 被放行。
- 最小 GREEN 只改 Controller role facts 传递和 Service roles-first overload，没有触碰审核状态机。
- 专家并行分析有效区分了三个候选后续切片：Review Gate、Agent Trace/detail、CourseAccess legacy。

## What To Keep

- 对每个 legacy subject-name inference 点，都使用 Bearer `ADMIN/TEACHER` success + `USER sub=admin/teacher_1` denial 矩阵。
- HTTP 主路径先迁移 roles-first，legacy overload 暂留兼容并单独排期清理。
- 对发布/审核类接口优先处理，因为写入或发布影响半径大于只读详情。

## What To Improve

- 当前工作区不是 git repo，后续若要更严格证明边界，需要用户提供 VCS 上下文或导出 diff 工具。
- `ResourceReviewControllerTest` 内 JWT helper 与其他 controller tests 有重复，后续可提取测试工具，但本切片不做避免扩散。

## Skill Extraction

不新增项目 skill。现有 `auth-context-boundary` 与 `object-scope-authorization` 已覆盖本模式。

## Next Candidate Slices

1. P3-4-V：ResourceGeneration detail / Agent Trace governance roles-first RBAC。
2. P3-4-W：CourseAccessService legacy overload 收口和误用防回归测试。
3. P3-4-X：formal OAuth2/JWK/Spring Security dependency/security review 与迁移计划。
