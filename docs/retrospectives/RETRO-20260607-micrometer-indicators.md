# 复盘 - Micrometer 运行指标

## 1. 交付结果

完成 P3-5 Micrometer 业务指标切片，补齐 HTTP、RAG、模型和 token/cost 的运行时观测，并暴露 `metrics` actuator endpoint。

## 2. 做得好的地方

- 用 `LearningOsMetrics` 统一收口，meter name 和 tag 白名单没有散落到业务类里。
- TDD 先暴露了 `@WebMvcTest` 切片兼容问题，避免了把过滤器硬依赖直接留到最后。
- 没有新增依赖，也没有改 API 或数据库 schema。
- 全量 `mvn test` 通过，验证结果明确。

## 3. 不足之处

- `StructuredRequestLoggingFilter` 最初对 `CurrentUserService` 依赖过硬，导致 MVC slice 测试上下文无法装配。
- Security & Quality 子代理因为 `agent thread limit reached` 没能启动，安全审查需要本地补偿。

## 4. 技能沉淀候选

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Micrometer 低基数业务指标适配层、指标白名单、slice 兼容、Actuator 最小暴露 | Yes | `docs/skills/project-specific/micrometer-observability.md` |

## 5. 过程改进

| Area | Current | Proposed |
|---|---|---|
| Filter 依赖 | 直接构造依赖容易破坏 slice 测试 | 对横切依赖优先使用 `ObjectProvider` 或 no-op fallback |
| 可观测性 | 指标语义容易散落 | 先沉淀项目级 metrics adapter，再由业务类调用 |
| 验证 | 只做定向测试不够稳 | 先定向，再全量回归，最后补证据和验收 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 沉淀 Micrometer 项目技能 | Main Codex | docs/skills/project-specific/micrometer-observability.md |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md
- [x] ARCHITECTURE_BASELINE.md

