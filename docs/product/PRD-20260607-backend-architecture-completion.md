# PRD-20260607 后端架构 TODO 完成计划

## Task 3 产品补充：P3-5-A 运维告警 API

### 背景

P3-5 已完成结构化请求日志、Micrometer 运行指标和深度健康检查，但还缺少一个后端可查询的运维告警视图，用于发现慢 RAG 查询、慢模型调用、RAG 无来源回答和审核积压。

### 目标

新增一个 admin-only 的 query-time API：

```text
GET /api/analytics/ops/alerts
```

该 API 为管理员提供四类风险信号的聚合告警，不做外部推送、不新增告警表、不新增依赖、不改前端。

### 非目标

- 不接入 Alertmanager、Grafana 推送或通知系统。
- 不新增数据库表或索引。
- 不暴露 prompt、用户原始问题、RAG 原文、模型原始输出、资源正文或 raw provider error。
- 不声明完成真实 JWT/RBAC；本切片仍使用临时 `X-User-Id: admin`。

## 1. 背景

`docs/planning/backend-architecture-todolist.md` 已完成 P0/P1/P2 大部分闭环，剩余工作集中在 P3 生产化与工程质量：

- RAG 索引生产化
- 真实模型接入边界
- 权限与安全加固
- 可观测性告警

用户要求完成该计划，并使用专家 subagent 并行开发。

## 2. 目标

把后端从“可演示闭环”推进到“可答辩、可审计、可恢复、可控风险”的生产化基线。

## 3. 范围

### 本总控范围

- 梳理剩余 P3 未完成项。
- 通过专家 subagent 并行分析后形成切片计划。
- 逐切片补齐 Spec-First 文档、测试、实现、证据、验收与记忆更新。

### 已完成实现范围

- P3-4-A：Course / Knowledge Catalog 写权限收口。
- P3-4-B：对象详情防枚举与 scoped authorization。已收口学习路径、资源生成任务、Agent Trace、RAG 文档/索引任务详情接口中普通用户可通过 `404` vs `403` 探测对象存在性的风险。

### 下一实现范围

P3-5-A：慢查询、慢模型、无引用回答、审核积压告警。

## 4. 非目标

- 本轮不一次性引入真实外部模型 provider。
- 本轮不新增 VectorDB、OCR 或外部告警依赖。
- 本轮不重写认证体系，不引入完整 JWT/RBAC 表结构。

## 5. 用户价值

- 防止学生或未授权用户篡改课程与知识图谱。
- 防止普通用户通过对象详情接口枚举 learning path、resource task、agent trace、document/index task。
- 降低 Review Gate、Analytics、Learning Path 被伪造课程污染的风险。
- 为后续完整 RBAC、班级授权和答题记录权限矩阵提供地基。

## 6. 成功标准

- 剩余 P3 项按切片逐步完成并更新 TODO checkbox。
- 每个切片都有 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT。
- 所有实现有聚焦测试，必要时有全量测试。
- 不新增未审查依赖。
- 不违反 `ARCHITECTURE_BASELINE.md`。
