# Subagent Run - P3-2-G Real Parser SDK Provider Test Plan

## 1. 测试策略

采用 TDD：

1. 先新增 `RealParserProviderTest`，引用未来 provider 与真实 PDFBox/POI 生成的 fixture。
2. RED 预期为 `testCompile` 失败：缺少 provider 类与依赖。
3. 再新增依赖和最小 provider 实现。
4. focused / adjacent / full backend 验证。

## 2. Focused tests

- PDFBox provider：
  - 使用 PDFBox 创建两页 PDF，验证输出两个 section，`pageNum` 为 1/2。
  - 空白/image-only PDF 不产生 section，不 raw bytes fallback。
  - 损坏 PDF 经 `DocumentParserService` 映射为 `DOCUMENT_PARSE_FAILED`。
  - 超过限制的 PDF 经 service 映射为 `DOCUMENT_PARSE_FAILED`。

- POI DOCX provider：
  - 使用 POI 创建 Heading1/Heading2 与正文段落，验证 `headingPath`。
  - 显式 page break 后正文 `pageNum` 递增。
  - `tab` 与普通换行作为空格分隔。
  - 空文档返回空 sections。
  - 超过段落或大小限制经 service 映射为 `DOCUMENT_PARSE_FAILED`。

## 3. Adjacent tests

- `IndexServiceTest` 保持 parser metadata、pageNum、headingPath 不回退。
- `IndexServiceParserFailureTest` 保持 safe error code，不泄露 raw parser message。
- 现有 lightweight behavior tests 继续作为无 Spring provider 场景的兼容保护。

