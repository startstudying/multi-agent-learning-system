# Prompt Version 质量对比需求

## 功能需求

1. 教师或管理员可以为 evaluation set 记录一次 evaluation run。
2. Evaluation run 必须绑定：
   - `evaluationSetId`
   - `setType`
   - `promptCode`
   - `promptVersion`
   - `status`
   - `sampleCount`
   - `createdBy`
3. Evaluation run 可以保存多个质量指标。
4. 质量指标使用白名单：
   - RAG：`recallAtK`、`citationAccuracy`、`groundedness`、`noSourceRefusalRate`
   - 批改：`meanAbsoluteError`、`agreementRate`、`wrongCauseAgreementRate`
   - 资源生成：`qualityScore`、`citationCoverage`、`reviewPassRate`
5. 对比 API 只使用 `SUCCEEDED` run。
6. 对比 API 至少需要两个 prompt version 有可用 run。
7. 对比结果必须包含：
   - baseline prompt version
   - 每个 prompt version 的指标平均值
   - 相对 baseline 的 delta
   - 每个指标的 winner prompt version

## 权限需求

1. `admin` 可以访问所有 evaluation run 和 comparison。
2. evaluation set 创建者可以访问自己的 run 和 comparison。
3. 绑定课程的课程教师可以访问该课程下的 run 和 comparison。
4. 普通学生不能访问 run 记录和 comparison。
5. 非创建者、非课程教师不能访问他人的 evaluation set。

## 数据治理需求

1. 不保存 raw prompt。
2. 不保存 raw model output。
3. 不保存学生原始答案全文。
4. `metadataJson` 只保存白名单摘要字段。
5. 失败 run 只保存安全错误码或短错误摘要。

