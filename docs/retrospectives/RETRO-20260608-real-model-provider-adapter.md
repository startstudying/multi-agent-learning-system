# Retrospective - P3-3 真实模型 Provider Adapter

## 1. Feature Summary

完成 P3-3 最小真实模型接入边界：Spring AI OpenAI-compatible `ChatModel` 接入 `AiModelGateway`，`EmbeddingModel` 接入 `EmbeddingService`，默认 `provider=none` 保持本地兼容，真实 provider 配置缺 bean 时 fail closed。

## 2. What Went Well

- 先用专家报告限定了最小切片，避免同时引入 DashScope、VectorDB、真实 provider smoke 和管理 UI。
- TDD 很快暴露 Spring AI 1.0.8 API 差异和旧 placeholder 规格冲突，修复范围小。
- 相邻回归暴露 OpenAI starter 默认启用 audio/image/moderation 的隐性自动配置，及时用显式 `none` 关闭。
- 全量后端测试通过，说明 starter 引入没有破坏现有 Resource Generation / RAG / RBAC / observability 测试。

## 3. What Didn't Go Well

- `spring-ai-starter-model-openai` 的自动配置面比 Chat/Embedding 大，单看 starter 名称容易低估 audio/image/moderation 的默认启用风险。
- 部分旧测试仍保留“configured provider 返回 placeholder”的历史语义，与 P3-3 fail-closed 决策冲突，需要同步更新。
- PowerShell 默认输出编码会把中文文档显示成乱码；后续读中文文档应显式 `-Encoding UTF8` 或设置输出编码。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Spring AI provider starter 接入时必须检查所有 auto-config 条件，未用模型族显式 disable | Yes | `docs/skills/project-specific/spring-ai-provider-adapter.md` |
| provider 配置完整但 bean 缺失必须 fail closed，不允许 placeholder 成功 | Yes | `docs/skills/project-specific/model-gateway-boundary.md` 已适用 |
| raw provider error 只映射固定错误码，不进入 memory/evidence/log | Yes | `docs/skills/project-specific/model-gateway-boundary.md` 已适用 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Dependency review | 已做依赖审查，但 starter auto-config 条件直到回归测试才暴露 | 后续 Spring Boot starter 接入必须用 `javap` / metadata / condition report 核对默认启用面 |
| Testing | Focused 后跑 adjacent 才发现 ApplicationContext 启动失败 | 保留 focused -> adjacent SpringBootTest -> full test 的三段验证 |
| Documentation | 中文文档读取偶发乱码 | 中文文档读取阶段显式 UTF-8，避免误判文档内容 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 在受控环境补跑真实 OpenAI-compatible Chat/Embedding smoke | 后续开发者 | P3-3 provider smoke |
| 单独评估 DashScope / Spring AI Alibaba 版本矩阵和依赖审查 | 后续开发者 | P3-3 DashScope adapter |
| 单独接入真实 VectorDB adapter，替换 noop boundary | 后续开发者 | P3-2 VectorDB production adapter |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [ ] SKILL_REGISTRY.md

本次未新增项目专属 skill 文件；仅记录候选模式，暂不更新 `SKILL_REGISTRY.md`。
