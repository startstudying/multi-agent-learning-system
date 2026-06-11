# RUN-20260608 Grading Evaluation Course Scope - Integration Reviewer

## 1. 最终权限矩阵

| Actor | `courseId` | Course state | Sample `knowledgePointId` state | Result |
|---|---|---|---|---|
| admin | missing / blank | not checked | not checked | `VALIDATION_ERROR` |
| admin | present | existing | blank or belongs to course | 200 |
| admin | present | missing | not checked | `NOT_FOUND` |
| admin | present | existing | any non-blank KP outside course or missing | `VALIDATION_ERROR` |
| own teacher | missing / blank | not checked | not checked | `VALIDATION_ERROR` |
| own teacher | present | own course | blank or belongs to course | 200 |
| teacher | present | missing / foreign course | not checked | `FORBIDDEN` |
| teacher | present | own course | any non-blank KP outside course or missing | `VALIDATION_ERROR` |
| student / other | any | any | any | `FORBIDDEN` |

## 2. 决议

采用强制 `courseId` 方案，不保留 HTTP 旧请求兼容。

保留兼容范围仅限纯指标算法和响应 shape：`samples` / legacy score arrays 的计算逻辑继续存在，但 HTTP `POST /api/assessment/grading-evaluations` 必须先通过 course scope。

## 3. 必须测试

- admin existing course + valid samples -> 200。
- admin missing `courseId` -> `VALIDATION_ERROR`。
- admin missing course -> `NOT_FOUND`。
- teacher own course + valid samples -> 200。
- teacher missing `courseId` -> `VALIDATION_ERROR`。
- teacher foreign course -> `FORBIDDEN`。
- teacher missing course -> `FORBIDDEN`。
- student with valid course -> `FORBIDDEN`。
- sample foreign/missing `knowledgePointId` -> `VALIDATION_ERROR`，不泄露具体 offending id。
- legacy score arrays with valid `courseId` keep metric behavior。

## 4. 架构漂移结论

`assessment -> knowledge` 依赖增加是有意安全收口，且与既有 assessment record RBAC 中的 `CourseAccessService` / `KnowledgePointRepository` 用法一致。权限必须留在 Service 层；Controller 不做归属判断。
