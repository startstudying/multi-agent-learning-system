# EVIDENCE-20260621 记忆系统与回答质量路线图证据

## 1. Evidence Type

Docs-only research and planning evidence。

## 2. Inputs Reviewed

- 用户附件：Agent 记忆系统框架。
- 用户附件：Answer Quality System 框架。
- 项目 workflow 与 AGENTS 规则。
- 项目 memory、architecture、skills、subagent registry。
- 本地 AI QA、RAG、Model Gateway、Learning Workflow、Orchestrator、chat session/message 相关代码。
- Subagent reports：
  - `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-external-research.md`
  - `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-security-quality.md`

## 3. Key Findings

1. 当前系统基础强，但组合不足：RAG、Trace、Evaluation、PromptVersion、ModelGateway、ProfileSnapshot 都存在，但没有统一 `QaRuntime`。
2. `/api/ai/qa` 仍偏 RAG wrapper，没有完整使用记忆、工具、verifier、eval feedback。
3. `answerMode` 当前缺少真实策略含义，不应只映射 `low/medium/high`。
4. `kb_chat_session` / `kb_chat_message` 当前不能支撑多轮高质量会话状态。
5. 长期记忆扩展前必须先处理 RAG query log、citation excerpt、profileSnapshot、trace retention 的敏感持久化风险。
6. 回答质量应通过 eval dataset、grader、trace、badcase 闭环提升，不能靠堆 prompt。

## 4. Documents Created

- `docs/product/PRD-20260621-memory-answer-quality-roadmap.md`
- `docs/requirements/REQ-20260621-memory-answer-quality-roadmap.md`
- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-roadmap.md`
- `docs/tasks/TASK-20260621-memory-answer-quality-roadmap.md`
- `docs/context/CONTEXT-20260621-memory-answer-quality-roadmap.md`
- `docs/evidence/EVIDENCE-20260621-memory-answer-quality-roadmap.md`
- `docs/acceptance/ACCEPT-20260621-memory-answer-quality-roadmap.md`

## 5. Verification Performed

- Verified target roadmap files did not already exist before creation.
- Verified subagent reports existed.
- No backend/frontend code changed.

## 6. Tests Run

No automated tests were run because this task is docs-only and does not change runtime code.

## 7. Residual Risks

- OpenAI official API details can change; implementation slices should re-check official docs.
- Some terminal output displayed encoding noise, but source artifact intent and structure were incorporated.
- Security findings require actual code slices to reduce risk; this roadmap alone does not remediate runtime behavior.
