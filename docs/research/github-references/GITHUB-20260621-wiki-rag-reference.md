# GITHUB-20260621 wiki-rag 项目参考调研

## 1. 基本信息

| 项 | 内容 |
|---|---|
| 仓库 | `moodlehq/wiki-rag` |
| 地址 | https://github.com/moodlehq/wiki-rag |
| 定位 | MediaWiki Retrieval-Augmented Generation |
| 维护方 | Moodle Research |
| 许可证 | BSD-3-Clause |
| 当前成熟度 | `pyproject.toml` 标注为 Alpha / experimental |
| 最新观察版本 | GitHub Releases 页面显示 `v0.17.0` 为 latest，发布日期为 2026-06-14 |

## 2. 一句话结论

`wiki-rag` 不是简单的“文档切块 + 向量检索”demo，而是一个围绕 MediaWiki 结构化知识构建的 RAG 系统。它最值得本项目借鉴的是：

1. MediaWiki API 结构化抽取。
2. section 级父子/前后/链接关系建模。
3. dense + BM25 hybrid search。
4. 检索后上下文优化：POP / POC。
5. OpenAI-compatible HTTP wrapper 和 MCP server 作为集成边界。
6. config / `.env` 分离，以及可选 LangSmith / Langfuse 观测。

不建议直接照搬它的 Python / LangChain / LangGraph / Milvus 技术栈。本项目已有 Java 21 + Spring Boot + Spring AI + MySQL + Qdrant/VectorIndexAdapter 边界，更适合吸收设计模式，而不是迁移运行时。

## 3. 架构拆解

### 3.1 数据加载：MediaWiki API，不是网页爬虫

README 明确说明项目通过 MediaWiki API 抽取内容和元数据。`wiki_rag/load/util.py` 中也能看到它调用 `api.php` 的 `query` / `parse` 能力，读取页面列表、wikitext、sections、categories、templates、links、langlinks、externallinks 等。

关键设计：

- 只处理配置 namespace。
- 支持分类和 wikitext regex 排除。
- 每个页面生成稳定 `doc_id`。
- 基于 revision id 生成 `doc_hash`。
- 将页面拆成 sections。
- 保留 categories、templates、internal/external/language links。
- 计算 section 的 parent / children / previous / next / relations。

对本项目启发：

- 当前课程文档/RAG 可以继续强化“结构化解析结果”，不要只保留平铺 chunk。
- 对教材章节、知识点、标题层级、相邻段落、引用关系，应形成可检索的结构关系。
- 这和我们后续 `MemoryContextService` / `ContextOrchestrator` 很契合。

### 3.2 索引：JSON dump -> schema validate -> vector store

README 说明 `wr-load` 先生成 JSON dump，`wr-index` 再创建或增量更新 vector index。`wiki_rag/index/util.py` 会先加载 JSON 并用 `schema.json` 校验，再把 pages/sections 写入向量集合。

关键设计：

- dump 文件可用于后续分析和 MCP resource 暴露。
- 支持 incremental loading / indexing。
- full reindex 使用临时 collection，再替换正式 collection。
- 对超长 section 有 chunking/trim 处理，最新版 release 已经加入 chunk-aware search。

对本项目启发：

- RAG ingestion 应保留“解析产物快照”，便于审计、回放和重新索引。
- 增量索引要基于文档 hash / revision / chunk hash，而不是每次全量重建。
- full reindex 使用临时索引再原子替换，是很好的线上稳定性策略。

### 3.3 检索：dense + BM25 hybrid search

`wiki_rag/vector/milvus.py` 使用 Milvus native SDK，建立 dense vector 与 sparse BM25 两类索引，并通过 `hybrid_search` + `WeightedRanker` 做融合。

关键设计：

- dense vector：HNSW + inner product。
- sparse vector：BM25。
- hybrid 权重默认 dense 0.7 / sparse 0.3。
- 输出字段包含 id、title、text、source、doc_id、doc_title、doc_hash、parent、children、previous、next、relations、page_id。

对本项目启发：

- 我们当前已有 RRF/hybrid retrieval 方向，`wiki-rag` 支持继续验证“关键词 + 向量 + 结构关系”的价值。
- 不建议切换到 Milvus；本项目已经有 Qdrant/VectorIndexAdapter 边界。
- 需要重点补的是“结构字段参与 rerank/context build”，而不是换 vector DB。

### 3.4 检索后优化：POP / POC

