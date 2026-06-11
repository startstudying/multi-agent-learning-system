# EVIDENCE-20260606-feature-development-workflow-skill-yaml-fix

## 验证对象

`.agents/skills/feature-development-workflow/SKILL.md` 的 YAML frontmatter。

## 根因证据

原 `description` 为未加引号的单行 YAML 标量，并包含 `Keywords:`。YAML 解析器在未加引号标量中遇到冒号映射语法，因此报错：

```text
mapping values are not allowed in this context
```

## 修复证据

`description` 已改为 folded block scalar：

```yaml
description: >
  Use this skill whenever the user gives a feature request, bug fix, refactor,
  ...
```

## 测试命令

```powershell
node -e "const fs=require('fs'); const path=require('path'); const YAML=require('./frontend/node_modules/yaml'); function walk(dir){let out=[]; for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name); if(e.isDirectory()) out=out.concat(walk(p)); else if(e.name==='SKILL.md') out.push(p);} return out;} const files=walk('.agents/skills'); let bad=[]; for(const file of files){const text=fs.readFileSync(file,'utf8'); const m=text.match(/^---\r?\n([\s\S]*?)\r?\n---/); if(!m){bad.push([file,'missing frontmatter']); continue;} try { const doc=YAML.parse(m[1]); if(!doc || typeof doc.name!=='string' || typeof doc.description!=='string') bad.push([file,'missing name/description']); } catch(e){ bad.push([file,e.message]); }} if(bad.length){ for(const [f,msg] of bad) console.log('BAD '+f+': '+msg); process.exit(1);} console.log('OK '+files.length+' local skill(s) parsed');"
```

## 测试结果

```text
OK 1 local skill(s) parsed
```

## 测试限制

未运行 backend/frontend 全量测试，因为本次只修改 Skill frontmatter 和文档，不触及业务代码、构建配置或依赖声明。
