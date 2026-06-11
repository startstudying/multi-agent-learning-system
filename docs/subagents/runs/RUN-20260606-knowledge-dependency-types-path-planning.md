# Knowledge DAG 依赖类型与路径规划 Subagent 集成记录

## 背景

本切片用于补齐 `docs/planning/backend-architecture-todolist.md` 中 P1-2 的最小能力：区分 `PREREQUISITE`、`RELATED`、`ADVANCED` 三类依赖边，并让路径规划只使用真正的前置依赖。

## 启用方式

Use Subagents: Yes

Parallelism Level: L1 只读并行分析

Implementation Mode: 主 Codex 串行实现

## 子代理分工

| 子代理 | 任务 | 结果 |
|---|---|---|
| Carver | 核查 `dependencyType` 创建、DTO、Service、异常 envelope | 完成，只读，无文件修改 |
| Ohm | 核查 `LearningWorkflowService` 路径规划如何使用依赖类型 | 完成，只读，无文件修改 |

## 采用的分析结果

- `CreateKnowledgeDependencyRequest` 只有 `@NotBlank` 和 `@Size(max = 40)`，不能阻止 `BLOCKING` 等非法字符串。
- 不建议把 DTO 改成 enum，因为 Jackson 反序列化错误可能绕过当前 `VALIDATION_ERROR` envelope，落入通用 500。
- `KnowledgeCatalogService.createDependency(...)` 是最适合做业务白名单校验的位置。
- `LearningWorkflowService.generateCourseDagPath(...)` 当前只过滤依赖两端是否属于同课程知识点，没有检查 `dependencyType`。
- 在路径规划依赖 stream 中过滤 `PREREQUISITE` 是最小实现，不需要新增 repository 方法。
- 必须保留已有 `PREREQUISITE` 测试，证明修复没有忽略全部依赖。

## 冲突处理

- 子代理建议测试文案可精确断言或包含关键字。最终采用关键字断言，降低错误文案的脆弱性，同时保证包含三种合法类型。
- 子代理指出权限风险，但权限加固属于 P3-4，本切片只记录，不扩大实现。

## 收口

两个子代理均已关闭，未留下后台 agent。
