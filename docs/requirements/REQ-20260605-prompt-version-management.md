# REQ: Prompt Version 管理基础

## 功能需求

1. 支持通过 API 创建或更新 Prompt Version。
2. 支持按 `code/version` 查询单个 Prompt Version。
3. 支持按 `code` 列出版本；未传 `code` 时列出全部版本。
4. 服务层提供 `findActiveByCode(String code)`，为后续模型调用绑定当前激活 Prompt Version 预留。
5. 请求字段使用实际表列：`code`、`version`、`promptText`、`status`。

## 数据需求

`prompt_version` 表实际列：

- `id varchar(80) not null`
- `code varchar(120) not null`
- `version varchar(80) not null`
- `prompt_text text not null`
- `status varchar(40) not null`
- `created_at datetime(6) not null`
- unique key `(code, version)`

当前表没有 `description`、`template`、`hash`、`is_active` 独立列。本轮用 `promptText` 对应模板内容，用 `status=ACTIVE` 表示激活状态。

## API 需求

- `POST /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions?code={code}`
- `GET /api/agent/prompt-versions/{code}/{version}`

所有响应使用统一 envelope：`{ "code": "OK", "message": "OK", "data": ... }`。

## 质量需求

- TDD：先写测试并运行失败，再实现。
- 覆盖服务层和 Controller 层。
- 不新增 migration、不新增依赖。
