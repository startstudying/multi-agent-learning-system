# REQ - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-o-evaluation-rbac.md`
- 需求编号: REQ-20260609-p3-4-o

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | Evaluation Set Controller 必须从 `UserContext.roles()` 计算 `ADMIN/TEACHER` role facts | 必须 | Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 可创建/list |
| FR-02 | Evaluation Run Controller 必须从 `UserContext.roles()` 计算 `ADMIN/TEACHER` role facts | 必须 | Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 可 record/compare |
| FR-03 | Service 管理主路径必须接收显式 `currentUserAdmin/currentUserTeacher` | 必须 | Service 不再通过 `sub=admin` 或 `teacher_` 前缀授予 HTTP 管理权限 |
| FR-04 | Teacher 只能访问自己创建或自己课程范围内的 evaluation set/run | 必须 | Bearer `TEACHER` own set 成功，foreign set 403 |
| FR-05 | Student/User 不得访问 Evaluation 管理面 | 必须 | Bearer `STUDENT sub=admin`、Bearer `USER sub=teacher_1` 均 403 |
| FR-06 | 非管理员 missing 与 foreign evaluation set 不得形成存在性 oracle | 必须 | 同一 teacher 访问 missing 和 foreign detail/compare 均返回 `FORBIDDEN` 且无 `data` |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增依赖、不修改 DB schema | 必须 |
| NFR-02 | 不修改 API path、HTTP method、DTO 字段 | 必须 |
| NFR-03 | 权限失败响应不得包含 foreign object id、title、sample 内容、prompt text、raw output | 必须 |
| NFR-04 | 保持现有 dev/test 无 Bearer `X-User-Id` 兼容测试通过 | 必须 |

## 4. 用户流程

### 流程 1: 管理员记录 evaluation run

```text
ADMIN Bearer token -> POST /api/evaluation-runs -> Service 校验 admin role -> 记录 run -> 返回 OK
```

### 流程 2: 普通用户尝试 subject 混淆

```text
USER Bearer token with sub=teacher_1 -> GET /api/evaluation-sets -> Service 检查 role facts -> FORBIDDEN
```

### 流程 3: 教师访问 foreign/missing set

```text
TEACHER Bearer token -> GET foreign set -> FORBIDDEN
TEACHER Bearer token -> GET missing set -> FORBIDDEN
```

## 5. 输入 / 输出

### 输入

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| `Authorization` | header | Bearer 场景必填 | HS256 JWT，包含 `sub` 和 `roles` |
| `X-User-Id` | header | 否 | 仅无 Bearer 且 dev/test fallback 时使用 |
| `evaluationSetId` | string | run/compare 必填 | 非空 |
| `promptCode` | string | run/compare 必填 | 非空且与 set 匹配 |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | string | `OK` / `FORBIDDEN` / `NOT_FOUND` / `VALIDATION_ERROR` |
| `data` | object/null | 成功时返回业务数据；权限失败不返回 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| Bearer `ADMIN sub=ops_admin` + `X-User-Id=student` | 使用 Bearer roles，允许 |
| Bearer `STUDENT sub=admin` | 不使用 subject 推断，拒绝 |
| Bearer `USER sub=teacher_1` | 不使用 subject 前缀推断，拒绝 |
| Teacher 访问 foreign set | `FORBIDDEN` |
| Teacher 访问 missing set | `FORBIDDEN` |
| Admin 访问 missing set | `NOT_FOUND` |

## 7. 依赖关系

- 上游依赖：P3-4-I `DevAuthFilter` / `CurrentUserService` / `UserContext`。
- 下游影响：Evaluation management API、prompt quality comparison 管理流。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved |

