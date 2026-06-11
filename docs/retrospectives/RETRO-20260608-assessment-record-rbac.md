# Retrospective - P3-4-E Assessment Record RBAC Matrix

## 1. Feature Summary

新增 assessment answer / wrong-question 详情端点，并在 Service 层实现 student / teacher / admin 对象级授权矩阵。非管理员 missing 与 foreign 统一返回 `FORBIDDEN` 且无 `data`，管理员 missing 返回 `NOT_FOUND`。

## 2. What Went Well

- 沿用 `object-scope-authorization` 的防枚举模式，测试用例能直接覆盖 IDOR 风险。
- 复用 P3-4-D 的 `CourseAccessService` 和 active enrollment 作为 teacher 读取学生 assessment 记录的父级授权来源。
- DTO 白名单控制响应面，避免暴露 answer idempotency 快照字段和内部 payload。
- Focused / adjacent / full backend Maven 验证均通过。

## 3. What Didn't Go Well

- `AnswerRecord` 未持久化 `courseId`，teacher 授权需要通过 `questionId -> knowledgePointId -> courseId` 推导，后续大列表场景不适合继续内存/多跳判断。
- `java-security-review` 的硬编码密钥扫描脚本存在编码解析错误，本次只能用 `rg` 兜底扫描。
- `PLAN` 中的部分实现步骤在接管时已完成但未及时勾选，需要收尾阶段统一补齐。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Assessment record detail RBAC: student owner、teacher course + active enrollment、admin global、non-admin anti-enumeration | Yes | `docs/skills/project-specific/object-scope-authorization.md` |
| 通过 `questionId -> knowledgePointId -> courseId` 为历史 assessment 记录补父级 scope | Yes | `docs/skills/project-specific/object-scope-authorization.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 中途接管时需从测试失败历史恢复上下文。 | 在 Evidence 中保留最终测试命令和结果，不记录冗余中间失败日志。 |
| Testing | 详情端点覆盖矩阵充分，但 list 场景仍缺。 | 后续新增 list 前先做 scoped repository/query 设计，避免 N+1 和存在性泄露。 |
| Documentation | `CourseAccessService` 语义在 SPEC 中需保持与实现一致。 | 写 SPEC 时优先引用现有 service 方法名和实际授权链路。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 实现正式 JWT/RBAC，替换 `X-User-Id` 过渡身份。 | Backend | Future P3-4 |
| 设计 answer / wrong-question list 的 course-scoped 查询与分页。 | Backend | Future P3-4 |
| 为 grading evaluation 增加 course scope 或绑定 evaluation set course scope。 | Backend | Future P3-4 / P2 |
| 修复或替换 `java-security-review` 硬编码密钥扫描脚本编码问题。 | Tooling | Future tooling cleanup |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md
- [x] ARCHITECTURE_BASELINE.md（无需修改；本切片无架构基线变更）
