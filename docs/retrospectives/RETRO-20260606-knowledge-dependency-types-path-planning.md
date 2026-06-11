# Retrospective - Knowledge DAG 依赖类型与路径规划

## 1. Feature Summary

本切片补齐 P1-2 Knowledge DAG 的最小后端能力：创建知识依赖时收敛 `dependencyType`，路径规划只把 `PREREQUISITE` 作为前置依赖，避免 `RELATED` 和 `ADVANCED` 错误锁定学习路径节点。

## 2. What Went Well

- 两个子代理分别核查创建路径和规划路径，结论与本地代码证据一致。
- RED 测试准确暴露了两个根因：任意字符串可保存、非前置边被当作前置边。
- 最小实现没有修改数据库、DTO 字段名、repository 签名或前端。
- 保留了原有 `PREREQUISITE` 路径锁定测试，避免误修成“忽略全部依赖”。

## 3. What Didn't Go Well

- PowerShell 输出中部分中文文档仍显示乱码，后续最好统一终端 UTF-8 设置。
- 依赖创建接口本身仍缺少课程教师权限控制，本切片只记录风险，不扩大范围。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Knowledge DAG 边类型校验 + 规划边过滤 | No | 暂不沉淀，等掌握度阈值和路径节点扩展一起总结 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Subagent | 子代理只读分析，完成后立即关闭 | 保持，避免后台任务长期残留 |
| TDD | 先证明两个目标行为失败，再实现 | 保持 |
| Scope | 严格限制在 P1-2 最小切片 | 保持，后续每次只做一个 TODO 子项 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计课程知识图谱写入权限 | 后续 | P3-4 |
| 实现低掌握度前置知识补救优先级 | 后续 | P1-2 |
| 扩展路径节点推荐原因、预计时长、资源类型和测评绑定 | 后续 | P1-2 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] API_MEMORY.md
- [ ] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md
