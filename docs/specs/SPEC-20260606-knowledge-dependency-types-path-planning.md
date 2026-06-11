# Knowledge DAG 依赖类型与路径规划规格

## API 规格

### `POST /api/knowledge-dependencies`

请求：

```json
{
  "knowledgePointId": "kp_repository",
  "prerequisiteId": "kp_entity",
  "dependencyType": "PREREQUISITE"
}
```

成功响应：

```json
{
  "code": "OK",
  "message": "OK",
  "data": {
    "id": "kd_xxx",
    "knowledgePointId": "kp_repository",
    "prerequisiteId": "kp_entity",
    "dependencyType": "PREREQUISITE",
    "createdAt": "2026-06-06T00:00:00Z"
  }
}
```

非法类型响应：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Unsupported dependencyType: BLOCKING",
  "data": null
}
```

HTTP 状态：`400 Bad Request`。

## 依赖类型语义

| dependencyType | 语义 | 是否参与路径锁定 |
|---|---|---|
| `PREREQUISITE` | 必须先掌握的前置知识 | 是 |
| `RELATED` | 相关知识，可用于补充说明或推荐 | 否 |
| `ADVANCED` | 进阶知识，可用于扩展学习 | 否 |

## 路径规划规则

`LearningWorkflowService.generateCourseDagPath(...)` 查询课程下所有知识点相关依赖后，需要先过滤：

```text
dependency.knowledgePointId in coursePointIds
dependency.prerequisiteId in coursePointIds
dependency.dependencyType == PREREQUISITE
```

之后再构建：

```text
prerequisitesByPoint
topologicalOrder
toDagNode
```

因此 `RELATED` 和 `ADVANCED` 仍可存在于知识图谱，但不影响学习路径节点顺序和 `LOCKED` 状态。

## 架构约束

- Controller 只接收请求和返回 envelope。
- `KnowledgeCatalogService` 负责 `dependencyType` 业务校验。
- `LearningWorkflowService` 负责路径规划策略过滤。
- 不新增依赖。
- 不新增数据库迁移。
- 不改变 DTO 字段名。

## 测试规格

- `CourseKnowledgeControllerTest`：
  - 新增非法 `dependencyType` 测试，期望 400 与 `VALIDATION_ERROR`。
  - 保留合法 `PREREQUISITE` 创建测试。
- `LearningWorkflowControllerTest`：
  - 新增 `RELATED` / `ADVANCED` 不锁定节点的路径规划测试。
  - 保留 `PREREQUISITE` 仍锁定未满足前置节点的断言。
