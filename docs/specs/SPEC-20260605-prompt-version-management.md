# SPEC: Prompt Version 管理基础

## 模块边界

后端模块：`com.learningos.agent`

分层：

- Controller：`PromptVersionController` 只处理 HTTP、参数和响应 envelope。
- Service：`PromptVersionService` 负责 upsert、查询、状态规范化、错误处理。
- Repository：`PromptVersionRepository` 只做 JPA 查询。
- Entity：`PromptVersion` 映射现有 `prompt_version` 表。

## 数据模型

`PromptVersion` 字段：

- `id`
- `code`
- `version`
- `promptText`
- `status`
- `createdAt`

`@PrePersist` 自动填充 `id` 和 `createdAt`。不增加 `updatedAt`，因为当前表没有该列。

## DTO

`PromptVersionUpsertRequest`：

- `code`: required
- `version`: required
- `promptText`: required
- `status`: optional，默认 `ACTIVE`

`PromptVersionResponse`：

- `id`
- `code`
- `version`
- `promptText`
- `status`
- `createdAt`

## API

### POST `/api/agent/prompt-versions`

创建或更新同一 `code/version` 的 Prompt Version。

### GET `/api/agent/prompt-versions`

可选 query `code`。传入时按 code 按创建时间升序列出；未传时列出全部。

### GET `/api/agent/prompt-versions/{code}/{version}`

返回单个版本；不存在时返回 `NOT_FOUND`。

## 状态规则

- 允许 `ACTIVE`、`INACTIVE`、`DRAFT`、`ARCHIVED`。
- 请求未传 `status` 时默认为 `ACTIVE`。
- 状态统一转为大写。

## 架构漂移检查

- Controller 只调用 Service：PASS。
- Service 持有业务规则：PASS。
- Repository 无业务逻辑：PASS。
- 不新增数据库结构：PASS。
- 不引入依赖：PASS。
