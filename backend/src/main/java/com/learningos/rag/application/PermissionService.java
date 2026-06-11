package com.learningos.rag.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.domain.Course;
import com.learningos.rag.domain.KbPermission;
import com.learningos.rag.domain.KnowledgeBase;
import com.learningos.rag.domain.enums.KnowledgeBaseBindingStatus;
import com.learningos.rag.domain.enums.Visibility;
import com.learningos.rag.repository.KbPermissionRepository;
import com.learningos.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private static final String SUBJECT_TYPE_USER = "USER";
    private static final String PERMISSION_OWNER = "OWNER";
    private static final String PERMISSION_READ = "READ";
    private static final String PERMISSION_WRITE = "WRITE";

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbPermissionRepository permissionRepository;
    private final CourseAccessService courseAccessService;

    public PermissionService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KbPermissionRepository permissionRepository,
            CourseAccessService courseAccessService
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.permissionRepository = permissionRepository;
        this.courseAccessService = courseAccessService;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBase> listAccessibleKnowledgeBases(String userId) {
        return listAccessibleKnowledgeBases(userId, false, false);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBase> listAccessibleKnowledgeBases(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher
    ) {
        List<KbPermission> userPermissions = loadUserPermissions(userId);
        return knowledgeBaseRepository.findAll().stream()
                .filter(kb -> kb.getDeletedAt() == null)
                .filter(kb -> canReadKnowledgeBase(kb, userId, currentUserAdmin, currentUserTeacher, userPermissions))
                .sorted((left, right) -> {
                    int comparison = compareInstant(right.getCreatedAt(), left.getCreatedAt());
                    if (comparison != 0) {
                        return comparison;
                    }
                    return left.getId().compareTo(right.getId());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> filterAllowedKbIds(String userId, Collection<String> requestedKbIds) {
        return filterAllowedKbIds(userId, false, false, requestedKbIds);
    }

    @Transactional(readOnly = true)
    public List<String> filterAllowedKbIds(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            Collection<String> requestedKbIds
    ) {
        if (requestedKbIds == null || requestedKbIds.isEmpty()) {
            return List.of();
        }
        Map<String, KnowledgeBase> kbById = knowledgeBaseRepository.findByIdInAndDeletedAtIsNull(requestedKbIds)
                .stream()
                .collect(Collectors.toMap(KnowledgeBase::getId, kb -> kb, (left, right) -> left, LinkedHashMap::new));
        List<KbPermission> userPermissions = loadUserPermissions(userId);
        return requestedKbIds.stream()
                .distinct()
                .filter(kbId -> canReadKnowledgeBase(
                        kbById.get(kbId),
                        userId,
                        currentUserAdmin,
                        currentUserTeacher,
                        userPermissions
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> requireReadableKbIds(String userId, Collection<String> requestedKbIds) {
        return requireReadableKbIds(userId, false, false, requestedKbIds);
    }

    @Transactional(readOnly = true)
    public List<String> requireReadableKbIds(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            Collection<String> requestedKbIds
    ) {
        List<String> normalizedKbIds = normalizeRequestedKbIds(requestedKbIds);
        if (normalizedKbIds.isEmpty()) {
            return List.of();
        }
        Map<String, KnowledgeBase> kbById = knowledgeBaseRepository.findByIdInAndDeletedAtIsNull(normalizedKbIds)
                .stream()
                .collect(Collectors.toMap(KnowledgeBase::getId, kb -> kb, (left, right) -> left, LinkedHashMap::new));
        List<KbPermission> userPermissions = loadUserPermissions(userId);
        List<String> deniedKbIds = normalizedKbIds.stream()
                .filter(kbId -> !canReadKnowledgeBase(
                        kbById.get(kbId),
                        userId,
                        currentUserAdmin,
                        currentUserTeacher,
                        userPermissions
                ))
                .toList();
        if (!deniedKbIds.isEmpty()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "No accessible knowledge bases for this query");
        }
        return normalizedKbIds;
    }

    @Transactional(readOnly = true)
    public boolean canReadKnowledgeBase(String userId, String kbId) {
        return canReadKnowledgeBase(userId, false, false, kbId);
    }

    @Transactional(readOnly = true)
    public boolean canReadKnowledgeBase(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String kbId
    ) {
        return loadKnowledgeBase(kbId)
                .map(kb -> canReadKnowledgeBase(
                        kb,
                        userId,
                        currentUserAdmin,
                        currentUserTeacher,
                        loadUserPermissions(userId)
                ))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canWriteKnowledgeBase(String userId, String kbId) {
        return canWriteKnowledgeBase(userId, false, false, kbId);
    }

    @Transactional(readOnly = true)
    public boolean canWriteKnowledgeBase(
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String kbId
    ) {
        return loadKnowledgeBase(kbId)
                .map(kb -> canWriteKnowledgeBase(
                        kb,
                        userId,
                        currentUserAdmin,
                        currentUserTeacher,
                        loadUserPermissions(userId)
                ))
                .orElse(false);
    }

    @Transactional
    public void grantOwnerAccess(String kbId, String userId) {
        KbPermission permission = new KbPermission();
        permission.setKbId(kbId);
        permission.setSubjectType(SUBJECT_TYPE_USER);
        permission.setSubjectId(userId);
        permission.setPermission(PERMISSION_OWNER);
        permissionRepository.save(permission);
    }

    private boolean canReadKnowledgeBase(KnowledgeBase knowledgeBase, String userId, List<KbPermission> userPermissions) {
        return canReadKnowledgeBase(knowledgeBase, userId, false, false, userPermissions);
    }

    private boolean canReadKnowledgeBase(
            KnowledgeBase knowledgeBase,
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<KbPermission> userPermissions
    ) {
        if (knowledgeBase == null || knowledgeBase.getDeletedAt() != null) {
            return false;
        }
        KnowledgeBaseBindingStatus bindingStatus = bindingStatus(knowledgeBase);
        if (bindingStatus == KnowledgeBaseBindingStatus.CONFLICTED) {
            return currentUserAdmin;
        }
        if (bindingStatus == KnowledgeBaseBindingStatus.BOUND) {
            return canReadCourseBoundKnowledgeBase(knowledgeBase, userId, currentUserAdmin, currentUserTeacher);
        }
        if (currentUserAdmin) {
            return true;
        }
        return userId.equals(knowledgeBase.getOwnerUserId())
                || knowledgeBase.getVisibility() == Visibility.PUBLIC
                || hasPermission(userPermissions, knowledgeBase.getId(), Set.of(PERMISSION_OWNER, PERMISSION_READ, PERMISSION_WRITE));
    }

    private boolean canWriteKnowledgeBase(KnowledgeBase knowledgeBase, String userId, List<KbPermission> userPermissions) {
        return canWriteKnowledgeBase(knowledgeBase, userId, false, false, userPermissions);
    }

    private boolean canWriteKnowledgeBase(
            KnowledgeBase knowledgeBase,
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            List<KbPermission> userPermissions
    ) {
        if (knowledgeBase == null || knowledgeBase.getDeletedAt() != null) {
            return false;
        }
        KnowledgeBaseBindingStatus bindingStatus = bindingStatus(knowledgeBase);
        if (bindingStatus == KnowledgeBaseBindingStatus.CONFLICTED) {
            return currentUserAdmin;
        }
        if (bindingStatus == KnowledgeBaseBindingStatus.BOUND) {
            return canWriteCourseBoundKnowledgeBase(knowledgeBase, userId, currentUserAdmin, currentUserTeacher);
        }
        if (currentUserAdmin) {
            return true;
        }
        return userId.equals(knowledgeBase.getOwnerUserId())
                || hasPermission(userPermissions, knowledgeBase.getId(), Set.of(PERMISSION_OWNER, PERMISSION_WRITE));
    }

    private boolean canReadCourseBoundKnowledgeBase(
            KnowledgeBase knowledgeBase,
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher
    ) {
        if (!hasText(knowledgeBase.getCourseId())) {
            return false;
        }
        try {
            courseAccessService.requireCourseRead(userId, currentUserAdmin, currentUserTeacher, knowledgeBase.getCourseId());
            return true;
        } catch (ApiException exception) {
            return false;
        }
    }

    private boolean canWriteCourseBoundKnowledgeBase(
            KnowledgeBase knowledgeBase,
            String userId,
            boolean currentUserAdmin,
            boolean currentUserTeacher
    ) {
        if (!hasText(knowledgeBase.getCourseId())) {
            return false;
        }
        try {
            Course course = courseAccessService.requireCourseRead(
                    userId,
                    currentUserAdmin,
                    currentUserTeacher,
                    knowledgeBase.getCourseId()
            );
            courseAccessService.requireCourseManage(userId, currentUserAdmin, currentUserTeacher, course);
            return true;
        } catch (ApiException exception) {
            return false;
        }
    }

    private KnowledgeBaseBindingStatus bindingStatus(KnowledgeBase knowledgeBase) {
        return knowledgeBase.getBindingStatus() == null
                ? KnowledgeBaseBindingStatus.UNBOUND
                : knowledgeBase.getBindingStatus();
    }

    private boolean hasPermission(List<KbPermission> userPermissions, String kbId, Set<String> allowedPermissions) {
        return userPermissions.stream()
                .anyMatch(permission -> kbId.equals(permission.getKbId()) && allowedPermissions.contains(permission.getPermission()));
    }

    private List<KbPermission> loadUserPermissions(String userId) {
        return permissionRepository.findAll().stream()
                .filter(permission -> SUBJECT_TYPE_USER.equals(permission.getSubjectType()))
                .filter(permission -> userId.equals(permission.getSubjectId()))
                .toList();
    }

    private List<String> normalizeRequestedKbIds(Collection<String> requestedKbIds) {
        if (requestedKbIds == null) {
            return List.of();
        }
        return requestedKbIds.stream()
                .filter(kbId -> kbId != null && !kbId.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private java.util.Optional<KnowledgeBase> loadKnowledgeBase(String kbId) {
        return knowledgeBaseRepository.findById(kbId)
                .filter(kb -> kb.getDeletedAt() == null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int compareInstant(java.time.Instant left, java.time.Instant right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }
}
