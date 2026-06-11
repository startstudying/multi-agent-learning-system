# PRD - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 问题陈述

当前 Evaluation Set / Run 管理 API 已具备基础 teacher/admin 访问控制，但实现仍依赖 `currentUserId` 字符串推断角色，例如 `"admin"`、`"teacher"`、`teacher_` 前缀。Bearer JWT 接入后，这会导致两个方向的问题：

- 合法 Bearer `ADMIN` 但 `sub` 不是 `admin` 时被错误拒绝。
- Bearer `STUDENT/USER` 但 `sub` 是 `admin` 或 `teacher_1` 时可能被错误放行。

这会削弱 P3-4 权限矩阵，并影响 prompt/evaluation 治理数据的管理面安全。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 运维管理员 | `ADMIN` | 全局管理 evaluation set/run，并在 spoofed header 下仍以 token role 为准 |
| 授权教师 | `TEACHER` | 管理自己创建或自己课程范围内的 evaluation set/run |
| 学生/普通用户 | `STUDENT` / `USER` | 不应访问管理面治理数据 |

## 3. 用户故事

- 作为管理员，我希望 Bearer `ADMIN` role 可以访问 Evaluation 管理 API，而不是必须让 `sub=admin`。
- 作为安全负责人，我希望 Bearer token 存在时 `X-User-Id` 不会影响授权。
- 作为教师，我希望可以用真实教师用户 id 访问自己创建的 evaluation set，而不依赖 `teacher_` 命名约定。
- 作为平台维护者，我希望普通用户无法通过 `sub=admin` 或 `sub=teacher_1` 的 token subject 混淆提权。

## 4. MVP 范围

### 纳入范围

- `POST /api/evaluation-sets`
- `GET /api/evaluation-sets`
- `GET /api/evaluation-sets/{setId}`
- `POST /api/evaluation-runs`
- `GET /api/evaluation-runs/comparison`
- Controller 从 `UserContext.roles()` 计算 role facts。
- Service 使用显式 `currentUserAdmin/currentUserTeacher` 授权。
- Bearer spoof / role-confusion / missing-vs-foreign 防回归测试。

### 非目标

- 不引入 Spring Security/OAuth2/JWK。
- 不修改 API path、request DTO、response DTO。
- 不修改数据库 schema。
- 不新增依赖。
- 不扩展到 RAG KB management、GradingEvaluation、全仓库 RBAC。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| Bearer roles-first 覆盖 | 100% P3-4-O 场景通过 | Controller tests |
| Role-confusion 阻断 | `sub=admin/teacher_1` 无 role 不提权 | Controller tests |
| 回归测试 | Focused/adjacent/full Maven 通过 | Maven output |
| API/DB drift | 0 | 文件变更与 SPEC 对照 |

## 6. 用户流程

```text
请求 Evaluation API
-> DevAuthFilter 建立 UserContext
-> Controller 从 roles 计算 admin/teacher facts
-> Service 执行管理面和对象级授权
-> 返回成功数据或安全错误 envelope
```

## 7. 依赖关系

- 依赖：P3-4-I Bearer JWT / `UserContext` 兼容层。
- 依赖：P3-4-M roles-first 授权模式经验。
- 阻塞：无。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| 是否同步引入 formal OAuth2/JWK/Spring Security | 后续 P3-4 切片 | 本切片不做 |

## 9. 审批

| 角色 | 姓名 | 日期 | 状态 |
|---|---|---|---|
| Main Codex | Codex | 2026-06-09 | Approved for implementation |

