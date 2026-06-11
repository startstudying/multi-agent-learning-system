package com.learningos.rag.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.domain.Chapter;
import com.learningos.knowledge.domain.Course;
import com.learningos.knowledge.repository.ChapterRepository;
import com.learningos.rag.api.dto.DocumentDtos.DocumentUploadResponse;
import com.learningos.rag.api.dto.DocumentDtos.IndexTaskDetailResponse;
import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.KnowledgeBaseBindingStatus;
import com.learningos.rag.repository.KbDocumentRepository;
import com.learningos.rag.repository.KbIndexTaskRepository;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import com.learningos.rag.storage.DocumentStorageService;
import com.learningos.rag.storage.StoredObject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class DocumentService {

    private static final int MAX_REQUEST_ID_LENGTH = 120;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final PermissionService permissionService;
    private final IndexService indexService;
    private final KbIndexTaskRepository indexTaskRepository;
    private final CourseAccessService courseAccessService;
    private final ChapterRepository chapterRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public DocumentService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbDocumentRepository documentRepository,
            DocumentStorageService storageService,
            PermissionService permissionService,
            IndexService indexService,
            KbIndexTaskRepository indexTaskRepository,
            CourseAccessService courseAccessService,
            ChapterRepository chapterRepository,
            ObjectMapper objectMapper,
            EntityManager entityManager
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.permissionService = permissionService;
        this.indexService = indexService;
        this.indexTaskRepository = indexTaskRepository;
        this.courseAccessService = courseAccessService;
        this.chapterRepository = chapterRepository;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public DocumentUploadResponse upload(
            String userId,
            String kbId,
            MultipartFile file,
            String courseId,
            String chapterId,
            String requestId
    ) {
        return upload(userId, false, false, kbId, file, courseId, chapterId, requestId);
    }

    @Transactional
    public DocumentUploadResponse upload(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String kbId,
            MultipartFile file,
            String courseId,
            String chapterId,
            String requestId
    ) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(kbId)
                .filter(kb -> kb.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
        ensureCanWrite(userId, currentUserAdmin, currentUserTeacher, kbId);

        String normalizedCourseId = normalizeOptionalMetadata(courseId);
        String normalizedChapterId = normalizeOptionalMetadata(chapterId);
        String normalizedRequestId = normalizeOptionalRequestId(requestId);
        if (normalizedRequestId != null) {
            DocumentUploadResponse replayResponse = replayExistingUploadIfPresent(
                    userId,
                    normalizedRequestId,
                    kbId,
                    normalizedCourseId,
                    normalizedChapterId,
                    file
            );
            if (replayResponse != null) {
                return replayResponse;
            }
        }

        lockUnboundKnowledgeBaseForUpload(userId, currentUserAdmin, currentUserTeacher, knowledgeBase);
        if (normalizedRequestId != null) {
            DocumentUploadResponse replayResponse = replayExistingUploadIfPresent(
                    userId,
                    normalizedRequestId,
                    kbId,
                    normalizedCourseId,
                    normalizedChapterId,
                    file
            );
            if (replayResponse != null) {
                return replayResponse;
            }
        }

        String effectiveCourseId = resolveEffectiveCourseScope(
                userId,
                currentUserAdmin,
                currentUserTeacher,
                knowledgeBase,
                normalizedCourseId,
                normalizedChapterId
        );
        validateCourseChapterScope(userId, currentUserAdmin, currentUserTeacher, effectiveCourseId, normalizedChapterId);

        if (normalizedRequestId == null) {
            return createDocumentUpload(
                    knowledgeBase,
                    userId,
                    kbId,
                    file,
                    effectiveCourseId,
                    normalizedChapterId,
                    null,
                    null
            );
        }

        String requestHash = requestHash(userId, kbId, effectiveCourseId, normalizedChapterId, file);
        try {
            return createDocumentUpload(
                    knowledgeBase,
                    userId,
                    kbId,
                    file,
                    effectiveCourseId,
                    normalizedChapterId,
                    normalizedRequestId,
                    requestHash
            );
        } catch (DataIntegrityViolationException exception) {
            return replayConcurrentUpload(userId, normalizedRequestId, requestHash, exception);
        }
    }

    private DocumentUploadResponse replayExistingUploadIfPresent(
            String userId,
            String requestId,
            String kbId,
            String courseId,
            String chapterId,
            MultipartFile file
    ) {
        KbDocument existingDocument = documentRepository.findByCreatedByAndRequestId(userId, requestId)
                .orElse(null);
        if (existingDocument == null) {
            return null;
        }
        String replayCourseId = courseId == null ? existingDocument.getCourseId() : courseId;
        String requestHash = requestHash(userId, kbId, replayCourseId, chapterId, file);
        return replayExistingUpload(existingDocument, requestHash);
    }

    private void lockUnboundKnowledgeBaseForUpload(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            KnowledgeBase knowledgeBase
    ) {
        if (bindingStatus(knowledgeBase) != KnowledgeBaseBindingStatus.UNBOUND) {
            return;
        }
        entityManager.refresh(knowledgeBase, LockModeType.PESSIMISTIC_WRITE);
        if (knowledgeBase.getDeletedAt() != null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Knowledge base not found");
        }
        ensureCanWrite(userId, currentUserAdmin, currentUserTeacher, knowledgeBase.getId());
    }

    private DocumentUploadResponse createDocumentUpload(
            KnowledgeBase knowledgeBase,
            String userId,
            String kbId,
            MultipartFile file,
            String courseId,
            String chapterId,
            String requestId,
            String requestHash
    ) {
        StoredObject storedObject;
        try {
            storedObject = storageService.store(kbId, file);
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Failed to store document");
        }

        KbDocument document = new KbDocument();
        document.setKnowledgeBase(knowledgeBase);
        document.setKbId(kbId);
        document.setCourseId(courseId);
        document.setChapterId(chapterId);
        document.setName(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        document.setContentType(storedObject.contentType());
        document.setSizeBytes(storedObject.sizeBytes());
        document.setStorageBucket(storedObject.bucket());
        document.setStorageKey(storedObject.key());
        document.setRequestId(requestId);
        document.setRequestHash(requestHash);
        document.setParseStatus(DocumentStatus.PENDING);
        document.setIndexStatus(DocumentStatus.PENDING);
        document.setCreatedBy(userId);
        KbDocument saved = documentRepository.saveAndFlush(document);
        KbIndexTask task = indexService.createPendingTask(saved);
        DocumentUploadResponse response = new DocumentUploadResponse(saved.getId(), task.getId(), task.getStatus());
        if (requestId != null) {
            saved.setResponseJson(toJson(response));
            documentRepository.save(saved);
        }
        return response;
    }

    private String resolveEffectiveCourseScope(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            KnowledgeBase knowledgeBase,
            String requestCourseId,
            String requestChapterId
    ) {
        KnowledgeBaseBindingStatus bindingStatus = bindingStatus(knowledgeBase);
        if (bindingStatus == KnowledgeBaseBindingStatus.CONFLICTED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base course scope denied");
        }
        if (bindingStatus == KnowledgeBaseBindingStatus.BOUND) {
            String kbCourseId = normalizeOptionalMetadata(knowledgeBase.getCourseId());
            if (kbCourseId == null) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base course scope denied");
            }
            if (requestCourseId != null && !kbCourseId.equals(requestCourseId)) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Document course must match knowledge base course");
            }
            return kbCourseId;
        }
        if (requestCourseId == null) {
            if (requestChapterId != null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "courseId is required when chapterId is provided");
            }
            return null;
        }
        if (documentRepository.existsByKbIdAndDeletedAtIsNull(knowledgeBase.getId())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Knowledge base must be course-bound before course document upload");
        }
        Course course = courseAccessService.requireCourseRead(userId, currentUserAdmin, currentUserTeacher, requestCourseId);
        courseAccessService.requireCourseManage(userId, currentUserAdmin, currentUserTeacher, course);
        bindKnowledgeBase(knowledgeBase, userId, course.getId());
        return course.getId();
    }

    private void bindKnowledgeBase(KnowledgeBase knowledgeBase, String userId, String courseId) {
        knowledgeBase.setCourseId(courseId);
        knowledgeBase.setBindingStatus(KnowledgeBaseBindingStatus.BOUND);
        knowledgeBase.setBoundBy(userId);
        knowledgeBase.setBoundAt(java.time.Instant.now());
        knowledgeBaseRepository.save(knowledgeBase);
    }

    @Transactional(readOnly = true)
    public KbDocument getDocument(String userId, String documentId) {
        return getDocument(userId, false, false, documentId);
    }

    @Transactional(readOnly = true)
    public KbDocument getDocument(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String documentId
    ) {
        KbDocument document = loadDocumentForDetail(currentUserAdmin, documentId);
        ensureCanRead(userId, currentUserAdmin, currentUserTeacher, document.getKbId());
        return document;
    }

    @Transactional(readOnly = true)
    public List<KbDocument> listDocuments(String userId, String kbId) {
        return listDocuments(userId, false, false, kbId);
    }

    @Transactional(readOnly = true)
    public List<KbDocument> listDocuments(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String kbId
    ) {
        ensureCanRead(userId, currentUserAdmin, currentUserTeacher, kbId);
        return documentRepository.findByKbIdAndDeletedAtIsNullOrderByCreatedAtDesc(kbId);
    }

    @Transactional
    public DocumentUploadResponse reindex(String userId, String documentId) {
        return reindex(userId, false, false, documentId);
    }

    @Transactional
    public DocumentUploadResponse reindex(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String documentId
    ) {
        KbDocument document = loadDocumentForDetail(currentUserAdmin, documentId);
        ensureCanWrite(userId, currentUserAdmin, currentUserTeacher, document.getKbId());
        document.setIndexStatus(DocumentStatus.PENDING);
        KbIndexTask task = indexService.createPendingTask(document);
        return new DocumentUploadResponse(document.getId(), task.getId(), task.getStatus());
    }

    @Transactional(readOnly = true)
    public IndexTaskDetailResponse getIndexTask(String userId, String taskId) {
        return getIndexTask(userId, false, false, taskId);
    }

    @Transactional(readOnly = true)
    public IndexTaskDetailResponse getIndexTask(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String taskId
    ) {
        KbIndexTask task = indexTaskRepository.findById(taskId)
                .orElseThrow(() -> scopedMissing(currentUserAdmin, "Index task not found", "Knowledge base access denied"));
        ensureCanRead(userId, currentUserAdmin, currentUserTeacher, task.getKbId());
        return IndexTaskDetailResponse.from(task);
    }

    private KbDocument loadDocument(String documentId) {
        return documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Document not found"));
    }

    private KbDocument loadDocumentForDetail(boolean currentUserAdmin, String documentId) {
        return documentRepository.findByIdAndDeletedAtIsNull(documentId)
                .orElseThrow(() -> scopedMissing(currentUserAdmin, "Document not found", "Knowledge base access denied"));
    }

    private void ensureCanRead(String userId, String kbId) {
        ensureCanRead(userId, false, false, kbId);
    }

    private void ensureCanRead(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String kbId) {
        if (!permissionService.canReadKnowledgeBase(userId, currentUserAdmin, currentUserTeacher, kbId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base access denied");
        }
    }

    private void ensureCanWrite(String userId, String kbId) {
        ensureCanWrite(userId, false, false, kbId);
    }

    private void ensureCanWrite(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String kbId) {
        if (!permissionService.canWriteKnowledgeBase(userId, currentUserAdmin, currentUserTeacher, kbId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base write access denied");
        }
    }

    private void validateCourseChapterScope(String userId, String courseId, String chapterId) {
        validateCourseChapterScope(userId, false, false, courseId, chapterId);
    }

    private void validateCourseChapterScope(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId,
            String chapterId
    ) {
        if (courseId == null) {
            if (chapterId != null) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "courseId is required when chapterId is provided");
            }
            return;
        }

        Course course = courseAccessService.requireCourseRead(userId, currentUserAdmin, currentUserTeacher, courseId);
        courseAccessService.requireCourseManage(userId, currentUserAdmin, currentUserTeacher, course);

        if (chapterId == null) {
            return;
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "chapterId must belong to request course"));
        if (!courseId.equals(chapter.getCourseId())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "chapterId must belong to request course");
        }
    }

    private KnowledgeBaseBindingStatus bindingStatus(KnowledgeBase knowledgeBase) {
        return knowledgeBase.getBindingStatus() == null
                ? KnowledgeBaseBindingStatus.UNBOUND
                : knowledgeBase.getBindingStatus();
    }

    private DocumentUploadResponse replayExistingUpload(KbDocument existingDocument, String requestHash) {
        if (!requestHash.equals(existingDocument.getRequestHash())) {
            throw new ApiException(ErrorCode.CONFLICT, "requestId already used with different document upload payload");
        }
        if (existingDocument.getResponseJson() == null || existingDocument.getResponseJson().isBlank()) {
            throw new ApiException(ErrorCode.CONFLICT, "requestId is already being processed");
        }
        try {
            return objectMapper.readValue(existingDocument.getResponseJson(), DocumentUploadResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize document upload response snapshot", exception);
        }
    }

    private DocumentUploadResponse replayConcurrentUpload(
            String userId,
            String requestId,
            String requestHash,
            DataIntegrityViolationException originalException
    ) {
        return documentRepository.findByCreatedByAndRequestId(userId, requestId)
                .map(existingDocument -> replayExistingUpload(existingDocument, requestHash))
                .orElseThrow(() -> originalException);
    }

    private String normalizeOptionalRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        String normalized = requestId.trim();
        if (normalized.length() > MAX_REQUEST_ID_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "requestId length must be less than or equal to 120");
        }
        return normalized;
    }

    private String normalizeOptionalMetadata(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String requestHash(
            String userId,
            String kbId,
            String courseId,
            String chapterId,
            MultipartFile file
    ) {
        String fileHash;
        try {
            fileHash = sha256(file.getBytes());
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "Failed to read document");
        }
        String payload = String.join("\n",
                normalize(userId),
                normalize(kbId),
                normalize(courseId),
                normalize(chapterId),
                normalize(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()),
                normalize(file.getContentType()),
                Long.toString(file.getSize()),
                fileHash,
                "rag-document-upload-v1"
        );
        return sha256(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private ApiException scopedMissing(boolean currentUserAdmin, String adminMessage, String userMessage) {
        return currentUserAdmin
                ? new ApiException(ErrorCode.NOT_FOUND, adminMessage)
                : new ApiException(ErrorCode.FORBIDDEN, userMessage);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize document upload response snapshot", exception);
        }
    }

    private String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
