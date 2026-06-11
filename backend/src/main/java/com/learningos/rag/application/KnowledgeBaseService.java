package com.learningos.rag.application;

import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.domain.Course;
import com.learningos.rag.api.dto.KnowledgeBaseDtos.CreateKnowledgeBaseRequest;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.KnowledgeBaseBindingStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final PermissionService permissionService;
    private final CourseAccessService courseAccessService;

    public KnowledgeBaseService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            PermissionService permissionService,
            CourseAccessService courseAccessService
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.permissionService = permissionService;
        this.courseAccessService = courseAccessService;
    }

    @Transactional
    public KnowledgeBase create(String userId, CreateKnowledgeBaseRequest request) {
        return create(userId, false, false, request);
    }

    @Transactional
    public KnowledgeBase create(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            CreateKnowledgeBaseRequest request
    ) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.name());
        knowledgeBase.setDescription(request.description());
        knowledgeBase.setVisibility(request.visibility() == null ? Visibility.PRIVATE : request.visibility());
        knowledgeBase.setOwnerUserId(userId);
        knowledgeBase.setCreatedBy(userId);
        String courseId = normalizeOptional(request.courseId());
        if (courseId == null) {
            knowledgeBase.setBindingStatus(KnowledgeBaseBindingStatus.UNBOUND);
        } else {
            Course course = courseAccessService.requireCourseRead(userId, currentUserAdmin, currentUserTeacher, courseId);
            courseAccessService.requireCourseManage(userId, currentUserAdmin, currentUserTeacher, course);
            knowledgeBase.setCourseId(course.getId());
            knowledgeBase.setBindingStatus(KnowledgeBaseBindingStatus.BOUND);
            knowledgeBase.setBoundBy(userId);
            knowledgeBase.setBoundAt(Instant.now());
        }
        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        permissionService.grantOwnerAccess(saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBase> listAccessible(String userId) {
        return permissionService.listAccessibleKnowledgeBases(userId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBase> listAccessible(String userId, boolean currentUserAdmin, boolean currentUserTeacher) {
        return permissionService.listAccessibleKnowledgeBases(userId, currentUserAdmin, currentUserTeacher);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
