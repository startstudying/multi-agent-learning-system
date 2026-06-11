# 高手版选题支撑：LLM 个性化学习多 Agent 架构路线图

本文把“LLM + 个性化学习 + Multi-Agent Orchestration + RAG + Assessment Feedback Loop”整理成工程交付视角的选题支撑材料，用于开题报告、README 摘要和后端交接。本文只说明当前仓库已有能力与可落地路线，不把论文或开源项目细节当作已实现能力。

## 选题定位

题目建议：**面向课程知识的个性化学习多 Agent 系统设计与实现**。

核心闭环：

```text
课程资料 -> RAG 索引与引用 -> 学生提问/作答 -> 学习画像与掌握度更新
-> 多 Agent 规划路径/生成资源/出题反馈 -> 教师或 Critic 审核 -> 下一轮学习重规划
```

工程目标不是单个聊天机器人，而是一个可追踪、可审核、可评估的学习工作流平台。

## 研究模块映射

| 研究模块 | 工程问题 | 仓库落点 | 当前状态 |
| --- | --- | --- | --- |
| LLM 个性化学习 | 从对话、作答、学习事件中抽取画像，生成个性化路径 | `learning`, `knowledge`, `assessment` | 已有画像、路径、掌握度持久化；真实 LLM 抽取待接入 |
| 教育 RAG | 用课程资料支撑问答、资源生成和引用追溯 | `rag`, `safety` | 已有 KB、文档、chunk、查询、SSE、引用合同；解析、嵌入、向量检索待加强 |
| 多 Agent 编排 | 将画像、诊断、规划、RAG、资源、评测、审核拆成有边界的工作流 | `agent`, `learning`, `rag`, `assessment` | 已有任务、trace、模型调用和 token 日志表；真实编排器与工具调用待实现 |
| 自动化评测反馈 | 作答评分、错因诊断、错题本、掌握度更新、路径重规划 | `assessment`, `learning` | 已有答题、评分、错题、掌握度更新记录；Rubric/LLM 评分待替换 deterministic 逻辑 |
| 可信与治理 | 权限过滤、引用、人工复核、成本与 trace 可观测 | `common`, `agent`, `rag`, `analytics`, `safety` | 已有 trace id、query log、review、model/token log 基础；指标面板待完善 |

## GitHub 工程参考

这些项目用于工程边界、测试思路和演示对标，不代表本仓库复用其实现：

