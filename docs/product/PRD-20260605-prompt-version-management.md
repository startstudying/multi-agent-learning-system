# PRD: Prompt Version 管理基础

## 背景

P2-1 要求补齐 Prompt Version 与实验集管理能力。当前数据库已经有 `prompt_version` 表，`agent_trace` 已有 `promptVersion` 字段，但后端缺少实体、Repository、服务和管理 API，导致后续 `model_call_log` 与实验评估无法稳定引用 Prompt 版本。

## 目标

- 提供 Prompt Version 的最小管理能力：创建或 upsert、按 `code/version` 查询、按 `code` 或全量列表查询。
- 按 `V2__learning_agent_loop.sql` 中现有列映射，不新增 migration。
- 为后续 `model_call_log` 关联 Prompt Version 预留服务层查询方法。
- 使用 TDD 覆盖创建、查询和重复 upsert。

## 非目标

- 本轮不修改 `AgentRunRecorder` 或 `model_call_log` 结构，避免与其他 worker 冲突。
- 本轮不新增 evaluation set 数据表；当前用户责任范围只包含 Prompt Version 后端文件。
- 本轮不新增依赖。

## 成功标准

- 管理端可以保存 `code`、`version`、`promptText`、`status`。
- 重复提交同一 `code/version` 时更新原记录而不是创建重复记录。
- 查询不存在的版本返回统一 `NOT_FOUND` 响应。
- 指定测试命令通过。
