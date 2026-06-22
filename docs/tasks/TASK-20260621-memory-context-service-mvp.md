# TASK-20260621 MemoryContextService MVP

## 1. 目标

完成 P0-2 `MemoryContextService MVP`：让 AI QA 在调用 RAG 后，能得到一份低敏、有限、有来源、有注入理由的上下文摘要，并通过现有 `toolCalls` 暴露准备过程。

## 2. 任务类型

后端 Agent/RAG 质量链路增强。

## 3. 技能选择

| Skill | 用途 |
|---|---|
| `feature-development-workflow` | 按 M 级流程执行文档、实现、证据、验收 |
| `executing-plans` | 执行现有落地计划 |
| `test-driven-development` | 先写失败测试，再实现服务 |
| `spring-ai-agent-backend` | 保持 Controller / Service / Repository 分层 |
| `educational-rag-pipeline` | 约束 citation、RAG 来源和无来源状态 |
| `learner-profile-agent` | 低敏画像字段选择和教师笔记隔离 |
| `agent-trace-governance` | 工具调用摘要和可解释上下文准备 |
| `security-review` | 隐私泄露、敏感字段、provider key 过滤 |

## 4. 规模判定

Size: M

原因：

- 新增一个后端服务层能力，并接入 AI QA 响应。
- 不修改 DB schema、依赖、部署拓扑或请求 DTO。
- 有 Agent/RAG/隐私集成风险，需要 SPEC / TASK / CONTEXT 和 focused + adjacent tests。

可跳过：

- 不创建 PRD / REQ / PLAN，因为边界来自既有 roadmap 和 execution plan，需求清晰。
- 不启用并行实现，避免在当前脏工作区扩大文件冲突。

升级触发：

- 若需要新增表、迁移、公开 API 字段、模型网关策略或 verifier/eval gate，则升级到 L 或新 M 切片。

## 5. Context Pack 摘要

完整 Context Pack: `docs/context/CONTEXT-20260621-memory-context-service-mvp.md`

允许修改：

- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/memory/*.java`
- `backend/src/main/java/com/learningos/learning/repository/LearningEventRepository.java`
- `backend/src/main/java/com/learningos/learning/repository/MasteryRecordRepository.java`
- `backend/src/test/java/com/learningos/aiqa/application/memory/*.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- 本任务相关 `docs/specs`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

不允许修改：

- 前端文件。
- DB migration / schema 文件。
- Maven dependency。
- AI provider 配置和密钥。

## 6. TDD 步骤

1. 新增 `MemoryContextServiceTest`，先断言缺失服务导致 RED。
2. 添加 API adjacent 测试，断言 `toolCalls` 包含 `MemoryContextService` 摘要。
3. 实现最小服务和 AI QA 接入。
4. 运行 focused 和 adjacent tests。

## 7. 验收标准

- 每条上下文都有 `source`、`score`、`reason`。
- `TokenBudget` 有上限，长文本或超限列表会标记截断。
- 上下文序列化不包含 raw prompt、provider key、`teacher_note`、完整 excerpt。
- `/api/ai/qa` 响应包含 `MemoryContextService` 工具调用摘要。

## 8. 证据

- RED: `mvn --% -Dtest=MemoryContextServiceTest test` 首次失败，原因是 `MemoryContextService` / `MemoryContext` / repository page query 方法不存在。
- GREEN: `mvn --% -Dtest=MemoryContextServiceTest test` 通过，1 run, 0 failures, 0 errors。
- Adjacent: `mvn --% -Dtest=AiQaControllerTest,RagQueryServiceTest test` 通过，26 run, 0 failures, 0 errors。
- 证据文件：`docs/evidence/EVIDENCE-20260621-memory-context-service-mvp.md`。

## 9. Acceptance Verdict

PASS。

验收文件：`docs/acceptance/ACCEPT-20260621-memory-context-service-mvp.md`。

P0-2 已完成：AI QA 现在会构建低敏、有限、有来源/分数/注入理由的 `MemoryContext`，并通过现有 `toolCalls` 暴露上下文准备摘要。
