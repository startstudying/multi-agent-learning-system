# PRD-20260609 P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. 问题陈述

当前 PromptVersion 管理 API 已具备创建、更新、列表和详情能力，但 HTTP 入口没有接入当前用户上下文，也没有角色权限检查。任意可访问 API 的请求都可能创建或覆盖 prompt version，并读取完整 `promptText`。

Prompt 是 AI 学习系统的执行策略资产。若未授权用户可写 active prompt，可能影响资源生成、RAG 回答、评估与 Agent 行为；若教师或学生可读取完整 prompt，也会扩大内部策略暴露面。

## 2. 目标用户

- 管理员：需要维护 prompt version，并查看完整 prompt 内容。
- 教师：可查看 prompt version 元数据以了解系统治理状态，但不应看到完整 prompt 内容。
- 学生/普通用户：不应访问 PromptVersion 管理面。
- 运维/安全审计人员：需要 Bearer roles-first 的可验证权限矩阵。

## 3. 用户故事

1. 作为管理员，我可以使用 Bearer `ADMIN` role 创建或更新 prompt version，即使请求中带有伪造的 `X-User-Id` 也不受影响。
2. 作为教师，我可以查看 prompt version 列表和详情的元数据，但不能看到完整 `promptText`，也不能修改 prompt。
3. 作为学生或普通用户，即使 Bearer `sub` 看起来像 `admin` 或 `teacher_1`，只要 roles 不是 `ADMIN` / `TEACHER`，就不能访问 PromptVersion 管理面。
4. 作为审计人员，我可以通过 focused 测试证明 PromptVersion 入口不再依赖 subject 字符串提权。

## 4. MVP 范围

- `POST /api/agent/prompt-versions` 增加 roles-first admin-only gate。
- `GET /api/agent/prompt-versions` 增加 roles-first admin/teacher gate。
- `GET /api/agent/prompt-versions/{code}/{version}` 增加 roles-first admin/teacher gate。
- `promptText` 仅对 admin 返回；teacher 响应中不出现该字段。
- 保留 dev/test 无 Bearer 时通过 `X-User-Id=admin/teacher_*` 派生 roles 的兼容路径。
- Bearer 优先级高于 `X-User-Id`，Bearer `USER/STUDENT sub=admin` 不提权。

## 5. 非目标

- 不引入 Spring Security、OAuth2 Resource Server、JWK 或新依赖。
- 不修改 `prompt_version` 表结构。
- 不新增 PromptVersion 审批流、发布流、回滚流。
- 不修改前端页面。
- 不迁移 Evaluation Set/Run、GradingEvaluation、RAG KB 或全量 legacy `CourseAccessService` 调用方。
- 不改变模型调用内部读取 active prompt 的服务能力。

## 6. 成功指标

- PromptVersion Controller focused tests 覆盖 Bearer admin、teacher、student、spoofed header、role confusion。
- Teacher list/detail 响应不包含 `promptText`。
- Student/User 访问 PromptVersion 管理面返回 `FORBIDDEN` 且无 `data`。
- Adjacent auth / agent tests 不回归。
- Full backend Maven test 可通过或有明确环境限制说明。

## 7. 开放问题

- 后续是否需要为 PromptVersion 增加审批/发布/回滚治理流？
- 教师是否应该只看部分 prompt code 的元数据，而不是所有 prompt version 元数据？
- PromptVersion 是否需要与 prompt ownership、课程或租户绑定？本切片不处理。

