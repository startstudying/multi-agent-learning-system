# Dependency Review：P3-2 real VectorDB adapter minimum integration

## 基本信息

- 日期：2026-06-10
- 范围：后端 RAG 真实 VectorDB adapter 最小集成
- 目标依赖：`org.springframework.ai:spring-ai-qdrant-store`
- 目标版本：由 Spring AI BOM 锁定为 `1.0.8`
- 是否新增运行时外部连接：是，启用后连接 Qdrant gRPC 服务

## 依赖选择

### 采用

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-qdrant-store</artifactId>
</dependency>
```

选择原因：

- 项目当前已经使用 Spring AI BOM `1.0.8`。
- Spring AI 官方 Qdrant 文档提供 `spring-ai-qdrant-store` 与 `QdrantVectorStore` / Qdrant native client 集成方式。
- 非 starter 依赖可以避免 Spring Boot 自动配置意外创建全局 `VectorStore`，更符合项目已有 `VectorIndexAdapter` 端口。

### 不采用

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-vector-store-qdrant</artifactId>
</dependency>
```

不采用原因：

- 本项目已有 `VectorIndexAdapter` 作为 RAG vector port。
- Starter 自动配置可能在测试或默认环境下创建真实连接，破坏“默认 disabled/noop”原则。
- 自动配置属性与项目自有 `learning-os.rag.vector.*` 安全策略不完全一致。

## 官方与本地核验

| 项 | 结果 |
|---|---|
| Spring AI Qdrant docs | 已核验，文档包含 `spring-ai-qdrant-store`、Qdrant client bean、`QdrantVectorStore.builder(...)` |
| Maven resolve：`spring-ai-qdrant-store:1.0.8` | 通过 |
| Maven resolve：`spring-ai-starter-vector-store-qdrant:1.0.8` | 通过，但不采用 |
| Maven resolve：`io.qdrant:client:1.15.0` | 通过，用于确认新版本可解析；本切片仍受 Spring AI 1.0.8 传递版本约束 |
| `spring-ai-qdrant-store:1.0.8` POM | 传递 `io.qdrant:client:1.13.0` |

## 许可证

| Component | License | 结论 |
|---|---|---|
| Spring AI Qdrant store | Apache-2.0 | 可接受 |
| Qdrant Java client | Apache-2.0 | 可接受 |
| Qdrant server | Apache-2.0 / 商业云服务另看云服务条款 | 本切片不绑定云服务 |
| gRPC / protobuf / Netty shaded | Apache-2.0 / BSD-like 组件 | 常规可接受，需安全扫描 |

## 传递依赖风险

专家提示：

- `spring-ai-qdrant-store:1.0.8` 传递 `io.qdrant:client:1.13.0`。
- 该 Qdrant client 可能传递 `grpc-netty-shaded:1.65.1`。
- `grpc-netty-shaded:1.65.1` 可能受 `CVE-2025-55163 / GHSA-prj3-ccx8-p6x4` 影响。

处理策略：

1. 实现后运行 `mvn dependency:tree` 核验最终解析版本。
2. 如果最终解析仍落在受影响版本，优先通过 dependencyManagement 覆盖到安全版本，并运行 focused/adjacent/full 测试。
3. 如覆盖与 Qdrant client 不兼容，则记录风险并将真实 Qdrant smoke / 依赖升级拆为后续阻断项。

## 安全边界

### 默认关闭

- `learning-os.rag.vector.enabled=false` 为默认值。
- 默认或配置缺失时使用 `NoopVectorIndexAdapter`。
- 默认 `mvn test` 不连接外部 Qdrant。

### 启用条件

启用真实 Qdrant adapter 必须满足：

- `learning-os.rag.vector.enabled=true`
- `learning-os.rag.vector.provider=qdrant`
- `host` 非空
- `port` 有效
- `collection-name` 非空

### 凭据

- `api-key` 只能从环境变量、Secret、配置中心注入。
- `application.yml` 仅允许空值或占位符。
- 日志、异常、metadata、query log 不得输出 API key。

### Endpoint / SSRF

- Qdrant host/port 是静态部署配置，不允许来自 HTTP request、prompt、前端参数或数据库业务字段。
- 生产环境应通过网络策略限制后端只能访问指定 Qdrant 服务。
- 本切片不提供 runtime endpoint override。

### Payload 脱敏

VectorDB point payload 只允许：

- `chunkId`
- `kbId`
- `documentId`
- `documentVersion`
- `chunkHash`
- `chunkIndex`

禁止：

- raw chunk content
- question / answer / prompt
- storage bucket / storage key
- user id
- secret / API key
- raw provider response

## 生产约束

- `initialize-schema` 默认 false。
- 生产集合建议预创建，并校验 embedding dimension。
- 本切片不新增 health endpoint；后续可新增 opt-in health/smoke。
- 本切片不新增 Testcontainers 或 Docker 依赖。

## 审查结论

有条件通过：

- 允许引入 `spring-ai-qdrant-store`，但必须走项目自有 `VectorIndexAdapter`。
- 不允许默认外连。
- 不允许 raw content 进入 VectorDB。
- 必须记录并核验 gRPC/Netty CVE 风险。
