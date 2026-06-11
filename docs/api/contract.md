# API Contract

All API responses use:

```json
{ "code": "OK", "message": "OK", "data": {} }
```

Every backend response includes `X-Trace-Id`.

## Frontend Routes

- `/`: student learning loop.
- `/teacher/reviews`: critic and teacher review queue.
- `/admin/operations`: operations and governance dashboard.

The frontend talks only to backend APIs. It must not hold model provider keys.

## Knowledge Base And RAG

### `POST /api/users`

Creates an application user.

```json
{
  "username": "alice",
  "displayName": "Alice",
  "roleCodes": ["STUDENT"]
}
```

### `GET /api/users/{userId}`

Returns one application user.

### `POST /api/courses`

Creates a course catalog record.

### `GET /api/courses`

Lists courses.

### `GET /api/courses/{courseId}`

Returns one course.

### `POST /api/courses/{courseId}/chapters`

Creates a chapter under a course.

### `POST /api/knowledge-points`

Creates a knowledge point under a chapter.

### `POST /api/knowledge-dependencies`

Creates a directed knowledge dependency edge.

### `GET /api/courses/{courseId}/knowledge-graph`

Returns course chapters, knowledge points, and dependency edges for planner use.

### `POST /api/knowledge-bases`

```json
{
  "name": "Java backend course",
  "description": "Spring Boot and database material",
  "visibility": "PRIVATE"
}
```

### `GET /api/knowledge-bases`

Lists knowledge bases accessible to the current user.

### `POST /api/knowledge-bases/{kbId}/documents`

Multipart fields:

- `file`
- `courseId`, optional
- `chapterId`, optional

Returns `documentId`, `indexTaskId`, and `status`.

### `GET /api/documents/{documentId}`

Returns parse and index status.

### `POST /api/documents/{documentId}/reindex`

Creates a pending index task.

### `POST /api/rag/query`

```json
{
  "kbIds": ["kb_java_backend"],
  "question": "Why does SQL JOIN duplicate rows?",
  "topK": 5
}
```

Returns `answer`, `sources`, and `traceId`. Backend retrieval must permission-filter KB ids before reading chunks.

### `GET /api/chat/sessions/{sessionId}/stream`

Query parameters:

- `question`
- repeated `kbIds`

Emits `status`, `token`, and `done` SSE events.

### `POST /api/tutor/ask`

```json
{
  "question": "Why does SQL JOIN duplicate rows?",
  "kbIds": ["kb_java_backend"],
  "topK": 5
}
```

Returns a RAG-grounded tutor answer with citations and a trace id. The tutor path reuses backend RAG permission filtering.

### `GET /api/tutor/sessions/{sessionId}/stream`

Query parameters:

- `question`
- repeated `kbIds`

Emits tutor `status`, `token`, and `done` SSE events with citations and trace id.

## Learning

### `POST /api/profile/dialogue/extract`

```json
{
  "learnerId": "stu_001",
  "message": "I am learning Spring Boot and struggle with SQL JOINs."
}
```

Returns a profile draft, follow-up questions, reason summary, and trace id.

### `POST /api/learning-paths`

```json
{
  "learnerId": "stu_001",
  "goalId": "goal_java_backend"
}
```

Returns path id, nodes, mastery values, reasons, trace id, and `profileSnapshot`.

### `GET /api/learning-paths/{pathId}`

Returns a previously generated learning path, including the saved `profileSnapshot`.

## Resource Generation And Agent Trace

### `POST /api/orchestrator/workflows`

Creates a unified orchestrator workflow envelope for executable AI learning-loop actions such as resource generation, RAG QA, and answer submission.

```json
{
  "workflowType": "RESOURCE_GENERATION",
  "learnerId": "stu_001",
  "payloadJson": "{\"goalId\":\"goal_java_backend\",\"pathNodeId\":\"kp_sql_join\"}",
  "requestId": "req_resource_1"
}
```

Returns `workflowId`, `workflowType`, `agentTaskId`, `traceId`, `status`, ordered `steps`, `recentFailedStep`, `traceSummary`, and `nextActions`.

Each item in `steps` and `recentFailedStep` also exposes node contract metadata:

- `inputDto`
- `outputDto`
- `failurePolicy`
- `retryPolicy`
- `retryable`