| 参考方向 | GitHub 参考 | 可借鉴点 |
| --- | --- | --- |
| LMS 数据模型 | [moodle/moodle](https://github.com/moodle/moodle), [openedx/edx-platform](https://github.com/openedx/edx-platform), [frappe/lms](https://github.com/frappe/lms) | 课程、学习者、作业/测评、权限等教育业务边界 |
| Agent 编排 | [langchain-ai/langgraph](https://github.com/langchain-ai/langgraph), [microsoft/autogen](https://github.com/microsoft/autogen), [crewAIInc/crewAI](https://github.com/crewAIInc/crewAI) | 有状态工作流、角色拆分、工具调用、失败恢复 |
| RAG 工程 | [deepset-ai/haystack](https://github.com/deepset-ai/haystack), [run-llama/llama_index](https://github.com/run-llama/llama_index), [infiniflow/ragflow](https://github.com/infiniflow/ragflow) | 文档解析、检索链路、引用、评估集 |
| LLM 应用平台 | [langgenius/dify](https://github.com/langgenius/dify) | Prompt/模型配置、应用编排、可观测性 |
| Java LLM 集成 | [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) | Chat、Embedding、结构化输出、工具调用的 Java 接入方式 |

## 目标架构

```text
Vue 学生/教师/管理员工作台
-> REST / SSE API
-> OrchestratorService
-> Profile Agent / Diagnosis Agent / Course RAG Agent / Path Planner Agent
-> Resource Agent / Assessment Agent / Critic Agent / Tutor Agent
-> application services
-> MySQL 8 + Redis + MinIO + optional VectorDB
```

边界原则：

- Orchestrator 只负责编排、状态推进和 trace，不直接写 repository。
- Agent 通过 application service 或受控 tool 访问领域能力。
- RAG 必须先做 KB 权限过滤，再检索 chunk。
- 资源生成必须经过 Critic/教师审核后才能发布给学生。
- 真实模型调用必须记录 `model_call_log`、`token_usage_log`、延迟、状态和错误。

## 后端模块映射

| 当前模块 | 现有职责 | 对应目标架构 |
| --- | --- | --- |
| `rag` | KB、权限、文档、index task、chunk、RAG query、SSE、citation | Course RAG Agent 的证据层和工具能力 |
| `learning` | 学习画像、学习事件、学习路径、路径节点、掌握度 | Profile Agent、Path Planner Agent、重规划状态 |
| `knowledge` | 课程、章节、知识点、先修依赖 | 知识图谱、路径规划和测评定位 |
| `agent` | 资源生成任务、学习资源、review、agent task/trace、model/token log | 多 Agent 运行时、Critic 审核、治理日志 |
| `assessment` | answer record、grading result、wrong question、掌握度更新 | Assessment Agent 与反馈闭环 |
| `safety` | 内容安全结果与审核状态 | 输入安全、生成草稿安全检查 |
| `analytics` | 健康、调用、token、活动和 review 汇总 | 管理端运营与答辩指标 |
| `common` / `config` / `health` / `user` | 通用响应、错误、trace filter、认证上下文、配置和健康检查 | 平台基础设施 |

## 当前能力到目标架构的对应关系

| 目标能力 | 当前已有 | 下一步工程补齐 |
| --- | --- | --- |
| 课程知识 RAG | 文档元数据、chunk、权限过滤、query log、SSE 响应、citation DTO | PDF/DOCX/Markdown/TXT 解析、chunk 策略、embedding、向量库 adapter、hybrid retrieval、reranker |
| 个性化画像 | 画像 DTO、学习事件、画像持久化 | LLM 结构化抽取、画像合并策略、置信度与人工修正 |
| 学习路径 | path/path node、知识点依赖基础 | DAG 驱动规划、基于掌握度的动态重规划 |
| 多 Agent 资源生成 | generation task、resource、review、agent trace、model/token log | 真实 Orchestrator、Agent DTO、工具边界、重试/取消/失败恢复 |
| 测评反馈 | 提交答案、评分结果、错题、掌握度更新 | Rubric-backed LLM grading、错因分类、反馈质量评估 |
| 可信治理 | trace id、query log、review gate、token log 基础 | trace coverage、成本、引用率、权限泄漏测试、教师审核面板增强 |

## 分阶段实施路线

1. **基线闭环固化**：保持 MySQL 8、Spring Boot、Vue 工作台和现有 API 合同稳定；补齐 README/接口文档中的演示步骤。
2. **RAG 生产化**：实现文档解析、清洗、chunk、embedding、可选 VectorDB adapter、hybrid retrieval、RRF、reranker fallback 和 RAG 评估集。
3. **模型接入**：通过 Spring AI / Spring AI Alibaba 接入 chat、embedding、结构化输出和工具调用；所有调用写入模型日志和 token 日志。
4. **Agent 编排**：新增 Orchestrator，定义 Profile、Diagnosis、RAG、Planner、Resource、Assessment、Critic、Tutor Agent 的输入/输出 DTO、状态机和失败策略。
5. **评测反馈闭环**：用 rubric 和课程证据驱动作答评分、错因诊断、掌握度更新、错题本、路径重规划。
6. **教师审核与可信治理**：强化资源审核 API、引用展示、安全检查、trace 看板、成本统计和权限泄漏测试。
7. **答辩演示加固**：准备固定课程资料、学生画像样例、RAG 问答、路径生成、资源审核、测评反馈、重规划和管理端指标的端到端脚本。

## Demo / 答辩故事线

1. **开场问题**：传统在线学习系统难以同时做到课程依据、个性化、自动反馈和可审计。
2. **资料进入系统**：教师上传课程资料，系统创建 KB、文档、索引任务和 chunk。
3. **学生提问**：学生围绕课程内容提问，RAG 返回答案、引用、trace id。
4. **画像与路径**：系统根据对话和目标生成学习画像与路径节点。
5. **多 Agent 生成资源**：Resource Agent 生成讲解/练习草案，Critic/教师审核后发布。
6. **测评反馈**：学生提交答案，Assessment Agent 给出评分、错因、掌握度变化和错题记录。
7. **闭环重规划**：Planner 根据新掌握度调整学习路径，后台保留 trace、成本、引用和审核证据。
8. **工程亮点**：强调模块边界清晰、RAG 权限先行、Agent 可追踪、生成内容可审核、模型调用可计量。

## 交接注意事项

- 不要把 Agent 直接写成 controller 内部逻辑；编排应落在 application/orchestration 层。
- 不要让工具或 Agent 直接访问 repository；通过领域 service 保持权限和事务边界。
- 不要将 PostgreSQL 重新设为主数据库；向量能力应作为可选 adapter。
- 不要在没有 citation、trace 和 review 证据的情况下发布生成资源。
- 新增真实模型能力时，优先保持现有 REST/SSE DTO 不破坏前端。

