# Subagent Report - P3-4 权限与安全加固安全审查

## 角色

Security & Quality Expert

## 结论

实现前整体风险为 HIGH：多个 P3-4 权限边界仍未执行。

## 发现

1. Profile owner check 缺失  
   `backend/src/main/java/com/learningos/learning/api/ProfileController.java` 直接把请求交给服务层；`LearningWorkflowService.extractProfile(...)` 会按请求体 `learnerId` 写入画像和学习事件。

2. Learning Path owner check 缺失  
   `LearningPathController` 创建和读取路径时未校验当前用户；`LearningWorkflowService.getPath(...)` 会按 `pathId` 返回路径。

3. Analytics overview 未限制 admin  
   `AnalyticsController.overview()` 未读取 `CurrentUserService`，旧测试还允许 `teacher` 访问全局 overview。

4. RAG mixed unauthorized `kbIds` 被过滤而非严格拒绝  
   `PermissionService.filterAllowedKbIds(...)` 会静默丢弃不可访问 KB；`RagQueryService` 只要剩余合法 KB 就继续检索并写日志。

5. Health 输出避免了直接 accessKey/secretKey，但仍暴露部署指纹  
   旧 `HealthService` 返回数据库 URL、Redis host/port、MinIO endpoint/bucket、provider/model 名称等公开拓扑信息。

## 建议

- 保持本切片为最小权限收口，不引入完整 JWT/RBAC 或新依赖。
- owner/admin/strict KB 拒绝必须发生在业务写入、检索、日志写入之前。
- Health 公开响应只保留粗粒度状态与 configured 布尔值。

