# RUN - 20260609 P3-4-P RAG KB roles-first RBAC Test Expert

## 1. 现有测试基础

- `KnowledgeBaseControllerTest` 当前只有基础 create/list owner 可见测试。
- `DocumentControllerTest` 已覆盖 upload/reindex/document/index-task、course metadata scope、student course metadata spoof deny、missing-vs-foreign anti-enumeration。
- `DevAuthFilterTest` / `CourseKnowledgeControllerTest` / `PromptVersionControllerTest` / `Evaluation*ControllerTest` 提供 Bearer JWT helper 和 roles-first 测试模式。

## 2. 建议 RED 测试

主要修改：

- `KnowledgeBaseControllerTest`
- `DocumentControllerTest`

建议测试：

1. `knowledgeBaseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
   - seed 两个 private KB。
   - Bearer `ADMIN sub=ops_admin` + `X-User-Id=student` list。
   - 预期看到两个 private KB。

2. `bearerAdminCanUploadDocumentToForeignPrivateKnowledgeBaseDespiteSpoofedUserIdHeader`
   - foreign private KB owned by teacher。
   - Bearer admin + spoofed header upload。
   - 预期 200。

3. `bearerTeacherCanUploadOwnCourseMetadataWithoutTeacherIdPrefix`
   - course.teacherId = `instructor_1`。
   - KB owner = `instructor_1`。
   - Bearer `TEACHER sub=instructor_1` 上传 course metadata。
   - 预期 200。

4. `bearerUserSubjectTeacherPrefixCannotUploadCourseMetadata`
   - course.teacherId = `teacher_1`。
   - KB owner = `teacher_1`。
   - Bearer `USER sub=teacher_1` 上传 course metadata。
   - 预期 403，无 document/index 写入。

5. `bearerAdminMissingDocumentAndIndexTaskReturnNotFoundDespiteSpoofedHeader`
   - Bearer admin + spoofed header。
   - missing document/index task。
   - 预期 `NOT_FOUND`。

6. `bearerUserSubjectAdminMissingDocumentAndIndexTaskReturnForbidden`
   - Bearer `USER sub=admin`。
   - missing document/index task。
   - 预期 `FORBIDDEN`。

## 3. 验证命令

RED/GREEN focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
```

Service focused:

```powershell
mvn --% -Dtest=PermissionServiceTest test
```

Adjacent:

```powershell
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
```

Full:

```powershell
mvn test
```

## 4. 注意事项

- 不要每次 RED 都跑 Document 全量相邻测试以外的大范围测试；`DocumentControllerTest` 已较重。
- 列表排序不要依赖固定 index，优先使用 body contains / doesNotContain。
- JWT helper 可短期复制，不在本切片抽公共 test helper，避免扩大变更面。
- 如果保留 student personal KB create，则不要加入 `studentCannotCreateKnowledgeBase` 测试。
