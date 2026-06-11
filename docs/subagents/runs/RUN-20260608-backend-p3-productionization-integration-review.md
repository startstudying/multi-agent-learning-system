# RUN-20260608 后端 P3 生产化 Integration Reviewer

## 角色

Integration Reviewer / Delivery Planner

## 结论

建议不要继续用单一 `backend-architecture-completion` 承载所有剩余 P3 工作，而是创建总控集成文档，并按 P3-4、P3-3、P3-2 拆成可验证切片。

## 推荐切片

1. P3-4 权限矩阵安全前置。
2. P3-3 provider observability schema。
3. P3-3 Spring AI Chat provider adapter。
4. P3-3 Spring AI Embedding provider adapter。
5. P3-2 VectorDB adapter。
6. P3-2 parser layout/page hierarchy。
7. P3-2 OCR fallback dependency review and adapter。

## 集成决策

- 当前先做 P3-4-C，因为接真实模型、Embedding、VectorDB、OCR 前必须先降低数据泄露面。
- subagent 并行分析和设计，代码实现默认单任务串行。
- 新增依赖必须先走 `docs/security/DEPENDENCY-REVIEW-*.md`。

## 验证建议

```powershell
cd backend
mvn test
mvn --% dependency:tree -Dscope=compile
```

按切片追加聚焦测试，并在 Evidence 中记录完整输出。
