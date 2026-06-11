# RETRO - Review Gate 课程范围收口

## 1. Feature Summary

完成 Review Gate 课程范围权限收口：`admin` 保持全局审核能力，`teacher` 只能查看和处理自己课程的 review，student/无关用户继续被拒绝。处理了代码审查发现的 reviewId 存在性 oracle，并沉淀对象级权限技能。

## 2. What Went Well

- 课程归属判断放在 `ReviewGovernanceService`，没有把权限逻辑下沉到 Controller 或 Prompt。
- 先用失败测试暴露 teacher missing review 返回 `404` 的对象存在性探测问题，再修复为安全 `FORBIDDEN`。
- `ResourceGenerationControllerTest` 补齐课程归属夹具后，NO_SOURCE 和发布回归能继续覆盖真实权限路径。
- 聚焦、宽回归、全量 Maven 测试均通过。

## 3. What Didn't Go Well

- `listResourceReviews(...)` 当前仍是先查 review 再逐条查 task/course/resource，存在 N+1 和无关数据加载风险。
- 本地 `find-hardcoded-secrets.ps1` 脚本存在字符串解析错误，安全扫描无法完全依赖该脚本。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 对象级权限应沿父资源归属链路授权，并避免 missing/foreign 响应差异形成对象存在性 oracle | Yes | `docs/skills/project-specific/object-scope-authorization.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 子代理审查在实现后发现 reviewId oracle | 在对象级权限任务的 Context Pack 中显式加入 missing-vs-forbidden 验证项 |
| Testing | 主要覆盖 own/foreign/student/admin | 增加 missing id 与 foreign id 对非管理员同类安全错误的默认测试 |
| Documentation | P3-4 权限项较大 | 将 Review Gate 已收口范围和剩余 RBAC 范围分开记录 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 后续将 Review Gate list 改为 scoped repository query 或批量加载，减少 N+1 | Backend | P3-4 生产化权限性能优化 |
| 修复本地 `find-hardcoded-secrets.ps1` 脚本编码/引号问题 | Tooling | 安全扫描工具维护 |
| 扩展 course/class、resource/task、answer record 的 RBAC 矩阵测试 | Backend | P3-4 后续权限矩阵 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] API_MEMORY.md
- [x] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md（本切片无架构基线变更）
