# Retrospective - P3-4 权限与安全加固

## 1. Feature Summary

完成 P3-4 最小安全收口：Profile / Learning Path owner 校验、analytics overview admin-only、Health 公开输出收敛、RAG mixed `kbIds` strict 拒绝，并补齐红绿回归测试和全量后端回归。

## 2. What Went Well

- 先补测试并观察 6 个预期失败，避免直接修改代码导致测试无效。
- 子专家报告明确指出旧测试夹具与新 owner 规则冲突，减少返工。
- RAG strict 校验发生在 retrieval/logging 之前，拒绝时不会写 query/citation 伪成功证据。
- 代码审查发现 `GET /api/rag/query` 缺少 handler，补齐后用 `ChatControllerTest` 做了红绿复核。
- 全量 `mvn test` 暴露了一个旧测试仍期待 raw provider error，本轮顺手把它对齐到既有 `MODEL_PROVIDER_ERROR` 安全合同。

## 3. What Didn't Go Well

- 本地 hardcoded-secret skill 脚本有语法错误，无法作为自动扫描证据。
- 项目配置仍保留本地开发默认凭据，虽然不是本轮新增，但需要在生产安全专题中统一治理。
- 当前 P3-4 仍是最小切片，完整 RBAC 与所有资源类型的权限矩阵还未完成。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 后端最小权限收口：owner/admin/strict resource list/health redaction + TDD 红绿证据 | Yes | 暂不新增本地 skill；后续做完整 RBAC 时再沉淀为项目级权限治理 skill |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | Context Pack 允许文件较窄 | 后续安全切片在 Context Pack 中显式列出 evidence、acceptance、retro、memory、changelog、skill extraction 文件 |
| Testing | 聚焦测试和全量测试均可运行 | 保持 red-green 记录，并在 evidence 中区分聚焦测试与全量回归 |
| Security scan | hardcoded-secret skill 脚本不可用 | 修复本地 skill 脚本，或在项目内新增可维护的 secret scan 命令 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 修复或替换 hardcoded-secret 扫描脚本 | 后续安全专题 | P3-4 后续 |
| 设计完整生产认证 / RBAC / 课程班级授权模型 | 后续安全专题 | P3-4 后续 |
| 扩展 taskId/resourceId/answer/course-scope 权限渗透测试 | 后续安全专题 | P3-4 后续 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [ ] SKILL_REGISTRY.md（本轮未新增 project-specific skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构基线变更）
