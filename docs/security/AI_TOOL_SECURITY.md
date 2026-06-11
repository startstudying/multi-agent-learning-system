# AI Tool Security

## Tool Design Rules

- Tools expose minimal necessary data to the agent
- Tools enforce permission checks in Service layer
- Tools never bypass authentication
- Tool inputs validated before execution
- Tool outputs sanitized (no secrets, no raw PII)

## Prompt Security

- Permission cannot rely on Prompt instructions
- System prompts must not contain secrets
- User input sanitized before inclusion in prompts
- Sensitive fields stripped from context sent to model

## Agent Loop Safety

- Max tool round trips enforced (configurable per workflow)
- Timeout on agent execution
- Circuit breaker for repeated failures
- Human review gate for high-risk actions (resource publish, grade override)

## RAG Security

- Retrieval enforces document-level permissions
- Context trimmed to prevent data leakage across users
- Citations only show documents user can access
- Embedding/index does not store decrypted secrets

## Trace Security

- Trace records sanitized (no API keys, no full prompts with PII)
- Trace access restricted by role
- Trace retention policy documented

## Frontend Security

- Frontend never holds LLM API keys
- Frontend never calls LLM APIs directly
- All AI requests authenticated via backend
