# Learner Profile 维度与快照规格

## ProfileDimension

`ProfileDimension` 扩展为：

```json
{
  "name": "baseline_level",
  "value": "Needs SQL JOIN and RAG grounding support",
  "confidence": 0.82,
  "evidence": "I want Spring Boot but SQL joins confuse me.",
  "sourceType": "CONVERSATION",
  "lastEvidenceId": "trc_xxx:baseline_level"
}
```

`lastEvidenceId` 使用当前 `traceId + ':' + dimensionName` 生成，表示该维度最后一次更新的证据锚点。

## 结构化画像字段

`ProfileStructuredFields` 包含：

```json
{
  "baseline_level": "...",
  "learning_goal": "...",
  "weak_point": ["..."],
  "preference": ["..."],
  "pace_and_feedback": "...",
  "recent_error_pattern": "...",
  "teacher_note": "...",
  "sources": ["CONVERSATION"]
}
```

## Profile Snapshot

`profileSnapshot` 是一个 JSON 字符串，最小包含：

```json
{
  "learnerId": "alice",
  "target": "Build production-grade Spring Boot APIs",
  "baseline_level": "...",
  "learning_goal": "...",
  "weak_point": ["SQL JOIN reasoning"],
  "preference": ["code labs"],
  "pace_and_feedback": "...",
  "recent_error_pattern": "...",
  "teacher_note": "...",
  "sources": ["CONVERSATION"]
}
```

## 持久化

新增 V10 migration：

- `learning_path.profile_snapshot text`
- `resource_generation_task.profile_snapshot text`

`LearningPathResponse` 和 `ResourceGenerationResponse` 增加 `profileSnapshot` 字段。

## 架构约束

- Controller 不增加业务逻辑。
- 画像快照由 Service 层从 `LearnerProfileRepository` 读取。
- 没有画像时生成稳定空快照，而不是返回错误。
- 不新增外部依赖。
