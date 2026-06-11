# Skill: Agent Trace Design

## When to Use

- Implementing agent execution audit
- Adding traceId to AI workflows
- Building trace visibility UI
- Debugging multi-agent runs

## Pattern

Every agent execution must produce a trace record:

```
traceId (UUID)
→ agentName
→ input (sanitized, no secrets)
→ toolCalls[] (name, input, output, duration)
→ modelCalls[] (model, tokens, duration)
→ output (sanitized)
→ status (SUCCESS / FAILED / TIMEOUT)
→ timestamps
```

## Implementation Rules

- traceId generated at workflow entry, propagated via MDC/filter
- Tool calls logged before and after execution
- Model calls log token usage for cost tracking
- Sensitive data stripped before persistence
- Frontend displays trace timeline (read-only)

## Backend Location

- `backend/src/main/java/com/learningos/common/trace/`
- Trace filter sets `X-Trace-Id` response header
- Agent orchestrator writes trace records to `agent_traces` table

## Anti-Patterns

- Logging full prompts with PII
- Skipping trace for "simple" agent calls
- Frontend generating traceId (backend owns this)
