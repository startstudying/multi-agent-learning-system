# PRD - 权限与安全加固

## 1. 问题陈述

当前后端存在部分临时身份与范围控制实现，能支撑开发态与部分服务层校验，但 P3-4 需要把最容易被伪造或越权的接口收紧到可验证的 owner / admin / teacher scope。重点对象是 Profile、Learning Path、Analytics overview、Health 暴露和 RAG `kbIds` 查询边界。

本切片不重做完整生产认证体系，不接入新身份提供方，不修改数据库结构。它解决的是已有接口的权限收口和越权测试补强。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 学生 | learner | 只能访问自己的画像、路径、资源和答题记录 |
| 教师 | teacher | 只能访问授权课程/班级数据 |
| 管理员 | admin | 访问治理和运维视图 |

## 3. 用户故事

- 作为学生，我不应通过伪造 `learnerId` 或枚举 `pathId` 读取别人的学习画像和学习路径。
- 作为管理员，我希望能继续查看运维和成本治理视图，但普通用户不能看到全局 analytics overview。
- 作为查询者，我不应通过混合合法与越权 `kbIds` 绕过 RAG 权限过滤。

## 4. MVP 范围

### 纳入范围

- 学生画像接口补齐 owner 校验。
- 学习路径接口补齐 owner 校验。
- analytics overview 限制 admin 访问。
- health 输出收敛敏感环境指纹。
- RAG `kbIds` 使用 strict 权限策略，混合越权请求直接拒绝。
- 为上述路径补端到端安全测试。

### 非目标

- 不实现完整 OAuth2/JWT 生产认证。
- 不新增角色表或课程 RBAC 表。
- 不改 RAG 索引、模型接入或评估逻辑。
- 不引入新依赖。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| 学生 owner 保护 | 100% | profile/path 越权请求返回 403 |
| 全局治理收口 | 100% | analytics overview 仅 admin 可访问 |
| RAG 权限边界 | 100% | 混合越权 `kbIds` 被拒绝或安全审计 |
| Health 指纹收敛 | 100% | 非 ops 视图不暴露敏感配置细节 |

## 6. 用户流程

```text
学生/教师/管理员发起请求
-> 后端读取 currentUserId
-> 执行 owner / admin / teacher scope 校验
-> 失败返回 FORBIDDEN
-> 成功继续业务查询
```

## 7. 依赖关系

- 上游依赖：`CurrentUserService`、`PermissionService`、`LearningWorkflowService`、`AnalyticsService`、`HealthService`、`RagQueryService`。
- 下游影响：所有可见性更强的 API 响应。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| 生产认证方案是否直接接 Spring Security JWT | 后续 P3 安全专项 | 暂不纳入 |
| teacher 的课程/班级授权从何而来 | 后续 RBAC 专项 | 暂不纳入 |

## 9. 审批

| 角色 | 姓名 | 日期 | 状态 |
|---|---|---|---|
| Main Codex | Codex | 2026-06-06 | 已执行 |
