# PRD - 深度健康检查
状态：已完成（2026-06-07）

## 1. 背景

P3-5 可观测性与运维已经完成结构化请求日志和 Micrometer 运行指标，但 `/api/health` 仍主要报告配置是否存在。当前健康检查可以说明数据库、Redis、MinIO、模型 provider 的配置状态，却不能证明数据库连接可用、Redis 可达、对象存储配置是否能建立客户端、模型 provider 是否处于可调用或显式禁用状态。

本切片补齐最小深度健康检查，服务于本地运行、部署前检查和运维诊断。实现必须保持安全脱敏：健康响应不能泄露 JDBC URL、Redis host、MinIO endpoint/bucket/key、模型 provider key/base URL、token、password。

## 2. 目标

- 数据库健康从“配置存在”升级为轻量连通性探测。
- Redis 健康区分配置缺失、连接成功、连接失败。
- MinIO 健康区分配置缺失、客户端可构造、配置异常；不做高成本对象读写。
- 模型 provider 健康区分 `none` 禁用、配置存在、配置缺失；本切片不真实调用外部模型。
- 健康响应使用稳定状态和安全 metadata，便于前端/运维读取。
- 不新增依赖、不修改数据库、不修改业务 API 路径。

## 3. 范围

纳入：

- 扩展 `HealthDtos.ComponentStatus`，支持健康状态和安全 metadata。
- 增强 `HealthService` 的数据库、Redis、MinIO、模型 provider 判断。
- 保留 `/api/health` 路径和统一 `ApiResponse` envelope。
- 补充健康检查测试，验证状态语义和敏感信息不泄露。
- 更新 evidence、acceptance、memory、changelog 和 TODO。

不纳入：

- Spring Boot Actuator 自定义 `HealthIndicator`。
- 外部模型 provider 的真实 API 探测。
- MinIO bucket existence 或对象读写探测。
- Redis 写入型探测。
- 告警系统、Dashboard、Prometheus/Grafana。

## 4. 用户价值

- 开发者能快速判断本地后端是否连上数据库。
- 运维能看到依赖是 `UP`、`DOWN`、`UNCONFIGURED` 还是 `DISABLED`。
- 安全审查能确认健康接口不会泄露连接字符串、凭据和内部 endpoint。
- 后续告警和部署检查可以复用同一健康语义。

## 5. 成功标准

- `/api/health` 返回 application/database/redis/minio/model 五个组件。
- 数据库可执行轻量探测时为 `UP`，失败时为 `DOWN`。
- Redis 配置存在且连接工厂可连通时为 `UP`，失败时为 `DOWN`，未配置时为 `UNCONFIGURED`。
- MinIO 配置完整且客户端构造成功时为 `CONFIGURED`，配置不完整时为 `UNCONFIGURED`，配置异常时为 `DOWN`。
- 模型 provider 为 `none` 时为 `DISABLED`；provider 非 `none` 且模型名存在时为 `CONFIGURED`。
- 响应不包含敏感字段值。
- 定向测试和全量后端测试通过，或在 Evidence 中记录限制。
