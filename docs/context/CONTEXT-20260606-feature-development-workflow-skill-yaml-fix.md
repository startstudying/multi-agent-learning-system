# CONTEXT-20260606-feature-development-workflow-skill-yaml-fix

## 当前任务边界

仅修复 `feature-development-workflow` Skill 的 YAML frontmatter 解析问题，并补齐项目要求的流程记录文档。

## 相关记忆与文档

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

## Selected Skills

| Skill | 用途 |
|---|---|
| feature-development-workflow | 本次修复目标和流程约束来源 |
| security-review | 确认无密钥、无敏感数据、无依赖新增 |
| architecture-drift-check | 确认不影响既有架构规则 |
| test-generator | 使用解析验证作为本次测试 |

## Subagent Plan

不启用 Subagent。原因：只影响一个 Skill 元数据文件，不涉及两个以上业务模块。

## 允许修改文件

- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/product/PRD-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/requirements/REQ-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/specs/SPEC-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/plans/PLAN-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/tasks/TASK-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/context/CONTEXT-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/evidence/EVIDENCE-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/acceptance/ACCEPT-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

## 不允许修改文件

- `backend/**`
- `frontend/**`
- `docs/superpowers/**`
- 数据库 migration 文件
- 任何真实密钥或私有凭据文件

## Test Commands

```powershell
node -e "const fs=require('fs'); const path=require('path'); const YAML=require('./frontend/node_modules/yaml'); function walk(dir){let out=[]; for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name); if(e.isDirectory()) out=out.concat(walk(p)); else if(e.name==='SKILL.md') out.push(p);} return out;} const files=walk('.agents/skills'); let bad=[]; for(const file of files){const text=fs.readFileSync(file,'utf8'); const m=text.match(/^---\r?\n([\s\S]*?)\r?\n---/); if(!m){bad.push([file,'missing frontmatter']); continue;} try { const doc=YAML.parse(m[1]); if(!doc || typeof doc.name!=='string' || typeof doc.description!=='string') bad.push([file,'missing name/description']); } catch(e){ bad.push([file,e.message]); }} if(bad.length){ for(const [f,msg] of bad) console.log('BAD '+f+': '+msg); process.exit(1);} console.log('OK '+files.length+' local skill(s) parsed');"
```

## Architecture Drift

不涉及后端、前端、Agent/RAG、API 或数据库变更。无架构漂移。
