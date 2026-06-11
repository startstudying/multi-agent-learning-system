# RAG 文档上传幂等 TASK

## Done Criteria

- [x] 上传接口接受可选 `requestId`。
- [x] 首次上传保存 `requestId/requestHash/responseJson`。
- [x] 相同 `requestId + payload` 重放首次响应。
- [x] 相同 `requestId` 不同 payload 返回 409。
- [x] 重放不新增 `kb_document` 和 `kb_index_task`。
- [x] V8 migration 增加字段和唯一索引。
- [x] 相关测试通过。

## 测试命令

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,DocumentControllerTest" test
```
