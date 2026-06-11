package com.learningos.rag.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.rag.api.dto.DocumentDtos.DocumentStatusResponse;
import com.learningos.rag.api.dto.DocumentDtos.DocumentUploadResponse;
import com.learningos.rag.api.dto.DocumentDtos.IndexTaskDetailResponse;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.application.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    public DocumentController(DocumentService documentService, CurrentUserService currentUserService) {
        this.documentService = documentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/knowledge-bases/{kbId}/documents")
    public ApiResponse<DocumentUploadResponse> upload(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String chapterId,
            @RequestParam(required = false) String requestId
    ) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(documentService.upload(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                kbId,
                file,
                courseId,
                chapterId,
                requestId
        ));
    }

    @GetMapping("/api/knowledge-bases/{kbId}/documents")
    public ApiResponse<List<DocumentStatusResponse>> list(@PathVariable String kbId) {
        UserContext currentUser = currentUserService.currentUser();
        List<DocumentStatusResponse> documents = documentService.listDocuments(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        kbId
                ).stream()
                .map(DocumentStatusResponse::from)
                .toList();
        return ApiResponse.success(documents);
    }

    @GetMapping("/api/documents/{documentId}")
    public ApiResponse<DocumentStatusResponse> get(@PathVariable String documentId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(DocumentStatusResponse.from(
                documentService.getDocument(
                        currentUser.userId(),
                        hasRole(currentUser, "ADMIN"),
                        hasRole(currentUser, "TEACHER"),
                        documentId
                )
        ));
    }

    @PostMapping("/api/documents/{documentId}/reindex")
    public ApiResponse<DocumentUploadResponse> reindex(@PathVariable String documentId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(documentService.reindex(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                documentId
        ));
    }

    @GetMapping("/api/index-tasks/{taskId}")
    public ApiResponse<IndexTaskDetailResponse> getIndexTask(@PathVariable String taskId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(documentService.getIndexTask(
                currentUser.userId(),
                hasRole(currentUser, "ADMIN"),
                hasRole(currentUser, "TEACHER"),
                taskId
        ));
    }

    private boolean hasRole(UserContext currentUser, String role) {
        return currentUser.roles().stream().anyMatch(existingRole -> role.equalsIgnoreCase(existingRole));
    }
}
