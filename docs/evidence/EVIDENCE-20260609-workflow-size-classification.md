# EVIDENCE-20260609 工作流 S/M/L 分级与父子任务命名

## 变更摘要

本次只调整项目开发工作流规则，不修改业务代码：

- `AGENTS.md` 增加 Size Classification Gate。
- `AGENTS.md` 将 Spec-First、Context Pack、Evidence/Acceptance、Retro、Done Definition 改为 size-aware。
- `.agents/skills/feature-development-workflow/SKILL.md` 同步 S/M/L 分级文档规则。
- 增加父计划 + 语义子任务命名规则，避免继续使用 `P3-4-X/Y/Z`。

## Verification

已执行规则段落检索：

```powershell
Select-String -LiteralPath 'D:\多元agent\AGENTS.md' -Pattern 'Size Classification Gate|Spec-First Rule|Context Pack Rule|Evidence / Acceptance Rule|Retrospective / Skill Extraction Rule|Done Definition|Permission/security cleanup|P3-4 子任务' -Context 2,12
Select-String -LiteralPath 'D:\多元agent\.agents\skills\feature-development-workflow\SKILL.md' -Pattern 'Size Classification Gate|Step 2.5|Create Size-Specific Documents|S - Small|Epic / Parent|S may use' -Context 2,14
```

关键验证结果：

- `AGENTS.md` 已包含 `Size Classification Gate (S / M / L)`。
- `AGENTS.md` 已包含 `S Done Definition`、`M Done Definition`、`L Done Definition`。
- `AGENTS.md` 已声明 S 任务可使用 embedded Context Pack 和 combined Evidence/Acceptance。
- `feature-development-workflow` 已包含 Step 2.5 Size Classification Gate。
- `feature-development-workflow` 已包含 S/M/L required documents 表。
- `feature-development-workflow` 已包含 `Epic / Parent-Child Naming`。

## Acceptance Verdict

PASS。

## Acceptance Criteria

- [x] 小切片不再强制走完整 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/subagent/retro。
- [x] 中/大型任务仍保留必要设计、上下文、验证和收尾纪律。
- [x] 父计划子任务采用语义命名，不再继续 `X/Y/Z` 字母切片。
- [x] 未修改 backend/frontend 业务代码。

## Limitations

- 未运行业务测试；本次为文档/工作流规则变更，无业务代码变更。
- 未清理历史文档中的编码显示问题，避免扩大范围。
