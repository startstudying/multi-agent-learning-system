# RAG 文档上传幂等 Retrospective

## 做得有效

- 复用了 Answer Submission 和 RAG Query 的 `requestId + requestHash + responseJson + unique index` 模式。
- 保持 `requestId` 可选，降低对旧上传调用的破坏。
- RED 先失败在缺少实体字段，随后 GREEN 验证了服务层行为。

## 后续改进

- 大文件 hash 应改为流式读取，避免一次性读入内存。
- P3 需要补 MySQL 8 Flyway smoke。
- 与索引任务后台恢复/锁切片集成后，需要跑更宽的 RAG 回归。
