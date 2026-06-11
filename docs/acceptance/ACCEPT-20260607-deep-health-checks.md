# ACCEPT - 深度健康检查
状态：已通过（2026-06-07）

## 验收结论

P3-5 深度健康检查切片已完成，验收通过。

## 验收项

- [x] `/api/health` 继续返回统一 `ApiResponse` envelope。
- [x] 响应包含 `application`、`database`、`redis`、`minio`、`model` 五个组件。
- [x] application 固定返回 `UP`，environment 做安全归一化。
- [x] database 基于 `DataSource` 做轻量连接探测，成功 `UP`，失败 `DOWN`。
- [x] Redis 未配置 host 时 `UNCONFIGURED`，ping 成功 `UP`，失败 `DOWN`。
- [x] MinIO 配置完整且 endpoint/client 可构造时 `CONFIGURED`，配置不完整 `UNCONFIGURED`，配置异常 `DOWN`。
- [x] model provider 为 `none` 时 `DISABLED`，provider 非 `none` 且模型名存在时 `CONFIGURED`，缺少模型名时 `UNCONFIGURED`。
- [x] 组件失败不会导致 `/api/health` 返回 500。
- [x] 响应不包含 JDBC URL、Redis host/password、MinIO endpoint/bucket/key、secret、token、password 或 raw exception。
- [x] 未新增 Maven dependency。
- [x] 未新增数据库 schema 或 migration。
- [x] 未修改 `/api/health` 路径。
- [x] 定向测试和全量后端测试通过。

## 证据关联

- 见 [EVIDENCE-20260607-deep-health-checks.md](../evidence/EVIDENCE-20260607-deep-health-checks.md)
- 见 [TASK-20260607-deep-health-checks.md](../tasks/TASK-20260607-deep-health-checks.md)

