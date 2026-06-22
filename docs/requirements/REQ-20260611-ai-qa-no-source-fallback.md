# AI QA 无资料降级回答需求

## 功能需求

1. `/api/ai/qa` 必须先执行当前 RAG 查询流程，以复用权限过滤、检索、traceId 和 query log。
2. 当 RAG 返回 `retrieval.noSource=true` 或引用为空时，AI QA 层必须改写为普通问答降级响应。
3. 降级响应必须明确标识：
   - `sourceStatus=GENERAL_FALLBACK`
   - `sourcePolicy=NO_COURSE_SOURCE_FALLBACK`
4. 当 RAG 有来源时，AI QA 响应必须标识：
   - `sourceStatus=COURSE_GROUNDED`
   - `sourcePolicy=COURSE_RAG`
5. 降级响应不得返回伪造 `sources`。
6. 降级响应的 `reasoningSummary` 必须是安全摘要，不暴露 chain-of-thought、prompt、供应商参数或密钥。
7. 学生端必须根据 `sourceStatus` 展示“课程资料回答”或“通用回答”，不能把无来源降级显示为错误。

## 非功能需求

- 不新增依赖。
- 不新增数据库迁移。
- 后端仍拥有 AI/问答逻辑，前端不得直接调用 LLM API。
- 修改范围限制在 AI QA 合同、学生端展示和对应测试。

## 兼容性

- 响应新增字段必须向后兼容。
- `/api/rag/query` 和 RAG 评测 no-source 规则不变。
