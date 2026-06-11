# SPEC - 功能名称

> 语言：正文使用中文。API 路径、JSON 字段、SQL、状态枚举可保留英文。

## 1. 概述

## 2. 追溯

- PRD：`docs/product/PRD-xxx.md`
- REQ：`docs/requirements/REQ-xxx.md`

## 3. 领域模型

## 4. API 契约

### 端点

```http
POST /api/v1/xxx
```

### 请求

```json
{}
```

### 响应

```json
{}
```

### 错误码

| 错误码 | 说明 | 触发条件 |
|---|---|---|

## 5. 前端交互

## 6. 后端流程

## 7. Agent 工作流

```text
用户请求
→ 编排器
→ Agent A
→ Agent B
→ 评审 Agent
→ 结果
```

## 8. RAG 工作流

## 9. 数据库变更

```sql
-- 表结构定义
```

## 10. 状态流转

```text
PENDING → PROCESSING → DONE / FAILED
```

## 11. 错误处理

## 12. 权限规则

## 13. Trace / 日志

## 14. 测试策略

## 15. 验收清单

- [ ] 功能需求 FR-01 已满足
- [ ] API 契约已验证
- [ ] 权限规则已落实
- [ ] Trace 已记录
- [ ] 测试通过
