# RUN-20260608 Assessment Record List RBAC / Pagination Backend Analysis

## 1. Summary

当前后端已完成 assessment answer / wrong-question 详情级 RBAC，但没有 list / pagination API。P3-4-F 建议新增两个只读分页列表接口：

- `GET /api/assessment/answers`
- `GET /api/assessment/wrong-questions`

授权仍放在 `AssessmentService`，查询下推到 Repository scoped query，不新增依赖、不新增 migration。

## 2. Current Implementation Evidence

- `AssessmentController` 当前只有提交、answer 详情、wrong-question 详情、grading evaluation 四类入口，没有 list API。
- answer 详情已通过 `AssessmentService.answerDetail(...)` 返回授权后的 DTO，并显式不返回 request snapshot 字段。
- wrong-question 详情同样在 service 内授权后返回。
- 现有详情授权矩阵是：admin 全局、student owner-only、teacher 需课程可读且 learner active enrolled。
- 非 admin missing / foreign 统一 `FORBIDDEN`，admin missing 为 `NOT_FOUND`。
- `CourseAccessService` 已有 active enrollment 能力：`requireCourseRead(...)`、`listActiveLearnerIds(...)`、`listActiveCourseIdsForLearner(...)`。
- Repository 当前不足以支持分页 list：`AnswerRecordRepository` 只有 count 和 requestId replay 查询；`WrongQuestionRepository` 只有 count 和 answerId 最近查询。

## 3. Root Cause

P3-4-E 只收口了对象详情防枚举，list 查询面仍缺失。根本问题不是缺少 Controller 方法，而是 assessment Repository 没有“按当前用户角色 + course enrollment + knowledge point”下推过滤的分页查询；如果直接 `findAll()` 后 service 过滤，会产生分页不准、数据规模风险和潜在越权窗口。

## 4. Recommendations

### 4.1 新增 list API

- `GET /api/assessment/answers`
- `GET /api/assessment/wrong-questions`
- 参数采用本切片 SPEC：`page=0`、`size=20`、`courseId?`、`learnerId?`
- 排序固定为 `createdAt desc, id asc`，避免客户端传任意 sort。

### 4.2 新增 DTO

- `AssessmentPageResponse<T>`
- `AssessmentRecordSummaryResponse`
- `WrongQuestionSummaryResponse`

list DTO 不返回 `answer`、`requestId`、`requestHash`、`responseJson`、完整 `causeAnalysis`；详情接口继续承载敏感/长文本。

### 4.3 Repository scoped query

- `AnswerRecordRepository` 增加 `Page<AnswerRecord>` scoped 查询。
- `WrongQuestionRepository` 增加按 `learnerId`、`knowledgePointId` 的分页查询。
- 使用 Spring Data `Pageable/PageRequest`，不需要新依赖。

### 4.4 Service 授权边界

- Controller 只解析 query param 和 current user，所有角色判断留在 `AssessmentService`。
- student：强制 `learnerId = currentUserId`；传 foreign `learnerId` 返回 `FORBIDDEN`。
- teacher：必须传 `courseId`，只返回自己课程下、active enrolled learner 的记录；传 foreign/missing `courseId` 返回 `FORBIDDEN`。
- admin：全局可查；显式 missing `courseId` 返回 `NOT_FOUND`。
- list 无匹配记录时返回 `OK` + empty page，不把 empty 当作 missing。

## 5. Trade-offs

| Option | Pros | Cons |
|---|---|---|
| 当前表上用 scoped repository 查询 | 不新增 migration；能快速完成 P3-4-F；复用 `questionId/knowledgePointId` 与 enrollment scope | answer list 需要通过 question/knowledge convention 推导课程 |
| 给 `answer_record` 增加 `knowledge_point_id/course_id` | 查询更直接，未来正确/错误记录分离也稳 | 需要 migration 和回填策略；本切片尚不能证明必要 |
| Service `findAll()` 后过滤 | 实现快 | 分页不准、性能差、越权风险更高，不建议 |

## 6. Test Recommendations

- student own-only。
- student foreign learner forbidden。
- teacher own-course active learner 可见。
- teacher foreign course 不可见。
- teacher unenrolled learner 返回空 page。
- admin 全局可见。
- page/size validation。
- list item 不含 `answer/requestId/requestHash/responseJson/causeAnalysis`。
