# PRD-20260608 P3-4-C 权限矩阵安全前置

## 1. 背景

P3-4 已完成课程/知识图谱写路径收口、Review Gate 课程范围、对象详情防枚举等基础工作。但计划仍要求补齐生产级权限检查和完整渗透测试矩阵。

当前最直接的缺口是课程读取和知识图谱读取未按用户范围过滤，评分评估接口也未限制 teacher/admin。

## 2. 目标

本切片先补齐低风险、无 schema 变更、可测试的安全前置：

- 教师只能读取自己的课程和知识图谱。
- 学生不能读取未授权课程和知识图谱。
- 管理员保留全局读取与真实 `NOT_FOUND` 语义。
- 评分评估接口限制为 teacher/admin。

## 3. 非目标

- 不实现完整 JWT。
- 不新增 class/enrollment 表。
- 不新增答题记录详情 API。
- 不接真实模型、VectorDB、OCR。
- 不修改数据库 schema。

## 4. 用户价值

- 避免 course、chapter、knowledge graph 作为课程结构数据被任意用户读取。
- 为后续 class/course 矩阵、RAG document course scope、answer record scope 提供可复用权限语义。