### `GET /api/orchestrator/workflows/{workflowId}`

Returns the current workflow status context for the current user, including the associated Agent task, trace summary, recent failed step, ordered steps, and continuable actions. Missing or inaccessible workflow ids return `NOT_FOUND` using the common API envelope.

`FAILED RESOURCE_GENERATION` returns `nextActions` with `RETRY_WORKFLOW`. `FAILED RAG_QA` and `FAILED ANSWER_SUBMISSION` return `RESUBMIT_ORIGINAL_REQUEST` because their workflow envelopes intentionally store only sanitized snapshots.

### `POST /api/orchestrator/workflows/{workflowId}/retry`

Retries a failed workflow when the current user owns the workflow and the workflow type is `RESOURCE_GENERATION`. Successful retry creates a new workflow and returns `retryOfWorkflowId` pointing to the original failed workflow. Non-owner retry returns `NOT_FOUND`; non-failed or non-resource workflows return `CONFLICT`.

### `POST /api/resources/generation-tasks`

```json
{
  "learnerId": "stu_001",
  "goalId": "goal_java_backend",
  "pathNodeId": "kp_sql_join",
  "resourceTypes": ["LECTURE", "MIND_MAP", "EXERCISE", "READING", "CODE_LAB"]
}
```

Returns generation task id, agent task id, review status, safety status, generated resource drafts, trace id, and `profileSnapshot`.

The response also includes task recovery metadata:

- `retryCount`
- `nextRetryAt`
- `lastError`
- `recoverable`

When model generation fails recoverably, the stored task is returned by `GET /api/resources/generation-tasks/{taskId}` with `status = FAILED`, `progressPercent = 0`, `retryCount = 1`, `lastError = MODEL_CALL_FAILED`, `recoverable = true`, and a future `nextRetryAt`.

Generated resource `citationSummary` is grounded by the backend review gate:

- `COURSE_RAG: ...` means task-level source citations were persisted under the task `traceId`.
- `NO_SOURCE: ...` means no usable source was persisted and the draft requires manual review before release.

### `GET /api/resources/generation-tasks/{taskId}`

Returns a stored generation task, including the saved `profileSnapshot` and recovery metadata.

### `GET /api/agent/tasks/{taskId}/trace`

Returns ordered agent trace steps. Generated resources remain `PENDING_CRITIC` until critic/teacher review approves release.

### `GET /api/reviews/resources`

Optional query parameter:

- `status`, for example `PENDING_CRITIC`

Returns resource review summaries: `reviewId`, `resourceId`, `generationTaskId`, `status`, `summary`, `resourceTitle`, `resourceType`, `resourceReviewStatus`, `reason`, `citationCheck`, `safetyCheck`, and `revisionSuggestion`.

### `POST /api/reviews/resources/{reviewId}/decision`

```json
{
  "decision": "APPROVED",
  "summary": "Accurate and ready for learner release."
}
```

Allowed decisions are `APPROVED`, `REVISION_REQUESTED`, and `REJECTED`. The backend updates the review, learning resource, and generation task review status transactionally. Reviews or resources marked `NO_SOURCE` cannot be directly approved; the decision endpoint returns `409 CONFLICT`.

## Assessment

### `POST /api/assessment/answers`

```json
{
  "learnerId": "stu_001",
  "questionId": "q_sql_join_cardinality",
  "answer": "A JOIN duplicates parent rows when multiple child rows match."
}
```

Returns answer id, grading result id, score, mastery updates, feedback id, replan record id, wrong-cause analysis, resource push strategy, and trace id.

## Operations

### `GET /api/health`

Returns application, database, Redis, MinIO, and model-provider configuration status.

### `GET /api/analytics/overview`

Returns agent task count, model call count, prompt/completion/total token usage, answer count, wrong-question count, learning-event count, and resource review status counts.

### `GET /api/analytics/classes/{courseId}/summary`

Returns teacher-facing course analytics for the current user. `admin` can query any course; a teacher can query only courses where `Course.teacherId` matches the current user.

The response includes `learnerCount`, `weakKnowledgePoints`, `wrongCauseDistribution`, `resourceCompletion`, and `pendingReviews`. Pending review entries expose only review/resource metadata and do not include draft `markdownContent`.
