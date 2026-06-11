# RAG 索引任务 Active 去重需求

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `IndexService.createPendingTask(document)` 必须先查询该文档最新索引任务。 |
| FR-02 | 最新任务状态为 `PENDING` 时，返回已有任务。 |
| FR-03 | 最新任务状态为 `RUNNING` 时，返回已有任务。 |
| FR-04 | 最新任务状态为 `FAILED` 或 `SUCCEEDED` 时，创建新的 `PENDING` 任务。 |
| FR-05 | `POST /api/documents/{documentId}/reindex` 必须体现上述去重语义。 |
| FR-06 | 重复 reindex 不应改变文档读写权限规则。 |

## 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不新增数据库字段、索引或依赖。 |
| NFR-02 | 去重逻辑位于 Service 层。 |
| NFR-03 | 保持现有 API 响应结构不变。 |
| NFR-04 | 聚焦测试和全量后端测试通过。 |

## 验收要求

- `DocumentControllerTest` 覆盖 active task 去重。
- `IndexService` 行为不依赖 Controller 层特殊判断。