README 提到 POP（popularity）和 POC（parent, own and child）。`wiki_rag/search/util.py` 中的 `optimise` 会基于检索结果中的 `id`、`parent`、`children`、`previous`、`next` 统计权重，再用 POC 策略构造上下文。

关键设计：

- 检索不是终点，检索结果会再组织。
- 命中的 section 会带上父节点和子节点。
- 结构相邻内容会补进上下文，减少“只命中半句话”的问题。
- sources 会按 section 生成 MediaWiki 链接。

对本项目启发：

- 这是最值得吸收的部分。
- 当前教育 RAG 可以做 “Knowledge POC Context”：命中 chunk 后补充所属知识点、章节标题、前后 chunk、父级章节摘要。
- 这比单纯提高 topK 更稳，也更省 token。

### 3.5 Query rewrite / HyDE / chitchat routing

`wiki_rag/search/util.py` 用 LangGraph 编排 `query_rewrite -> hyde_rewrite -> retrieve -> optimise -> generate`。它会：

- 使用上下文重写问题。
- 判断是否为 chitchat。
- 可选启用 HyDE，生成假想文档 passage 后再做 dense retrieval。
- 仍保留原始问题用于 BM25。

对本项目启发：

- `IntentRouter` 可以先做轻量任务分类：课程问答、闲聊、无来源、学习诊断、资源生成。
- HyDE 不应直接默认上线，应先进入 eval-only 或 gated 实验路径。
- Query rewrite 必须 trace，并且要有 prompt-injection 测试。

### 3.6 OpenAI-compatible API wrapper

README 说明 `wr-server` 暴露 `v1/models` 和 `v1/chat/completions`。`wiki_rag/server/server.py` 实现了 FastAPI wrapper，支持 streaming、history filter、model name 校验和 bearer token。

关键设计：

- 把 RAG 系统包装成一个“模型”。
- 客户端可以按 OpenAI Chat Completions 接口调用。
- 支持 streaming。
- 历史消息会按 turns 和 token budget 过滤，并移除 system/developer 消息。

对本项目启发：

- 本项目不需要把整个系统伪装成 OpenAI API，但可以借鉴“统一 QA Gateway”思想。
- `/api/ai/qa` 后续应成为 `QaRuntime` 的稳定入口。
- 历史上下文必须裁剪，不能前端发多少就喂多少。

### 3.7 MCP server

README 说明 `wr-mcp` 暴露 prompts、resources、tools。`wiki_rag/mcp_server/server.py` 中可以看到 `retrieve`、`optimise`、`generate` 三个 tool，以及获取解析页面的 resource 和 prompt。

关键设计：

- MCP 不是只给最终答案，还能暴露中间能力。
- tool 可以只做 retrieve / optimise / generate 的某一段。
- resource 可访问解析后的页面 dump。
- prompt 也能被外部读取。

对本项目启发：

- MCP 可以作为 P2/P3 能力，不应现在直接引入。
- 更现实的近期做法：先把本项目内部工具边界做清楚，例如 `retrieveCourseChunks`、`buildMemoryContext`、`verifyAnswer`。
- 未来若接 MCP，必须有 auth、allowlist、trace、rate limit、privacy guard。

### 3.8 配置与安全边界

`config.yml.template` 明确把非密配置放 `config.yml`，密钥放 `.env` 或环境变量。`dotenv.template` 也强调 secrets-only。server 和 MCP 都有 `auth_required`，使用本地 token 列表或远端 auth service。

正向点：

- 非密配置与密钥分离。
- 支持 bearer token。
- 支持远端 auth delegation。
- 支持 history token budget。

风险点：

- GitHub Security 页面显示未设置 `SECURITY.md`。
- 项目定位 alpha/experimental，不适合直接作为生产安全基线。
- MCP resource 能暴露解析页面，需要谨慎处理私有课程资料和学生数据。
- `auth_required=false` 仅应本地使用，生产环境必须 fail-closed。

## 4. 与本项目的匹配度

