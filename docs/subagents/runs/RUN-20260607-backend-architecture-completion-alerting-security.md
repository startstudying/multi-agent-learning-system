# RUN-20260607 backend architecture completion - alerting security

## 任务

P3-5-A 告警 API 权限、脱敏、安全测试只读分析。

## 结论

告警 API 是跨用户、跨课程、跨 Agent 的治理视图，必须 admin-only。

当前项目身份模型仍是临时 `X-User-Id` 字符串模型，因此只能声明：

```text
X-User-Id: admin
```

不是完整 JWT/RBAC。

## 权限要求

- `admin` 可访问。
- `teacher`、`student`、缺失 header 时的 `dev_user` 均返回 `FORBIDDEN`。
- 权限必须在后端代码中执行，不能依赖前端菜单、prompt 或 response 过滤。

## 脱敏要求

告警 response 必须是白名单 DTO，不能直接序列化 entity，不能复用已有包含敏感字段的 `AbnormalModelCall`。

禁止 response 出现：

- prompt 正文
- RAG 原始问题、答案、chunk、source excerpt
- `responseJson` / `sourcesJson`
- `markdownContent`
- `errorMessage`
- `citationCheck`
- `reason`
- `revisionSuggestion`
- raw provider error
- secret / token / connection string / API key

## 测试建议

- 非 admin 访问 `/api/analytics/ops/alerts` 返回 `403 / FORBIDDEN`。
- admin 可读取告警 summary。
- 响应不包含敏感字段名和敏感 seed 字符串。
- 阈值等于边界时触发：`latencyMs >= threshold`、`ageHours >= thresholdHours`。
- 非法参数返回 `VALIDATION_ERROR`。

## Dependency / GitHub Research

不需要。

原因：

- 不新增依赖。
- 不改 schema。
- 现有 Spring MVC、JPA、Jackson、MockMvc 足够完成本切片。
