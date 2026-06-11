# Deep Health Checks

## Use When

Use this skill when implementing or reviewing backend dependency health checks for database, Redis, object storage, model provider configuration, or other runtime dependencies.

## Core Rules

1. Keep health checks behind an application service; controllers should only return the existing API envelope.
2. Prefer low-cost, bounded probes:
   - Database: `DataSource` connection plus `Connection#isValid(timeoutSeconds)`.
   - Redis: `ping()` only.
   - Object storage: configuration validation and client construction only unless a SPEC explicitly requires I/O.
   - Model provider: configuration state only unless a SPEC explicitly requires a provider call.
3. A single dependency failure must not make `/api/health` return 500.
4. Return stable component statuses such as `UP`, `DOWN`, `CONFIGURED`, `UNCONFIGURED`, and `DISABLED`.
5. Use stable `errorCode` values instead of raw exception messages.
6. Do not expose JDBC URLs, hosts, ports, usernames, passwords, object storage endpoints, buckets, access keys, secret keys, model API keys, base URLs, deployment URLs, tokens, prompts, or raw exceptions.
7. Optional dependencies should default to unconfigured, not to fake local endpoints that create misleading `DOWN` states.
8. Do not add dependencies, database schema, or actuator endpoints without the normal dependency/security workflow.

## Testing

- Cover service-level branches for success, failure, unconfigured, and disabled states.
- Cover controller/envelope behavior for at least one success shape and one dependency failure shape.
- Assert sensitive configuration values and raw exception text are absent from the serialized response.
- Avoid real external ports in tests; use mocks/stubs for dependency failures.
- Run focused health tests and full backend tests before claiming completion.

## Anti-Patterns

- Returning raw `ex.getMessage()` in health details.
- Using request parameters to choose health probe targets.
- Performing Redis writes, MinIO object I/O, or model provider calls in a minimal health endpoint.
- Treating a missing optional dependency the same as a failed configured dependency unless the SPEC says so.