| 能力 | wiki-rag 做法 | 本项目现状 | 建议 |
|---|---|---|---|
| 文档来源 | MediaWiki API | 课程 KB / 文档上传 / 解析器 | 借鉴结构化抽取，不照搬 MediaWiki 依赖 |
| 结构关系 | parent / children / previous / next / relations | 已有章节/知识点/Chunk metadata 基础 | 强化 chunk 与知识点/章节/前后文关系 |
| Hybrid search | dense + BM25 + WeightedRanker | 已有 hybrid/RRF/Qdrant 边界 | 继续用现有边界，补结构 rerank |
| 检索后优化 | POP / POC | Context builder 仍需加强 | 做 `POCContextBuilder` 或纳入 `MemoryContextService` |
| Query rewrite | contextualisation model | AI QA 尚未统一 runtime | 作为 `IntentRouter` / rewrite node 的候选 |
| HyDE | 可选 | 曾在路线图中提到 governed HyDE | eval-only，不能默认上线 |
| API wrapper | OpenAI-compatible server | 本项目有业务 REST API | 借鉴统一入口，不伪装模型 |
| MCP | 内置 MCP server | 本项目尚未接 MCP | P2/P3 再评估，先内部 tool boundary |
| Observability | LangSmith/Langfuse 可选 | 本项目已有 Agent Trace / model logs | 保持项目内 Trace 为主 |
| 安全 | bearer token / auth URL | 本项目 RBAC 更强 | 不照搬 auth，保持 roles-first RBAC |

## 5. 可直接转化为本项目任务的点

### T1：RAG POC Context Builder

目标：

- 命中 chunk 后补充父级章节、当前 chunk、相邻 chunk、子节点摘要。
- 替代单纯 topK 堆上下文。

落地位置：

- `RagQueryService`
- `ChunkService`
- 未来 `MemoryContextService`

验收：

- source-required 样本 Citation Accuracy 不下降。
- Groundedness 提升或至少不下降。
- token 使用量可控。

### T2：结构关系参与 rerank

目标：

- 将 chunk 的 `headingHierarchy`、`pageNum`、`contentKind`、`knowledgePointId`、`previous/next` 等元数据纳入 rerank。

落地位置：

- RAG hybrid retrieval / reranker fallback。

验收：

- 多段落解释类问题能命中完整上下文。
- 不引入跨课程/跨 KB 泄露。

### T3：QA Query Rewrite Gate

目标：

- 对多轮追问做 context-aware rewrite。
- 闲聊/越界问题不走重检索。

前置：

- Prompt injection eval。
- Trace 记录 rewrite 输入输出摘要。
- 隐私脱敏。

### T4：HyDE eval-only 分支

目标：

- 只在 evaluation set 或 admin-config gated 模式下试验 HyDE。

验收：

- 对比 baseline：Recall@K、Citation Accuracy、Groundedness、No-source Refusal。
- 若 no-source 幻觉上升，则不得上线。

### T5：MCP ADR

目标：

- 明确是否、何时、如何暴露内部 RAG/QA 工具为 MCP。

前置：

- Auth allowlist。
- Tool schema。
- Trace coverage。
- Sensitive resource policy。

## 6. 不建议照搬的点

1. 不建议引入 Python sidecar 作为主 RAG runtime。本项目后端主栈是 Java/Spring Boot/Spring AI。
2. 不建议迁移到 Milvus。当前已有 Qdrant/VectorIndexAdapter，切库收益不如补结构检索和评测。
3. 不建议直接开放 MCP resource 给课程文档。学生数据、教师资料、课程私有内容都需要强权限和脱敏。
4. 不建议默认开启 HyDE。HyDE 会生成“假想文档”，可能提高召回，也可能放大无来源幻觉。
5. 不建议把它的 OpenAI-compatible wrapper 当作本项目 API 形态。本项目需要业务语义、学习状态、权限和 trace，而不是只伪装成模型。

## 7. 推荐下一步

将本仓库参考吸收到既有路线图中，但不启动迁移：

1. 在 `Memory/RAG Privacy Guard` 后，增加一个 M-size 切片：`RAG POC Context Builder`。
2. 将 `wiki-rag` 的 parent/own/children 思路映射到本项目课程章节、知识点、chunk 相邻关系。
3. 在 Evaluation Set 中加入对照样本，比较 `baseline topK` vs `POC context`。
4. 只有 eval 通过后，才考虑 query rewrite / HyDE。

## 8. Source Links

- GitHub repository: https://github.com/moodlehq/wiki-rag
- README: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/README.md
- `pyproject.toml`: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/pyproject.toml
- `config.yml.template`: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/config.yml.template
- Loader implementation: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/load/util.py
- Index implementation: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/index/util.py
- Search / LangGraph implementation: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/search/util.py
- Milvus vector implementation: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/vector/milvus.py
- OpenAI-compatible server: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/server/server.py
- Server utilities/auth/history filtering: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/server/util.py
- MCP server: https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/mcp_server/server.py
- Releases: https://github.com/moodlehq/wiki-rag/releases
- Security page: https://github.com/moodlehq/wiki-rag/security
