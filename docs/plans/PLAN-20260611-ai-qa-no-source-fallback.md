# AI QA 无资料降级回答计划

## Skill Selection

| Skill | 用途 |
|---|---|
| feature-development-workflow | 按项目强制流程执行 |
| educational-rag-pipeline | 保持 RAG 来源和 no-source 边界 |
| model-gateway-boundary | 后端拥有问答策略和安全摘要 |
| api-contract-design | 新增兼容响应字段 |
| vue3-component-design | 调整学生端状态展示 |
| test-generator | 增加前后端回归测试 |

## Size Classification

Size: M

原因：改动用户可见回答语义，并新增 `/api/ai/qa` 响应字段；涉及后端合同和前端展示，但不改数据库、依赖或纯 RAG 接口。

## Subagent Decision

Use Subagents: No

原因：影响文件少、边界清晰，单 Codex 可以完成分析、实现和测试。

## GitHub Reference

不需要。该任务复用项目现有 AI QA/RAG 架构，不引入外部库或未知模式。

## 实施步骤

1. 更新 AI QA DTO，新增 `sourceStatus` 和 `sourcePolicy`。
2. 更新 `AiQaService`，在 no-source RAG 结果下生成通用降级回答。
3. 更新 `QaModePolicy` 安全摘要，区分课程资料回答和通用降级回答。
4. 更新学生端类型和状态展示。
5. 增加后端 controller 测试和前端组件测试。
6. 运行 focused backend/frontend 测试和 build。

## 风险

- 如果未来接入真实模型，需要把当前规则化通用回答替换为 model gateway 调用，并保留相同来源状态合同。
- 纯 RAG no-source 不能被误改，否则会影响 RAG 评测和引用治理。
