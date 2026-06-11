# REQ - 权限与安全加固

## 1. 追踪

- PRD：`docs/product/PRD-20260606-permission-security-hardening.md`
- 需求编号：REQ-20260606-permission-security-hardening

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | 学生画像读取必须校验当前用户与请求 owner 一致。 | 必须 | 越权请求返回 `FORBIDDEN`。 |
| FR-02 | 学习路径创建与读取必须校验当前用户与 owner 一致。 | 必须 | 越权请求返回 `FORBIDDEN` 或 `NOT_FOUND`。 |
| FR-03 | `GET /api/analytics/overview` 仅 admin 可访问。 | 必须 | 非 admin 返回 `FORBIDDEN`。 |
| FR-04 | `GET /api/health` 输出不得暴露密码或敏感 provider 细节给普通调用者。 | 必须 | 响应收敛敏感字段。 |
| FR-05 | RAG `kbIds` 请求必须使用 strict 权限策略。 | 必须 | 混合越权 `kbIds` 请求被拒绝或不返回越权结果。 |
| FR-06 | 必须补充 API / service 安全测试。 | 必须 | MockMvc / Service 测试覆盖越权路径。 |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不引入新依赖。 | 必须 |
| NFR-02 | 不新增数据库结构。 | 必须 |
| NFR-03 | 不暴露 prompt、answer、secret 或原始 provider error。 | 必须 |
| NFR-04 | 保持现有开发态 `X-User-Id` 兼容，但限制高风险接口。 | 必须 |

## 4. 用户流程

### 流程 1：学生访问个人画像 / 路径

```text
学生请求
-> currentUserId 读取
-> 校验 owner
-> 允许则返回
-> 否则 403
```

### 流程 2：RAG 混合 kbIds 查询

```text
请求 kbIds
-> permission filter
-> 若存在越权 kbIds，则拒绝
-> 若全部允许，则继续 retrieval
```

## 5. 输入 / 输出

### 输入

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| `learnerId` | String | 是 | 必须与当前用户一致 |
| `pathId` | String | 是 | 必须属于当前用户 |
| `kbIds` | List<String> | 是 | 必须全部可访问 |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| `FORBIDDEN` | Error | 越权访问 |
| `NOT_FOUND` | Error | 不可感知的资源隐藏 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| 学生读别人画像 | 403 |
| 学生枚举别人路径 | 403/404 |
| admin 读 overview | 允许 |
| 非 admin 读 overview | 403 |
| 混合合法 + 越权 `kbIds` | 403 或安全拒绝 |

## 7. 依赖关系

- 上游依赖：`CurrentUserService`、`PermissionService`、`LearningWorkflowService`、`AnalyticsService`、`HealthService`、`RagQueryService`。
- 下游影响：越权测试和审计日志。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 已执行 |
