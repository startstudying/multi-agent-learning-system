# Knowledge DAG 掌握度阈值补救优先规格

## 规则常量

```java
COMPLETION_THRESHOLD = 0.80
REMEDIATION_THRESHOLD = 0.60
```

`COMPLETION_THRESHOLD` 继续用于：

- 节点 `DONE` 判断。
- 前置依赖是否已满足。

`REMEDIATION_THRESHOLD` 用于：

- 可学习节点排序时识别补救优先候选。
- `reasonSummary` 说明补救原因。

## 规划流程

课程 DAG 路径规划顺序：

```text
读取课程知识点
读取 PREREQUISITE 依赖
读取 learner 最新 mastery
构建 prerequisitesByPoint
构建 dependentsByPoint
按拓扑约束分批选择可学习节点
在每批可学习节点中优先补救 mastery < 0.6 且有下游依赖的前置知识
生成 LearningPathNodeResponse
```

## 排序规则

在同一轮可学习节点中：

1. 补救优先候选排在普通节点前。
2. 补救优先候选之间按 mastery 升序。
3. 普通节点保持原知识点创建顺序。

当存在环或无法继续推进时，沿用现有降级策略：剩余节点按原顺序追加，避免死循环。

## 节点状态

本切片不新增状态：

- `DONE`：当前节点 mastery `>= 0.80`。
- `ACTIVE`：当前节点未完成，且前置依赖均达到 `0.80`。
- `LOCKED`：存在未达到 `0.80` 的前置依赖。

## reasonSummary

补救优先节点输出类似：

```text
Prioritized for remediation because current mastery 0.5 is below the remediation threshold 0.6 and this prerequisite unlocks downstream knowledge in the Knowledge DAG.
```

## 架构约束

- 只修改 `LearningWorkflowService` 和相关测试。
- 不改 Controller。
- 不改 DTO。
- 不改数据库。
- 不新增依赖。
