# Evaluation Set 管理需求

## 功能需求

1. 系统必须支持创建或更新评估集。
2. 系统必须支持按 `type` 查询当前用户可见的评估集。
3. 系统必须支持按 `id` 查询评估集详情和样本。
4. 系统必须支持三类评估集：
   - `RAG_QUESTION`
   - `GRADING_SAMPLE`
   - `RESOURCE_GENERATION_SAMPLE`
5. `RAG_QUESTION` 样本必须包含 `question` 和 `expectedSourceIds`。
6. `GRADING_SAMPLE` 样本必须包含 `answerText`、`rubric` 和 `humanScore`。
7. `RESOURCE_GENERATION_SAMPLE` 样本必须包含 `learnerProfileSnapshot`、`learningGoal` 和 `expectedResourceType`。
8. 同一创建者下相同 `code + version` 的评估集必须 upsert，不新增重复行。

## 非功能需求

- Controller 只处理 HTTP 请求和当前用户提取，业务规则放在 Service。
- 不新增外部依赖。
- 数据库迁移必须向前兼容。
- 响应不返回 raw prompt 或 raw model output。
- 管理 API 必须在 Service 层做权限判断。

## 输入输出

### 输入

- `code`
- `version`
- `name`
- `description`
- `type`
- `status`
- `courseId`
- `kbId`
- `promptCode`
- `promptVersion`
- `samples`

### 输出

- 评估集基础信息。
- `sampleCount`。
- 详情接口返回结构化样本字段。

## 边界情况

- 缺少 `code`、`version`、`name` 或 `type`：返回 `VALIDATION_ERROR`。
- `type` 不在允许集合：返回 `VALIDATION_ERROR`。
- 普通学生创建或查询：返回 `FORBIDDEN`。
- 查询不存在的 set：返回 `NOT_FOUND`。
- 样本 payload 与 set type 不匹配：返回 `VALIDATION_ERROR`。
