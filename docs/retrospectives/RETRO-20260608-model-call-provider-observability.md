# Retrospective - P3-3-B 模型调用 provider 持久化观测

## 1. Feature Summary

完成 `model_call_log.provider` 最小持久化观测切片：V18 schema、entity 字段、成功/失败 recorder 写入、gateway provider 低基数归一化、敏感 provider 字符串降级为 `other`。

## 2. What Went Well

- 切片边界清晰，没有把真实 provider SDK、API 配置、前端展示和 analytics API 混入本次变更。
- 保留 `recordFailure` 旧签名并新增 provider overload，降低相邻调用点兼容风险。
- provider 在 gateway response、recorder 和 metrics 侧使用同一低基数口径，避免 DB 与指标标签语义漂移。
- 聚焦/相邻回归和完整后端 Maven 测试均通过。

## 3. What Didn't Go Well

- 本机真实 MySQL smoke 无法完成，原因是 3306 MySQL `root` 凭据不可用；V18 实库迁移仍需环境恢复后补验。
- 现有 PowerShell 默认输出编码容易把旧中文文档显示为乱码，后续读取中文文档应优先使用 UTF-8 显式读取。

## 4. Skill Extraction

已更新：

- `docs/skills/project-specific/model-gateway-boundary.md`

新增规则重点：

- provider 必须使用低基数白名单。
- unknown / URL / tenant / `apiKey` / `sk-` 统一写 `other`。
- `model_call_log.errorMessage` 不得承载 provider 原文或 raw exception。

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| MySQL smoke | 依赖本机 3306 root 凭据或 Docker daemon | 后续在任务开始前先确认可用 MySQL smoke 环境，或提供固定 CI/Docker 端口 |
| Provider 接入 | provider schema 已完成，真实 SDK 未接 | P3-3-C 单独做 dependency review、official docs check 和安全配置边界 |
| 文档编码 | 部分历史文档在默认 console 下显示乱码 | 收尾阶段统一用 UTF-8 显式读取，避免误判内容 |

## 6. Action Items

| Action | Owner | Status |
|---|---|---|
| 环境可用后补跑 `MysqlMigrationSmokeTest` V18 | 后续开发者 | Open |
| 设计真实 Chat provider adapter 接入 | 后续开发者 | Open |
| 根据实际 provider 聚合查询评估 provider/status/time 索引 | 后续开发者 | Open |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] DATABASE_MEMORY.md
- [x] CHANGELOG.md
- [x] SKILL_REGISTRY.md / project-specific skill
