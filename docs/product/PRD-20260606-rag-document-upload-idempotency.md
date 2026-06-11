# RAG 文档上传幂等 PRD

## 背景

P0-3 仍有“重复上传不产生重复业务结果”未完成。当前文档上传每次都会保存一个新的 `kb_document`，并创建新的索引任务；用户重复点击上传时会产生重复文档和重复索引业务结果。

## 目标

为 RAG 文档上传增加 `requestId` 幂等能力：同一用户使用同一 `requestId` 上传同一业务 payload 时，系统返回首次上传结果；同一 `requestId` 对应不同 payload 时返回 409。

## 非目标

- 不实现真实文件解析、embedding 或 VectorDB。
- 不改变未传 `requestId` 的旧上传行为。
- 不处理索引任务调度器和 DB 级索引锁；这些由独立切片处理。

## 成功标准

- 重复上传同一文件和 metadata 不新增 `kb_document`。
- 重复上传不新增 `kb_index_task`。
- 同一 `requestId` 不同文件或 metadata 返回 409。
- 无权限上传仍不写文档和索引任务。
