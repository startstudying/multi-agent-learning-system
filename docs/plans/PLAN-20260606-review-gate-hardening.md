# Review Gate 强约束计划

## Skill Selection Report

## Task Type

后端安全/治理优化，属于 P0-4 Review Gate 强约束。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 保持 AI 学习系统后端分层和资源审核方向 |
| `critic-review-agent` | 审核状态、发布门禁、学生可见性 |
| `test-driven-development` | 先用失败测试固定未审核正文泄漏 |
| `multi-agent-coder` / `dispatching-parallel-agents` | 用户要求启动多 subagent 并行开发 |
| `verification-before-completion` | 完成前必须用测试证据证明 |

## Missing Skills

无。

## GitHub Research Needed

No。本轮使用用户已给出的 LMS / Spring AI 参考方向，不新增依赖，不复制外部代码。

## New Project-Specific Skill To Create

暂不需要。

## Subagent Decision

Use Subagents: Yes。

Reason: 用户明确要求多 subagent 并行；当前总 TODO 涉及 RAG、Review Gate、Orchestrator 多个独立后端工作流。

Parallelism Level: L1 并行分析。

Selected Subagents:

- Sartre：P0-3 RAG 文档上传/索引/RAG 查询幂等与恢复审查。
- Fermat：P0-4 Review Gate 绕过路径安全审查。
- Epicurus：P0-1 Orchestrator Workflow 剩余项架构审查。

Implementation Mode: Single Codex implementation。本轮主线程只实现 P0-4 最小切片，避免并行编辑冲突。

## 执行步骤

1. 读取现有资源生成、审核服务和测试。
2. 创建 PRD/REQ/SPEC/PLAN/TASK/Context Pack。
3. 写失败测试：未审核任务详情和创建响应不应返回正文。
4. 在 `ResourceGenerationService` 服务层根据 `canReleaseToLearner` 屏蔽正文。
5. 审核通过后确认正文恢复可见。
6. 运行聚焦测试和全量测试。
7. 更新 TODO、Evidence、Acceptance、Memory、Changelog。

## 风险

- 任务详情接口可能被前端用于预览草稿；本轮按“学生端不可见”优先处理，教师审核详情后续单独补。
- `GeneratedResourceResponse` 加空字段不序列化后，客户端需要以字段缺失表示未发布正文。
- 没有角色系统，无法区分 teacher/admin 详情视图；P3-4 权限加固时需要补齐。
