# EVIDENCE-20260613-student-ui-design-alignment

## 变更摘要

- 侧栏品牌改为「AI 学习陪伴系统」
- 顶部导航改为「学习工作台 / 查找中心 / 设置」（原「管理后台」）
- 学生主区对齐参考图：顶栏搜索、紫色用户气泡、已深度思考、参考资料卡片、快捷追问、底部输入区
- 右侧上下文面板：学习上下文、来源范围、回答依据、记忆卡片

## 验证

```text
cd frontend && pnpm test -- --run
→ 34 passed

cd frontend && pnpm build
→ 见下方 build 输出
```

## 验收结论

PASS — 前端测试全部通过，UI 文案与布局已对齐参考设计。
