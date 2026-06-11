# PRD-20260609 P3-4-K 权限渗透测试矩阵补齐

## 1. 背景

P3-4 已通过多个小切片完成课程读写、对象详情防枚举、RAG 文档元数据、assessment record、analytics student summary、Review Gate 和 Bearer JWT 兼容层收口。但 `backend-architecture-todolist.md` 仍保留“扩展完整权限渗透测试矩阵”未完成项。

当前风险不是某一个端点完全无权限，而是权限证据分散在多个模块测试中，缺少一组跨模块、同一攻击模型下的最小渗透矩阵，尤其是：

- Bearer roles 是否真正贯穿 Controller 入口，而不是只在 filter 单测中成立。
- student / teacher / admin 对 course、answer、RAG document、resource task、review、analytics 的越权响应是否一致。
- 非 admin 对 foreign 与 missing object 是否保持防枚举语义。

## 2. 目标

- 新增 P3-4-K 权限渗透测试矩阵，覆盖最小跨模块不变量。
- 验证 Bearer token 身份优先于 spoofed `X-User-Id`，且 roles 能驱动真实 Controller 权限判断。
- 验证 student 无法写 teacher-managed surface。
- 验证 teacher 无法访问 foreign course scoped surface。
- 验证 student 无法访问 foreign learner scoped surface。
- 验证非 admin missing 与 foreign object 响应不形成存在性 oracle。
- 验证 admin 保留 global read 和 missing `NOT_FOUND` 运维语义。

## 3. 非目标

- 不引入 Spring Security / OAuth2 Resource Server / JWK / JWKS。
- 不新增认证、授权或测试依赖。
- 不新增数据库 migration。
- 不设计正式 class domain / 班级成员模型。
- 不改前端。
- 不重写现有业务权限体系。
- 不宣称 broader class/course 权限模型完成。
- 不覆盖 PromptVersion / Evaluation / 全量 KB 管理权限矩阵；这些保留为后续安全切片。

## 4. 用户价值

- 用跨模块测试把 P3-4 已完成的分散安全收口变成可回归的证据矩阵。
- 降低后续权限改动导致 IDOR、防枚举或 header spoofing 回退的风险。
- 为正式 OAuth2/JWK/Spring Security 迁移保留一组行为基线。

## 5. 验收摘要

- 新增或扩展的权限矩阵测试通过。
- 生产代码只在新增测试揭示明确安全缺陷时做最小修复。
- 不新增依赖、schema、前端改动。
- Evidence / Acceptance 记录 focused、adjacent、full backend verification。
- `backend-architecture-todolist.md` 中“扩展完整权限渗透测试”更新为 P3-4-K 已完成当前 transitional scope，broader class/course 和正式 OAuth2/JWK/Spring Security 仍保留未完成。

## 6. 完成状态

P3-4-K 已在 2026-06-09 完成当前 transitional 权限渗透测试矩阵：

- Bearer roles 业务入口贯通、staging header-only deny、student write deny、RAG document course metadata spoofing deny 均已补齐测试。
- analytics admin-only 入口由 RED 暴露并最小修复为 roles-first admin gate。
- focused、adjacent、full backend Maven verification 均通过。

仍未完成：broader class/course、formal OAuth2/JWK/Spring Security、PromptVersion/Evaluation full RBAC、RAG KB management full matrix。
