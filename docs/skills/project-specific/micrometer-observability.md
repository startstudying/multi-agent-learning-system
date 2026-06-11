# Micrometer Observability

## Use When

Use this skill when implementing or reviewing backend Micrometer metrics, request logging metrics, RAG runtime metrics, model-call metrics, token/cost summaries, or Actuator exposure for observability.

## Core Rules

1. Keep all metric names and tag policy behind `LearningOsMetrics` or a similar adapter.
2. Use low-cardinality tags only.
3. Allowed HTTP tags: `method`, `route`, `status`, `error_code`.
4. Allowed RAG tags: `strategy`, `outcome`, `no_source`, `replayed`, `error_code`.
5. Allowed model tags: `agent_name`, `provider`, `model`, `status`, `error_code`.
6. Allowed token tags: `model`, `prompt_code`, `token_type`, `currency`.
7. Do not put `traceId`, `userId`, `requestId`, `agentTaskId`, `kbId`, `documentId`, `resourceId`, `question`, `prompt`, `answer`, `excerpt`, `source`, `sourcesJson`, `responseJson`, or raw `errorMessage` into metric tags.
8. Replay paths should increment counts only; they must not contaminate latency metrics.
9. Keep model latency at the gateway boundary and token/cost summaries at the recorder boundary.
10. Expose only `metrics` from Actuator unless another SPEC explicitly widens the surface.

## Testing

- Assert meter names and tag sets through `MeterRegistry`.
- Verify counts, timer records, and summaries separately.
- Include slice tests so optional collaborators do not break `@WebMvcTest`.
- Run full backend tests before claiming completion.

## Anti-Patterns

- Recording metrics directly in Controller code.
- Using metrics as an audit store.
- Tagging user identity, task identity, prompt text, source text, or raw errors.
- Repeating the same tag policy in multiple business services.

