# API Reference

All backend responses use:

```json
{ "code": "OK", "message": "OK", "data": {} }
```

Every HTTP request receives `X-Trace-Id`.

Validation and application errors use the same envelope with `code`, `message`, and omitted or null `data`.

## Health

- `GET /api/health`

Returns application, database, Redis, MinIO, and model configuration status.

## Knowledge Base And RAG

### `POST /api/users`

Creates an application user.

### `GET /api/users/{userId}`

Returns one application user.

### `POST /api/courses`

Creates a course.

### `GET /api/courses`

Lists courses.

### `GET /api/courses/{courseId}`

Returns one course.

### `POST /api/courses/{courseId}/chapters`

Creates a chapter.

### `POST /api/knowledge-points`

Creates a knowledge point.

### `POST /api/knowledge-dependencies`

Creates a prerequisite/dependency edge.

### `GET /api/courses/{courseId}/knowledge-graph`

Returns graph data for course knowledge points and dependencies.

### `POST /api/knowledge-bases`

Creates a knowledge base for the current `X-User-Id` user.

```json
{
  "name": "Java backend course",
  "description": "Spring Boot and database notes",
  "visibility": "PRIVATE"
}
```

### `GET /api/knowledge-bases`

Lists knowledge bases accessible to the current user.

### `POST /api/knowledge-bases/{kbId}/documents`

Uploads a course document as `multipart/form-data`.

Fields:

- `file`
- `courseId` optional
- `chapterId` optional

Returns `documentId`, `indexTaskId`, and index task `status`.

### `GET /api/documents/{documentId}`

Returns document parse and index status.

### `POST /api/documents/{documentId}/reindex`

Creates a new index task for an existing document.

### `POST /api/rag/query`

```json
{
  "kbIds": ["kb_java_backend"],
  "question": "Why does my SQL JOIN duplicate rows?",
  "topK": 20
}
```

RAG query responses include `answer`, `sources`, and `traceId`. Retrieval code must filter by allowed knowledge-base ids before returning chunks.

### `GET /api/chat/sessions/{sessionId}/stream`

Returns Server-Sent Events:

- `status`
- `token`
- `done`

### `POST /api/tutor/ask`

```json
{
  "question": "Why does my SQL JOIN duplicate rows?",
  "kbIds": ["kb_java_backend"],
  "topK": 5
}
```

Returns a permission-filtered, citation-grounded tutor answer.

### `GET /api/tutor/sessions/{sessionId}/stream`

Returns tutor Server-Sent Events:

- `status`
- `token`
- `done`

## Learning

### `POST /api/profile/dialogue/extract`

```json
{
  "learnerId": "stu_001",
  "message": "I am learning Spring Boot and struggle with SQL JOINs."
}
```

Returns a profile draft, follow-up questions, a reason summary, and `traceId`.

### `POST /api/learning-paths`

```json
{
  "learnerId": "stu_001",
  "goalId": "goal_java_backend"
}
```

Returns the generated path plus `profileSnapshot`.

### `GET /api/learning-paths/{pathId}`

Learning paths include traceable reasons per path node and the saved `profileSnapshot` used for planning.

## Agent And Resources

### `POST /api/orchestrator/workflows`

Creates an executable Orchestrator workflow for `RESOURCE_GENERATION`, `RAG_QA`, or `ANSWER_SUBMISSION`. Workflow responses include ordered `steps`, `recentFailedStep`, `traceSummary`, `nextActions`, and node contract fields on each step: `inputDto`, `outputDto`, `failurePolicy`, `retryPolicy`, and `retryable`.

### `GET /api/orchestrator/workflows/{workflowId}`

Returns the current workflow status for the owner. Failed resource generation workflows can show `RETRY_WORKFLOW`; failed RAG QA and answer submission workflows show `RESUBMIT_ORIGINAL_REQUEST`.

### `POST /api/orchestrator/workflows/{workflowId}/retry`

Retries owner-owned `FAILED RESOURCE_GENERATION` workflows and returns a new workflow with `retryOfWorkflowId`.

### `POST /api/resources/generation-tasks`

```json
{
  "learnerId": "stu_001",
  "goalId": "goal_java_backend",
  "pathNodeId": "node_sql_join",
  "resourceTypes": ["LECTURE", "EXERCISE"]
}
```

Returns the resource generation task plus `profileSnapshot` and recovery metadata: `retryCount`, `nextRetryAt`, `lastError`, and `recoverable`. Resource drafts expose a `citationSummary` that starts with `COURSE_RAG` when task-level source citations are persisted, or `NO_SOURCE` when the draft has no usable source and must remain under review.

### `GET /api/resources/generation-tasks/{taskId}`

Returns the generation task, generated resource drafts, the saved `profileSnapshot` used for resource generation, and recovery metadata. Recoverable model failures expose `status = FAILED`, `retryCount = 1`, `lastError = MODEL_CALL_FAILED`, `recoverable = true`, and `nextRetryAt`.

### `GET /api/agent/tasks/{taskId}/trace`

Generated resources remain in `PENDING_CRITIC` until reviewed.

### `GET /api/reviews/resources`

Optional `status` query parameter filters review rows, for example `PENDING_CRITIC`.

### `POST /api/reviews/resources/{reviewId}/decision`

```json
{
  "decision": "REVISION_REQUESTED",
  "summary": "Add stronger citations before approval."
}
```

Allowed decisions are `APPROVED`, `REVISION_REQUESTED`, and `REJECTED`. `NO_SOURCE` resources cannot be directly approved and return `409 CONFLICT`.

## Assessment

### `POST /api/assessment/answers`

```json
{
  "learnerId": "stu_001",
  "questionId": "q_sql_join",
  "answer": "JOIN duplicates often come from one-to-many matches."
}
```

Submitting an answer returns grading, mastery updates, feedback id, replan record id, and trace id.

## Operations

### `GET /api/analytics/overview`

Returns agent task counts, model call counts, token usage totals, answer and wrong-question counts, learning event counts, and resource review status counts.
