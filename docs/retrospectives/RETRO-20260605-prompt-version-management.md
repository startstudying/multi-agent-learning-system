# Retrospective: Prompt Version 管理基础

## 1. Feature Summary

补齐 `prompt_version` 表的 JPA entity、Repository、Service、Controller、DTO 和目标测试，支持创建/upsert、按 `code/version` 查询、列表查询以及 active 版本查找。

## 2. What Went Well

- 表结构先从 migration 确认，避免误加 `description/hash/is_active` 等不存在字段。
- TDD 覆盖了服务层和 API 层的核心行为。
- 没有修改 `AgentRunRecorder`，降低并行 worker 冲突风险。

## 3. What Didn't Go Well

- 首次 RED 被共享工作区的无关 RAG 编译错误阻塞，说明并行开发时全局编译状态可能短暂不稳定。
- 后续又遇到 analytics 编译状态波动，最终重跑后恢复通过。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 现有表的最小 JPA 管理接口 | No | 无，当前项目已有 Spring 后端规则覆盖 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Testing | 目标测试依赖全模块编译 | 并行 worker 在提交前尽快保持主代码可编译 |
| Documentation | 工作流文档需要每个小任务都补齐 | 小型后端任务可以继续使用精简 PRD/REQ/SPEC/PLAN/TASK |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 补 evaluation set 管理 | 后续 worker | P2-1 子任务 |
| 绑定 `model_call_log` 与 Prompt Version | 后续 worker | P2-1 子任务 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [ ] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md
