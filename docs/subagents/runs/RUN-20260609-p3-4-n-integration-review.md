# RUN-20260609 P3-4-N 多专家集成评审

## 1. 输入来源

本轮 P3-4-N 下一刀选择使用 L1 并行分析：

| 角色 | 产物 | 结论 |
|---|---|---|
| Security reviewer | `RUN-20260609-p3-4-n-next-security.md` | 推荐 PromptVersion 管理 API roles-first RBAC 与 `promptText` 暴露收口 |
| Backend architect | `RUN-20260609-p3-4-n-next-backend.md` | 推荐 `POST /api/assessment/grading-evaluations` roles-first 局部迁移 |
| Test engineer | `RUN-20260609-p3-4-n-next-test.md` | 推荐 Evaluation Set/Run endpoints roles-first RBAC matrix |

## 2. 冲突点

三位专家均指向真实 P3-4 缺口，但优先级依据不同：

- Security reviewer 按直接安全风险排序：PromptVersion 当前无鉴权，且可写 active prompt、读完整 `promptText`。
- Backend architect 按最小代码迁移和 P3-4-M 延续性排序：GradingEvaluation 是旧 `CourseAccessService` 调用方的最小迁移。
- Test engineer 按可形成完整 Controller-level RED 矩阵排序：Evaluation Set/Run 已有测试夹具，适合成组迁移。

## 3. 集成决策

P3-4-N 采用：

```text
PromptVersion 管理 API roles-first RBAC 与 promptText 暴露收口
```

## 4. 决策理由

1. `PromptVersion` 是管理面全裸露，而 Evaluation / GradingEvaluation 多数路径已有过渡权限，只是 roles-first 不完整。
2. `promptText` 是 AI 系统执行策略资产；未授权写入 active prompt 的 blast radius 高于单个 course-scoped evaluation endpoint。
3. 切片集中在 `agent` 模块的 PromptVersion Controller/Service/DTO/Test，不需要新增依赖、schema、frontend 或正式 OAuth2/JWK。
4. 可复用 P3-4-K/M 已建立的 Bearer role facts、spoofed `X-User-Id`、`USER sub=admin/teacher_1` 防混淆测试模式。
5. 该切片不会关闭整个 P3-4；Evaluation/GradingEvaluation/RAG KB/legacy CourseAccessService callers 继续作为后续切片推进。

## 5. 最终边界

本切片只覆盖：

- `POST /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions/{code}/{version}`

授权策略：

- `POST` 仅允许 `ADMIN` role。
- `GET` 允许 `ADMIN` / `TEACHER` role。
- `promptText` 仅对 `ADMIN` 返回。
- `TEACHER` 读取时只返回 prompt version metadata。
- `STUDENT` / `USER` 禁止读取和写入。
- Bearer token 存在时只相信 token roles，不从 `sub` 字符串推断管理角色。

不纳入：

- Evaluation Set/Run roles-first。
- GradingEvaluation roles-first。
- RAG KB management 权限模型。
- 全量 legacy `CourseAccessService` 调用方迁移。
- formal OAuth2/JWK/Spring Security。

