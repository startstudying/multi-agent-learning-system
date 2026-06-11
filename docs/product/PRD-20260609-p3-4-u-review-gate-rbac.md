# PRD - P3-4-U Review Gate ResourceReview roles-first RBAC

## 背景

P3-4 已逐步把 Course、RAG KB、Assessment、LearningPath、ResourceGeneration create、Orchestrator ResourceGeneration create/retry 等路径迁移到 Bearer roles-first。Review Gate 审核接口仍通过用户 subject 字符串推断 `admin` / `teacher`，与当前认证上下文边界不一致。

## 目标

在不改变 API 合同和数据库 schema 的前提下，使资源审核列表与审核决策接口只信任 `UserContext.roles()` 中的显式角色事实。

## 用户价值

- 防止 Bearer `USER sub=admin` 获得审核发布权限。
- 防止 Bearer `USER sub=teacher_1` 仅凭 subject 与课程教师 id 相同获得审核权限。
- 保留 Bearer `ADMIN` 与 `TEACHER` 的合法审核能力。
- 保持非管理员 missing/foreign review 的安全 `FORBIDDEN` 语义。

## 非目标

- 不迁移 formal OAuth2/JWK/Spring Security。
- 不修改审核状态机、发布规则、NO_SOURCE 规则。
- 不修改 ResourceGeneration create/detail/trace/cancel。
- 不清理全仓 legacy overload。
- 不新增依赖、DB schema 或 frontend 变更。
