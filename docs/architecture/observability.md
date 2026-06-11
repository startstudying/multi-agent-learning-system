# Observability Guide

The current backend exposes lightweight observability surfaces. It does not yet include a full Prometheus/Grafana stack or distributed tracing exporter, but resource generation now writes durable Agent/model evidence, key runtime paths emit Micrometer metrics, and `/api/health` reports minimal deep dependency health.

## Request Tracing

- Every request receives an `X-Trace-Id` response header.
- If the client sends `X-Trace-Id`, the backend reuses it.
- If the client omits it, the backend generates a UUID.
- Application services can read the active trace id through the request trace context.

Recommended operator check:

```powershell
Invoke-WebRequest http://localhost:8080/api/health | Select-Object -ExpandProperty Headers
```

## Health

Use:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

The response is wrapped in the common API envelope and includes:

- `application`
- `database`
- `redis`
- `minio`
- `model`

Health checks are low-cost dependency probes:

- `application`: always `UP`, with sanitized environment metadata.
- `database`: uses the configured `DataSource` and `Connection#isValid(1)`; failures return component `DOWN` while the HTTP envelope remains OK.
- `redis`: returns `UNCONFIGURED` when no host is configured; configured Redis is checked with `ping()` and failures return component `DOWN`.
- `minio`: validates configuration, endpoint shape, and client construction only; it does not read buckets or objects.
- `model`: reports `DISABLED` for `provider=none`, `CONFIGURED` when a non-none provider has a chat or embedding model name, and `UNCONFIGURED` when model names are missing.

Health metadata must remain sanitized. Do not expose JDBC URLs, Redis hosts, MinIO endpoints or buckets, credentials, provider keys, tokens, model deployment URLs, prompts, or raw exception messages.

## RAG Query Evidence

RAG query responses include:

- `traceId`
- `sources`
- citation fields such as `documentId`, `documentName`, `pageNum`, `sectionTitle`, `excerpt`, and `score`

The current RAG implementation also persists query evidence in `kb_query_log`, including trace id, user id, KB ids, question, retrieval count, reranker status, sources, latency, and creation time.

## Agent Trace Evidence

Resource generation persists `agent_task`, `agent_trace`, `model_call_log`, and `token_usage_log` rows. Inspect the task timeline with:

```powershell
Invoke-RestMethod http://localhost:8080/api/agent/tasks/{taskId}/trace
```

Trace steps currently include planner, teacher, resource, question, critic, tutor, and safety stages. Production hardening should add retention rules, dashboards, and export hooks.

## Runtime Metrics

Actuator exposes:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/metrics
```

Current business meters:

- `learningos.http.server.requests`
- `learningos.http.server.failures`
- `learningos.rag.query.duration`
- `learningos.rag.query.count`
- `learningos.rag.retrieval.count`
- `learningos.rag.citation.count`
- `learningos.rag.query.failures`
- `learningos.model.call.duration`
- `learningos.model.call.failures`
- `learningos.token.usage`
- `learningos.token.cost`

Metric tags are intentionally low-cardinality. They must not include `traceId`, `userId`, `requestId`, `agentTaskId`, `kbId`, `documentId`, raw questions, prompts, sources, excerpts, or raw error messages.

## Metrics And Logs Roadmap

The next observability pass should add:

- Retention and aggregation jobs for `agent_trace`, `model_call_log`, and `token_usage_log`.
- Dashboard panels for health status, error rate, RAG citation count, Agent task state, and assessment replan events.
- Alert thresholds for dependency unavailability, high error rate, missing citations, and model timeout spikes.
