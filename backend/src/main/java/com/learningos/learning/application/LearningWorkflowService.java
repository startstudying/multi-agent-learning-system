package com.learningos.learning.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.common.trace.TraceContext;
import com.learningos.knowledge.application.CourseAccessService;
import com.learningos.knowledge.domain.KnowledgeDependency;
import com.learningos.knowledge.domain.KnowledgePoint;
import com.learningos.knowledge.repository.KnowledgeDependencyRepository;
import com.learningos.knowledge.repository.KnowledgePointRepository;
import com.learningos.learning.domain.LearnerProfile;
import com.learningos.learning.domain.LearningEvent;
import com.learningos.learning.domain.LearningPath;
import com.learningos.learning.domain.LearningPathNode;
import com.learningos.learning.domain.MasteryRecord;
import com.learningos.learning.dto.CreateLearningPathRequest;
import com.learningos.learning.dto.LearningPathNodeResponse;
import com.learningos.learning.dto.LearningPathResponse;
import com.learningos.learning.dto.ProfileDimension;
import com.learningos.learning.dto.ProfileDraft;
import com.learningos.learning.dto.ProfileExtractRequest;
import com.learningos.learning.dto.ProfileExtractResponse;
import com.learningos.learning.dto.ProfileStructuredFields;
import com.learningos.learning.dto.ProfileUpdateSourceType;
import com.learningos.learning.repository.LearnerProfileRepository;
import com.learningos.learning.repository.LearningEventRepository;
import com.learningos.learning.repository.LearningPathNodeRepository;
import com.learningos.learning.repository.LearningPathRepository;
import com.learningos.learning.repository.MasteryRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningWorkflowService {

    private static final double COMPLETION_THRESHOLD = 0.80;
    private static final double REMEDIATION_THRESHOLD = 0.60;

    private final Map<String, LearningPathResponse> paths = new ConcurrentHashMap<>();
    private final LearnerProfileRepository learnerProfileRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathNodeRepository learningPathNodeRepository;
    private final LearningEventRepository learningEventRepository;
    private final MasteryRecordRepository masteryRecordRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeDependencyRepository knowledgeDependencyRepository;
    private final CourseAccessService courseAccessService;
    private final ObjectMapper objectMapper;

    public LearningWorkflowService() {
        this(null, null, null, null, null, null, null, null, new ObjectMapper());
    }

    public LearningWorkflowService(
            LearnerProfileRepository learnerProfileRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            LearningEventRepository learningEventRepository,
            MasteryRecordRepository masteryRecordRepository,
            KnowledgePointRepository knowledgePointRepository,
            KnowledgeDependencyRepository knowledgeDependencyRepository,
            ObjectMapper objectMapper
    ) {
        this(
                learnerProfileRepository,
                learningPathRepository,
                learningPathNodeRepository,
                learningEventRepository,
                masteryRecordRepository,
                knowledgePointRepository,
                knowledgeDependencyRepository,
                null,
                objectMapper
        );
    }

    @Autowired
    public LearningWorkflowService(
            LearnerProfileRepository learnerProfileRepository,
            LearningPathRepository learningPathRepository,
            LearningPathNodeRepository learningPathNodeRepository,
            LearningEventRepository learningEventRepository,
            MasteryRecordRepository masteryRecordRepository,
            KnowledgePointRepository knowledgePointRepository,
            KnowledgeDependencyRepository knowledgeDependencyRepository,
            CourseAccessService courseAccessService,
            ObjectMapper objectMapper
    ) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathNodeRepository = learningPathNodeRepository;
        this.learningEventRepository = learningEventRepository;
        this.masteryRecordRepository = masteryRecordRepository;
        this.knowledgePointRepository = knowledgePointRepository;
        this.knowledgeDependencyRepository = knowledgeDependencyRepository;
        this.courseAccessService = courseAccessService;
        this.objectMapper = objectMapper;
    }

    public ProfileExtractResponse extractProfile(ProfileExtractRequest request) {
        String traceId = traceId();
        ProfileUpdateSourceType sourceType = request.sourceTypeOrDefault();
        DialogueSignals signals = DialogueSignals.from(request.message(), sourceType);
        ProfileDraft extractedDraft = extractedDraft(request.learnerId(), signals, sourceType);
        ProfileDraft draft = mergeWithLatestProfile(request.learnerId(), extractedDraft);
        persistLearnerProfile(request.learnerId(), draft, traceId);
        recordLearningEvent(request.learnerId(), "PROFILE_EXTRACTED", request.message(),
                "Extracted a six-dimension learner profile from " + sourceType + ".", traceId);
        return new ProfileExtractResponse(
                draft,
                signals.followUpQuestions(),
                "Profile draft inferred " + signals.summaryReason() + ".",
                traceId
        );
    }

    public LearningPathResponse generatePath(CreateLearningPathRequest request) {
        String traceId = traceId();
        String pathId = stableId("path", request.learnerId(), request.goalId());
        String profileSnapshot = profileSnapshot(request.learnerId());
        LearningPathResponse response = generateCourseDagPath(request, pathId, traceId);
        if (response == null) {
            response = generateTemplatePath(request, pathId, traceId);
        }
        response = withProfileSnapshot(response, profileSnapshot);
        paths.put(pathId, response);
        persistLearningPath(response);
        recordLearningEvent(request.learnerId(), "PATH_GENERATED", request.goalId(),
                "Generated a knowledge DAG path with prerequisite ordering.", traceId);
        return response;
    }

    public LearningPathResponse createPathForUser(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            CreateLearningPathRequest request
    ) {
        if (!currentUserAdmin && !currentUserId.equals(request.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
        }
        if (courseAccessService != null) {
            courseAccessService.requireLearnerEnrolledForExistingCourse(
                    currentUserId,
                    currentUserAdmin,
                    request.learnerId(),
                    request.goalId()
            );
        }
        return generatePath(request);
    }

    private LearningPathResponse generateTemplatePath(CreateLearningPathRequest request, String pathId, String traceId) {
        GoalSignals signals = GoalSignals.from(request.goalId());
        return new LearningPathResponse(
                pathId,
                request.learnerId(),
                request.goalId(),
                "Path for learner " + request.learnerId() + " and goal " + request.goalId()
                        + " prioritizes " + signals.prioritySummary() + ".",
                signals.nodes(request.learnerId(), request.goalId()),
                traceId,
                null
        );
    }

    public LearningPathResponse getPath(String pathId) {
        LearningPathResponse response = paths.get(pathId);
        if (response == null && learningPathRepository != null) {
            response = learningPathRepository.findById(pathId)
                    .map(this::toResponse)
                    .orElse(null);
            if (response != null) {
                paths.put(pathId, response);
            }
        }
        if (response == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Learning path not found");
        }
        return response;
    }

    public LearningPathResponse getPathForUser(String currentUserId, String pathId) {
        return getPathForUser(currentUserId, false, pathId);
    }

    public LearningPathResponse getPathForUser(String currentUserId, boolean currentUserAdmin, String pathId) {
        LearningPathResponse response;
        try {
            response = getPath(pathId);
        } catch (ApiException exception) {
            if (exception.getErrorCode() == ErrorCode.NOT_FOUND && !currentUserAdmin) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
            }
            throw exception;
        }
        if (!currentUserAdmin && !currentUserId.equals(response.learnerId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
        }
        return response;
    }

    private LearningPathResponse generateCourseDagPath(CreateLearningPathRequest request, String pathId, String traceId) {
        if (knowledgePointRepository == null || knowledgeDependencyRepository == null || masteryRecordRepository == null) {
            return null;
        }
        List<KnowledgePoint> points = knowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(request.goalId());
        if (points.isEmpty()) {
            return null;
        }
        Map<String, KnowledgePoint> pointById = points.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<KnowledgeDependency> dependencies = knowledgeDependencyRepository
                .findByKnowledgePointIdInOrPrerequisiteIdIn(pointById.keySet(), pointById.keySet())
                .stream()
                .filter(dependency -> pointById.containsKey(dependency.getKnowledgePointId())
                        && pointById.containsKey(dependency.getPrerequisiteId()))
                .filter(this::isPrerequisiteDependency)
                .toList();
        Map<String, List<String>> prerequisitesByPoint = prerequisitesByPoint(points, dependencies);
        Map<String, List<String>> dependentsByPoint = dependentsByPoint(points, dependencies);
        Map<String, Double> masteryByPoint = latestMasteryByPoint(request.learnerId(), points);
        List<KnowledgePoint> orderedPoints = topologicalOrder(points, prerequisitesByPoint, dependentsByPoint, masteryByPoint);
        List<LearningPathNodeResponse> nodes = orderedPoints.stream()
                .map(point -> toDagNode(
                        point,
                        prerequisitesByPoint.getOrDefault(point.getId(), List.of()),
                        pointById,
                        masteryByPoint,
                        dependentsByPoint
                ))
                .toList();
        return new LearningPathResponse(
                pathId,
                request.learnerId(),
                request.goalId(),
                "Knowledge DAG path for course " + request.goalId()
                        + " orders " + nodes.size() + " knowledge points by prerequisites and current mastery.",
                nodes,
                traceId,
                null
        );
    }

    private boolean isPrerequisiteDependency(KnowledgeDependency dependency) {
        String dependencyType = dependency.getDependencyType();
        return dependencyType != null && "PREREQUISITE".equals(dependencyType.trim().toUpperCase(Locale.ROOT));
    }

    private Map<String, List<String>> prerequisitesByPoint(
            List<KnowledgePoint> points,
            List<KnowledgeDependency> dependencies
    ) {
        Map<String, List<String>> prerequisitesByPoint = new HashMap<>();
        for (KnowledgePoint point : points) {
            prerequisitesByPoint.put(point.getId(), new ArrayList<>());
        }
        for (KnowledgeDependency dependency : dependencies) {
            prerequisitesByPoint.get(dependency.getKnowledgePointId()).add(dependency.getPrerequisiteId());
        }
        return prerequisitesByPoint;
    }

    private Map<String, List<String>> dependentsByPoint(
            List<KnowledgePoint> points,
            List<KnowledgeDependency> dependencies
    ) {
        Map<String, List<String>> dependentsByPoint = new HashMap<>();
        for (KnowledgePoint point : points) {
            dependentsByPoint.put(point.getId(), new ArrayList<>());
        }
        for (KnowledgeDependency dependency : dependencies) {
            dependentsByPoint.get(dependency.getPrerequisiteId()).add(dependency.getKnowledgePointId());
        }
        return dependentsByPoint;
    }

    private List<KnowledgePoint> topologicalOrder(
            List<KnowledgePoint> points,
            Map<String, List<String>> prerequisitesByPoint,
            Map<String, List<String>> dependentsByPoint,
            Map<String, Double> masteryByPoint
    ) {
        List<KnowledgePoint> ordered = new ArrayList<>();
        Set<String> scheduled = new HashSet<>();
        Map<String, Integer> originalOrder = new HashMap<>();
        for (int i = 0; i < points.size(); i++) {
            originalOrder.put(points.get(i).getId(), i);
        }
        while (ordered.size() < points.size()) {
            List<KnowledgePoint> available = points.stream()
                    .filter(point -> !scheduled.contains(point.getId()))
                    .filter(point -> scheduled.containsAll(prerequisitesByPoint.getOrDefault(point.getId(), List.of())))
                    .sorted((left, right) -> comparePathPriority(
                            left,
                            right,
                            prerequisitesByPoint,
                            dependentsByPoint,
                            masteryByPoint,
                            originalOrder
                    ))
                    .toList();
            if (available.isEmpty()) {
                points.stream()
                        .filter(point -> !scheduled.contains(point.getId()))
                        .forEach(point -> {
                            ordered.add(point);
                            scheduled.add(point.getId());
                        });
            } else {
                available.forEach(point -> {
                    ordered.add(point);
                    scheduled.add(point.getId());
                });
            }
        }
        return ordered;
    }

    private int comparePathPriority(
            KnowledgePoint left,
            KnowledgePoint right,
            Map<String, List<String>> prerequisitesByPoint,
            Map<String, List<String>> dependentsByPoint,
            Map<String, Double> masteryByPoint,
            Map<String, Integer> originalOrder
    ) {
        boolean leftRemediation = isRemediationPriority(
                left,
                prerequisitesByPoint.getOrDefault(left.getId(), List.of()),
                dependentsByPoint,
                masteryByPoint
        );
        boolean rightRemediation = isRemediationPriority(
                right,
                prerequisitesByPoint.getOrDefault(right.getId(), List.of()),
                dependentsByPoint,
                masteryByPoint
        );
        if (leftRemediation != rightRemediation) {
            return leftRemediation ? -1 : 1;
        }
        if (leftRemediation) {
            int masteryCompare = Double.compare(
                    masteryByPoint.getOrDefault(left.getId(), 0.0),
                    masteryByPoint.getOrDefault(right.getId(), 0.0)
            );
            if (masteryCompare != 0) {
                return masteryCompare;
            }
        }
        return Integer.compare(
                originalOrder.getOrDefault(left.getId(), Integer.MAX_VALUE),
                originalOrder.getOrDefault(right.getId(), Integer.MAX_VALUE)
        );
    }

    private Map<String, Double> latestMasteryByPoint(String learnerId, List<KnowledgePoint> points) {
        Map<String, Double> masteryByPoint = new HashMap<>();
        for (KnowledgePoint point : points) {
            double mastery = masteryRecordRepository
                    .findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc(learnerId, point.getId())
                    .map(MasteryRecord::getMastery)
                    .orElse(0.0);
            masteryByPoint.put(point.getId(), mastery);
        }
        return masteryByPoint;
    }

    private LearningPathNodeResponse toDagNode(
            KnowledgePoint point,
            List<String> prerequisiteIds,
            Map<String, KnowledgePoint> pointById,
            Map<String, Double> masteryByPoint,
            Map<String, List<String>> dependentsByPoint
    ) {
        double mastery = masteryByPoint.getOrDefault(point.getId(), 0.0);
        boolean done = mastery >= COMPLETION_THRESHOLD;
        List<String> unmetPrerequisiteTitles = prerequisiteIds.stream()
                .filter(prerequisiteId -> masteryByPoint.getOrDefault(prerequisiteId, 0.0) < COMPLETION_THRESHOLD)
                .map(prerequisiteId -> pointById.get(prerequisiteId).getTitle())
                .toList();
        String status = done ? "DONE" : unmetPrerequisiteTitles.isEmpty() ? "ACTIVE" : "LOCKED";
        NodeRecommendationMetadata metadata = recommendationMetadata(point.getId(), status, mastery);
        return new LearningPathNodeResponse(
                point.getId(),
                point.getTitle(),
                status,
                mastery,
                dagReason(
                        point,
                        status,
                        mastery,
                        prerequisiteIds,
                        unmetPrerequisiteTitles,
                        isRemediationPriority(point, prerequisiteIds, dependentsByPoint, masteryByPoint)
                ),
                metadata.recommendationReason(),
                metadata.estimatedDurationMinutes(),
                metadata.resourceType(),
                metadata.assessmentBindingRelation()
        );
    }

    private static NodeRecommendationMetadata recommendationMetadata(String knowledgePointId, String status, double mastery) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String questionBinding = "question_" + knowledgePointId;
        if ("DONE".equals(normalizedStatus)) {
            return new NodeRecommendationMetadata(
                    "Review briefly and confirm retention because this knowledge point already meets the completion threshold.",
                    10,
                    "REVIEW",
                    "MASTERY_CHECK:" + questionBinding
            );
        }
        if ("LOCKED".equals(normalizedStatus)) {
            return new NodeRecommendationMetadata(
                    "Review prerequisite material first because this knowledge point is locked by unmet dependencies.",
                    20,
                    "PREREQUISITE_REVIEW",
                    "LOCKED_UNTIL_PREREQUISITE_MASTERY:" + questionBinding
            );
        }
        if (mastery < REMEDIATION_THRESHOLD) {
            return new NodeRecommendationMetadata(
                    "Start with remediation practice because current mastery is below the remediation threshold.",
                    45,
                    "REMEDIATION",
                    "REMEDIATION_CHECK:" + questionBinding
            );
        }
        return new NodeRecommendationMetadata(
                "Start with a guided lesson because this knowledge point is available and still below the completion threshold.",
                30,
                "LECTURE",
                "FORMATIVE_CHECK:" + questionBinding
        );
    }

    private String dagReason(
            KnowledgePoint point,
            String status,
            double mastery,
            List<String> prerequisiteIds,
            List<String> unmetPrerequisiteTitles,
            boolean remediationPriority
    ) {
        if ("DONE".equals(status)) {
            return "Selected because current mastery " + mastery
                    + " meets the completion threshold in the Knowledge DAG.";
        }
        if ("ACTIVE".equals(status)) {
            if (remediationPriority) {
                String prefix = prerequisiteIds.isEmpty()
                        ? "Prioritized for remediation"
                        : "Selected because prerequisites are satisfied and prioritized for remediation";
                return prefix + " because current mastery " + mastery
                        + " is below the remediation threshold " + REMEDIATION_THRESHOLD
                        + " and this prerequisite unlocks downstream knowledge in the Knowledge DAG.";
            }
            return prerequisiteIds.isEmpty()
                    ? "Selected because this is an entry knowledge point in the Knowledge DAG."
                    : "Selected because prerequisites are satisfied in the Knowledge DAG.";
        }
        return "Locked because prerequisite " + String.join(", ", unmetPrerequisiteTitles)
                + " must be mastered before " + point.getTitle() + " in the Knowledge DAG.";
    }

    private boolean isRemediationPriority(
            KnowledgePoint point,
            List<String> prerequisiteIds,
            Map<String, List<String>> dependentsByPoint,
            Map<String, Double> masteryByPoint
    ) {
        double mastery = masteryByPoint.getOrDefault(point.getId(), 0.0);
        return mastery < REMEDIATION_THRESHOLD
                && !dependentsByPoint.getOrDefault(point.getId(), List.of()).isEmpty()
                && prerequisiteIds.stream()
                .allMatch(prerequisiteId -> masteryByPoint.getOrDefault(prerequisiteId, 0.0) >= COMPLETION_THRESHOLD);
    }

    private ProfileDraft extractedDraft(String learnerId, DialogueSignals signals, ProfileUpdateSourceType sourceType) {
        List<ProfileDimension> dimensions = List.of(
                profileDimension("baseline_level", signals.knowledgeBase(), signals.knowledgeConfidence(),
                        signals.knowledgeEvidence(), sourceType, learnerId),
                profileDimension("learning_goal", signals.target(), signals.goalConfidence(),
                        signals.goalEvidence(), sourceType, learnerId),
                profileDimension("weak_point", String.join(", ", signals.weakPoints()), signals.errorConfidence(),
                        signals.errorEvidence(), sourceType, learnerId),
                profileDimension("preference", String.join(", ", signals.preferences()), signals.preferenceConfidence(),
                        signals.preferenceEvidence(), sourceType, learnerId),
                profileDimension("pace_and_feedback", signals.paceAndFeedback(), signals.paceConfidence(),
                        signals.paceEvidence(), sourceType, learnerId),
                profileDimension("recent_error_pattern", signals.errorPattern(), signals.errorConfidence(),
                        signals.errorEvidence(), sourceType, learnerId),
                profileDimension("teacher_note", teacherNote(signals, sourceType), teacherNoteConfidence(sourceType),
                        teacherNoteEvidence(sourceType), sourceType, learnerId)
        );
        return new ProfileDraft(
                learnerId,
                signals.target(),
                signals.weakPoints(),
                signals.preferences(),
                dimensions,
                new ProfileStructuredFields(
                        signals.knowledgeBase(),
                        signals.target(),
                        signals.preferences(),
                        signals.weakPoints(),
                        signals.paceAndFeedback(),
                        signals.errorPattern(),
                        teacherNote(signals, sourceType),
                        List.of(sourceType)
                ),
                "LEARN_AS_YOU_GO"
        );
    }

    private ProfileDimension profileDimension(
            String name,
            String value,
            double confidence,
            String evidence,
            ProfileUpdateSourceType sourceType,
            String learnerId
    ) {
        return new ProfileDimension(
                name,
                value,
                confidence,
                evidence,
                sourceType,
                stableId("evd", learnerId, sourceTypeOrDefault(sourceType).name(), name, evidence == null ? "" : evidence)
        );
    }

    private ProfileUpdateSourceType sourceTypeOrDefault(ProfileUpdateSourceType sourceType) {
        return sourceType == null ? ProfileUpdateSourceType.CONVERSATION : sourceType;
    }

    private String teacherNote(DialogueSignals signals, ProfileUpdateSourceType sourceType) {
        if (sourceType == ProfileUpdateSourceType.TEACHER_NOTE) {
            return signals.paceAndFeedback();
        }
        return "No teacher note is available yet";
    }

    private double teacherNoteConfidence(ProfileUpdateSourceType sourceType) {
        return sourceType == ProfileUpdateSourceType.TEACHER_NOTE ? 0.86 : 0.30;
    }

    private String teacherNoteEvidence(ProfileUpdateSourceType sourceType) {
        return sourceType == ProfileUpdateSourceType.TEACHER_NOTE
                ? "Teacher note was the profile update source."
                : "No teacher note evidence was provided in this update.";
    }

    private ProfileDraft mergeWithLatestProfile(String learnerId, ProfileDraft extractedDraft) {
        if (learnerProfileRepository == null) {
            return extractedDraft;
        }
        return learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc(learnerId)
                .map(existing -> mergeDraft(existing, extractedDraft))
                .orElse(extractedDraft);
    }

    private ProfileDraft mergeDraft(LearnerProfile existing, ProfileDraft extractedDraft) {
        List<String> weakPoints = mergeTextLists(readStringList(existing.getWeakPointsJson()), extractedDraft.weakPoints());
        List<String> preferences = mergeTextLists(readStringList(existing.getPreferencesJson()), extractedDraft.preferences());
        List<ProfileDimension> dimensions = mergeDimensions(readDimensions(existing.getDimensionsJson()), extractedDraft.dimensions());
        ProfileStructuredFields structuredProfile = structuredProfile(
                existing.getDimensionsJson(),
                existing.getTarget(),
                extractedDraft,
                preferences,
                weakPoints,
                dimensions
        );
        return new ProfileDraft(
                extractedDraft.learnerId(),
                preferredTarget(existing.getTarget(), extractedDraft.target()),
                weakPoints,
                preferences,
                dimensions,
                structuredProfile,
                extractedDraft.updatePolicy()
        );
    }

    private ProfileStructuredFields structuredProfile(
            String existingDimensionsJson,
            String existingTarget,
            ProfileDraft extractedDraft,
            List<String> preferences,
            List<String> weakPoints,
            List<ProfileDimension> dimensions
    ) {
        ProfileStructuredFields previous = readStructuredProfile(existingDimensionsJson, existingTarget);
        return new ProfileStructuredFields(
                strongestDimensionValue(dimensions, "baseline_level", previous.baselineLevel()),
                preferredTarget(previous.learningGoal(), extractedDraft.structuredProfile().learningGoal()),
                preferences,
                weakPoints,
                strongestDimensionValue(dimensions, "pace_and_feedback", previous.paceAndFeedback()),
                strongestDimensionValue(dimensions, "recent_error_pattern", previous.recentErrorPattern()),
                strongestDimensionValue(dimensions, "teacher_note", previous.teacherNote()),
                mergeSources(previous.sources(), extractedDraft.structuredProfile().sources())
        );
    }

    private void persistLearnerProfile(String learnerId, ProfileDraft draft, String traceId) {
        if (learnerProfileRepository == null) {
            return;
        }
        LearnerProfile profile = learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc(learnerId)
                .orElseGet(LearnerProfile::new);
        profile.setLearnerId(learnerId);
        profile.setTarget(draft.target());
        profile.setWeakPointsJson(toJson(draft.weakPoints()));
        profile.setPreferencesJson(toJson(draft.preferences()));
        profile.setDimensionsJson(toJson(profileDimensionsPayload(draft)));
        profile.setUpdatePolicy(draft.updatePolicy());
        profile.setTraceId(traceId);
        learnerProfileRepository.save(profile);
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<ProfileDimension> readDimensions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode dimensionsNode = root.isArray() ? root : root.path("dimensions");
            if (!dimensionsNode.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(dimensionsNode, new TypeReference<>() {
            });
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return List.of();
        }
    }

    private ProfileStructuredFields readStructuredProfile(String json, String existingTarget) {
        List<ProfileDimension> dimensions = readDimensions(json);
        String fallbackBaseline = strongestDimensionValue(dimensions, "baseline_level",
                strongestDimensionValue(dimensions, "knowledge_base", "unknown"));
        String fallbackGoal = strongestDimensionValue(dimensions, "learning_goal", existingTarget);
        String fallbackPace = strongestDimensionValue(
                dimensions,
                "pace_and_feedback",
                "Needs frequent mastery checks with quick feedback"
        );
        String fallbackRecentErrorPattern = strongestDimensionValue(
                dimensions,
                "recent_error_pattern",
                strongestDimensionValue(dimensions, "error_pattern", "No recent error pattern is available yet")
        );
        String fallbackTeacherNote = strongestDimensionValue(
                dimensions,
                "teacher_note",
                "No teacher note is available yet"
        );
        List<ProfileUpdateSourceType> fallbackSources = dimensions.stream()
                .map(ProfileDimension::sourceTypeOrDefault)
                .distinct()
                .toList();
        if (json == null || json.isBlank()) {
            return new ProfileStructuredFields(
                    fallbackBaseline,
                    fallbackGoal,
                    List.of(),
                    List.of(),
                    fallbackPace,
                    fallbackRecentErrorPattern,
                    fallbackTeacherNote,
                    fallbackSources
            );
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return new ProfileStructuredFields(
                        fallbackBaseline,
                        fallbackGoal,
                        List.of(),
                        List.of(),
                        fallbackPace,
                        fallbackRecentErrorPattern,
                        fallbackTeacherNote,
                        fallbackSources
                );
            }
            return new ProfileStructuredFields(
                    textValue(root, "baseline_level", fallbackBaseline),
                    textValue(root, "learning_goal", fallbackGoal),
                    stringArrayOrDimension(root.path("preference"), dimensions, "resource_preference"),
                    stringArrayOrDimension(root.path("weak_point"), dimensions, "weak_point"),
                    textValue(root, "pace_and_feedback", fallbackPace),
                    textValue(root, "recent_error_pattern", fallbackRecentErrorPattern),
                    textValue(root, "teacher_note", fallbackTeacherNote),
                    sourceArray(root.path("sources"), fallbackSources)
            );
        } catch (JsonProcessingException ex) {
            return new ProfileStructuredFields(
                    fallbackBaseline,
                    fallbackGoal,
                    List.of(),
                    List.of(),
                    fallbackPace,
                    fallbackRecentErrorPattern,
                    fallbackTeacherNote,
                    fallbackSources
            );
        }
    }

    private List<String> mergeTextLists(List<String> existing, List<String> extracted) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addNonBlank(merged, existing);
        addNonBlank(merged, extracted);
        return List.copyOf(merged);
    }

    private List<ProfileDimension> mergeDimensions(List<ProfileDimension> existing, List<ProfileDimension> extracted) {
        Map<String, ProfileDimension> merged = new LinkedHashMap<>();
        for (ProfileDimension dimension : existing) {
            merged.put(dimensionKey(dimension), dimension);
        }
        for (ProfileDimension dimension : extracted) {
            String key = dimensionKey(dimension);
            ProfileDimension previous = merged.get(key);
            if (previous == null || dimension.confidence() >= previous.confidence()) {
                merged.put(key, dimension);
            }
        }
        return List.copyOf(merged.values());
    }

    private List<ProfileUpdateSourceType> mergeSources(
            List<ProfileUpdateSourceType> existing,
            List<ProfileUpdateSourceType> extracted
    ) {
        LinkedHashSet<ProfileUpdateSourceType> merged = new LinkedHashSet<>();
        if (existing != null) {
            merged.addAll(existing);
        }
        if (extracted != null) {
            merged.addAll(extracted);
        }
        return List.copyOf(merged);
    }

    private Map<String, Object> profileDimensionsPayload(ProfileDraft draft) {
        ProfileStructuredFields structuredProfile = draft.structuredProfile();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dimensions", draft.dimensions());
        payload.put("baseline_level", structuredProfile.baselineLevel());
        payload.put("learning_goal", structuredProfile.learningGoal());
        payload.put("preference", structuredProfile.preference());
        payload.put("weak_point", structuredProfile.weakPoint());
        payload.put("pace_and_feedback", structuredProfile.paceAndFeedback());
        payload.put("recent_error_pattern", structuredProfile.recentErrorPattern());
        payload.put("teacher_note", structuredProfile.teacherNote());
        payload.put("sources", structuredProfile.sources());
        return payload;
    }

    private String strongestDimensionValue(List<ProfileDimension> dimensions, String name, String fallback) {
        String value = fallback;
        double confidence = -1.0;
        for (ProfileDimension dimension : dimensions) {
            if (name.equals(dimension.name()) && dimension.confidence() > confidence && isNotBlank(dimension.value())) {
                value = dimension.value();
                confidence = dimension.confidence();
            }
        }
        return value;
    }

    private String preferredTarget(String existingTarget, String extractedTarget) {
        if (!isNotBlank(existingTarget)) {
            return extractedTarget;
        }
        if (!isNotBlank(extractedTarget)) {
            return existingTarget;
        }
        boolean existingSpecific = !existingTarget.equals("Build production-grade backend services");
        boolean extractedSpecific = !extractedTarget.equals("Build production-grade backend services");
        if (existingSpecific && !extractedSpecific) {
            return existingTarget;
        }
        if (extractedSpecific && !existingSpecific) {
            return extractedTarget;
        }
        return extractedTarget.length() > existingTarget.length() ? extractedTarget : existingTarget;
    }

    private String dimensionKey(ProfileDimension dimension) {
        return dimension.name() + "|" + dimension.sourceTypeOrDefault();
    }

    private void addNonBlank(LinkedHashSet<String> values, List<String> candidates) {
        for (String candidate : candidates) {
            if (isNotBlank(candidate)) {
                values.add(candidate);
            }
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String textValue(JsonNode root, String field, String fallback) {
        JsonNode value = root.path(field);
        return value.isTextual() && isNotBlank(value.asText()) ? value.asText() : fallback;
    }

    private List<String> stringArray(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual() && isNotBlank(value.asText())) {
                values.add(value.asText());
            }
        });
        return values;
    }

    private List<String> stringArrayOrDimension(JsonNode node, List<ProfileDimension> dimensions, String dimensionName) {
        List<String> values = stringArray(node);
        if (!values.isEmpty()) {
            return values;
        }
        String dimensionValue = strongestDimensionValue(dimensions, dimensionName, null);
        return isNotBlank(dimensionValue) ? List.of(dimensionValue) : List.of();
    }

    private List<ProfileUpdateSourceType> sourceArray(JsonNode node, List<ProfileUpdateSourceType> fallback) {
        if (!node.isArray()) {
            return fallback == null ? List.of() : fallback;
        }
        List<ProfileUpdateSourceType> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.isTextual()) {
                try {
                    values.add(ProfileUpdateSourceType.valueOf(value.asText()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown historical source labels.
                }
            }
        });
        return values.isEmpty() && fallback != null ? fallback : values;
    }

    private void persistLearningPath(LearningPathResponse response) {
        if (learningPathRepository == null || learningPathNodeRepository == null) {
            return;
        }
        LearningPath path = new LearningPath();
        path.setId(response.pathId());
        path.setLearnerId(response.learnerId());
        path.setGoalId(response.goalId());
        path.setReasonSummary(response.reasonSummary());
        path.setStatus("ACTIVE");
        path.setTraceId(response.traceId());
        path.setProfileSnapshot(response.profileSnapshot());
        learningPathRepository.save(path);

        List<LearningPathNode> nodes = new ArrayList<>();
        for (int i = 0; i < response.nodes().size(); i++) {
            LearningPathNodeResponse nodeResponse = response.nodes().get(i);
            LearningPathNode node = new LearningPathNode();
            node.setId(stableId("node", response.learnerId(), response.goalId(), nodeResponse.nodeId()));
            node.setPathId(response.pathId());
            node.setLearnerId(response.learnerId());
            node.setKnowledgePointId(nodeResponse.nodeId());
            node.setTitle(nodeResponse.title());
            node.setStatus(nodeResponse.status());
            node.setMastery(nodeResponse.mastery());
            node.setReasonSummary(nodeResponse.reasonSummary());
            node.setRecommendationReason(nodeResponse.recommendationReason());
            node.setEstimatedDurationMinutes(nodeResponse.estimatedDurationMinutes());
            node.setResourceType(nodeResponse.resourceType());
            node.setAssessmentBindingRelation(nodeResponse.assessmentBindingRelation());
            node.setSequenceNo(i + 1);
            nodes.add(node);
        }
        learningPathNodeRepository.saveAll(nodes);
    }

    private void recordLearningEvent(String learnerId, String eventType, String subjectId, String summary, String traceId) {
        if (learningEventRepository == null) {
            return;
        }
        LearningEvent event = new LearningEvent();
        event.setLearnerId(learnerId);
        event.setEventType(eventType);
        event.setSubjectId(subjectId);
        event.setSummary(summary);
        event.setPayloadJson(toJson(Map.of(
                "eventType", eventType,
                "subjectId", subjectId,
                "summary", summary,
                "traceId", traceId,
                "timestamp", Instant.now().toString()
        )));
        event.setTraceId(traceId);
        learningEventRepository.save(event);
    }

    private LearningPathResponse toResponse(LearningPath path) {
        List<LearningPathNodeResponse> nodes = learningPathNodeRepository.findByPathIdOrderBySequenceNoAsc(path.getId())
                .stream()
                .map(node -> {
                    NodeRecommendationMetadata metadata = fallbackRecommendationMetadata(node);
                    return new LearningPathNodeResponse(
                            node.getKnowledgePointId(),
                            node.getTitle(),
                            node.getStatus(),
                            node.getMastery(),
                            node.getReasonSummary(),
                            metadata.recommendationReason(),
                            metadata.estimatedDurationMinutes(),
                            metadata.resourceType(),
                            metadata.assessmentBindingRelation()
                    );
                })
                .toList();
        return new LearningPathResponse(
                path.getId(),
                path.getLearnerId(),
                path.getGoalId(),
                path.getReasonSummary(),
                nodes,
                path.getTraceId(),
                firstNonBlank(path.getProfileSnapshot(), profileSnapshot(path.getLearnerId()))
        );
    }

    private LearningPathResponse withProfileSnapshot(LearningPathResponse response, String profileSnapshot) {
        return new LearningPathResponse(
                response.pathId(),
                response.learnerId(),
                response.goalId(),
                response.reasonSummary(),
                response.nodes(),
                response.traceId(),
                profileSnapshot
        );
    }

    private String profileSnapshot(String learnerId) {
        ProfileStructuredFields structuredProfile = new ProfileStructuredFields(
                "unknown",
                "unknown",
                List.of(),
                List.of(),
                "Needs frequent mastery checks with quick feedback",
                "No recent error pattern is available yet",
                "No teacher note is available yet",
                List.of()
        );
        String target = "unknown";
        if (learnerProfileRepository != null) {
            LearnerProfile profile = learnerProfileRepository.findFirstByLearnerIdOrderByUpdatedAtDesc(learnerId)
                    .orElse(null);
            if (profile != null) {
                structuredProfile = readStructuredProfile(profile.getDimensionsJson(), profile.getTarget());
                structuredProfile = new ProfileStructuredFields(
                        structuredProfile.baselineLevel(),
                        structuredProfile.learningGoal(),
                        mergeTextLists(structuredProfile.preference(), readStringList(profile.getPreferencesJson())),
                        mergeTextLists(structuredProfile.weakPoint(), readStringList(profile.getWeakPointsJson())),
                        structuredProfile.paceAndFeedback(),
                        structuredProfile.recentErrorPattern(),
                        structuredProfile.teacherNote(),
                        structuredProfile.sources()
                );
                target = firstNonBlank(profile.getTarget(), structuredProfile.learningGoal());
            }
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("learnerId", learnerId);
        snapshot.put("target", target);
        snapshot.put("baseline_level", structuredProfile.baselineLevel());
        snapshot.put("learning_goal", structuredProfile.learningGoal());
        snapshot.put("weak_point", structuredProfile.weakPoint());
        snapshot.put("preference", structuredProfile.preference());
        snapshot.put("pace_and_feedback", structuredProfile.paceAndFeedback());
        snapshot.put("recent_error_pattern", structuredProfile.recentErrorPattern());
        snapshot.put("teacher_note", structuredProfile.teacherNote());
        snapshot.put("sources", structuredProfile.sources());
        return toJson(snapshot);
    }

    private NodeRecommendationMetadata fallbackRecommendationMetadata(LearningPathNode node) {
        NodeRecommendationMetadata fallback = recommendationMetadata(
                node.getKnowledgePointId(),
                node.getStatus(),
                node.getMastery() == null ? 0.0 : node.getMastery()
        );
        return new NodeRecommendationMetadata(
                firstNonBlank(node.getRecommendationReason(), fallback.recommendationReason()),
                node.getEstimatedDurationMinutes() == null || node.getEstimatedDurationMinutes() <= 0
                        ? fallback.estimatedDurationMinutes()
                        : node.getEstimatedDurationMinutes(),
                firstNonBlank(node.getResourceType(), fallback.resourceType()),
                firstNonBlank(node.getAssessmentBindingRelation(), fallback.assessmentBindingRelation())
        );
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String traceId() {
        return TraceContext.currentTraceId().orElse("trc_" + UUID.randomUUID().toString().replace("-", ""));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize learning record", ex);
        }
    }

    private String stableId(String prefix, String... parts) {
        return prefix + "_" + UUID.nameUUIDFromBytes(String.join("|", parts).getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "");
    }

    private record DialogueSignals(
            String target,
            List<String> weakPoints,
            List<String> preferences,
            String knowledgeBase,
            double knowledgeConfidence,
            String knowledgeEvidence,
            double goalConfidence,
            String goalEvidence,
            String cognitiveStyle,
            double cognitiveConfidence,
            String cognitiveEvidence,
            String errorPattern,
            double errorConfidence,
            String errorEvidence,
            double preferenceConfidence,
            String preferenceEvidence,
            String paceAndFeedback,
            double paceConfidence,
            String paceEvidence,
            List<String> followUpQuestions,
            String summaryReason
    ) {

        private static DialogueSignals from(String message, ProfileUpdateSourceType sourceType) {
            String normalized = message.toLowerCase(Locale.ROOT);
            boolean mentionsSpringBoot = containsAny(normalized, "spring boot", "springboot");
            boolean mentionsApi = containsAny(normalized, "api", "apis", "controller", "rest");
            boolean mentionsSqlJoin = containsAny(normalized, "sql", "join", "joins", "cardinality");
            boolean mentionsRag = containsAny(normalized, "rag", "citation", "citations", "grounding");
            boolean mentionsCodeLab = containsAny(normalized, "code lab", "code labs", "coding", "hands-on");
            boolean mentionsDiagram = containsAny(normalized, "diagram", "mind map", "visual");
            boolean mentionsSlowPace = containsAny(normalized, "slowly", "slow", "stuck", "struggled", "took longer");
            boolean mentionsTeacherFeedback = sourceType == ProfileUpdateSourceType.TEACHER_NOTE
                    || containsAny(normalized, "teacher note", "direct feedback", "feedback");

            String target = target(mentionsSpringBoot, mentionsApi);
            List<String> weakPoints = weakPoints(normalized, mentionsSqlJoin, mentionsRag);
            List<String> preferences = preferences(mentionsCodeLab, mentionsDiagram, mentionsTeacherFeedback);

            String weakPointSummary = String.join(" and ", weakPoints);
            String knowledgeBase = mentionsSpringBoot
                    ? "Understands backend goals, needs targeted practice on " + weakPointSummary
                    : "Needs learning goal scoping before targeted practice on " + weakPointSummary;
            String errorPattern = errorPattern(weakPoints, mentionsSqlJoin, normalized);
            double errorConfidence = errorConfidence(sourceType, mentionsSqlJoin);
            String errorEvidence = mentionsSqlJoin
                    ? sourceEvidence(sourceType) + " explicitly mentions SQL JOIN confusion."
                    : sourceEvidence(sourceType) + " supports weak-point update for " + weakPoints.getFirst() + ".";
            String preferenceEvidence = mentionsCodeLab || mentionsDiagram
                    ? sourceEvidence(sourceType) + " directly names preferred resource format."
                    : sourceEvidence(sourceType) + " updates default resource format preferences.";
            String paceAndFeedback = paceAndFeedback(sourceType, mentionsSlowPace, mentionsTeacherFeedback);
            double paceConfidence = sourceType == ProfileUpdateSourceType.TEACHER_NOTE ? 0.86 : mentionsSlowPace ? 0.80 : 0.65;
            String paceEvidence = paceEvidence(sourceType, mentionsSlowPace, mentionsTeacherFeedback);

            return new DialogueSignals(
                    target,
                    weakPoints,
                    preferences,
                    knowledgeBase,
                    mentionsSpringBoot ? 0.82 : 0.62,
                    mentionsSpringBoot
                            ? "Message mentions Spring Boot as the learning domain."
                            : "Message does not name a specific backend framework.",
                    mentionsSpringBoot ? 0.78 : 0.58,
                    mentionsSpringBoot
                            ? "Declared Spring Boot learning target."
                            : "Learning target inferred from generic backend dialogue.",
                    mentionsCodeLab ? "Prefers hands-on code labs before abstractions" : "Prefers concrete worked examples before abstractions",
                    mentionsCodeLab ? 0.76 : 0.68,
                    mentionsCodeLab
                            ? "Message explicitly asks for code-lab style learning."
                            : "Weak-point phrasing suggests example-led remediation.",
                    errorPattern,
                    errorConfidence,
                    errorEvidence,
                    mentionsCodeLab || mentionsDiagram ? 0.78 : 0.66,
                    preferenceEvidence,
                    paceAndFeedback,
                    paceConfidence,
                    paceEvidence,
                    followUpQuestions(mentionsSqlJoin, mentionsRag, mentionsCodeLab),
                    "from target '" + target + "' with weak points " + weakPointSummary
            );
        }

        private static String target(boolean mentionsSpringBoot, boolean mentionsApi) {
            if (mentionsSpringBoot && mentionsApi) {
                return "Build production-grade Spring Boot APIs";
            }
            if (mentionsSpringBoot) {
                return "Build production-grade Spring Boot services";
            }
            return "Build production-grade backend services";
        }

        private static List<String> weakPoints(String normalized, boolean mentionsSqlJoin, boolean mentionsRag) {
            LinkedHashSet<String> weakPoints = new LinkedHashSet<>();
            if (mentionsSqlJoin) {
                weakPoints.add("SQL JOIN reasoning");
            }
            if (mentionsRag) {
                weakPoints.add("RAG grounding");
            }
            if (containsAny(normalized, "transaction boundary", "transaction boundaries", "transactional boundary")) {
                weakPoints.add("transaction boundaries");
            }
            if (weakPoints.isEmpty()) {
                weakPoints.add("Learning goal scoping");
            }
            return List.copyOf(weakPoints);
        }

        private static List<String> preferences(
                boolean mentionsCodeLab,
                boolean mentionsDiagram,
                boolean mentionsTeacherFeedback
        ) {
            LinkedHashSet<String> preferences = new LinkedHashSet<>();
            if (mentionsCodeLab) {
                preferences.add("code labs");
            }
            if (mentionsDiagram) {
                preferences.add("diagrams");
            }
            if (mentionsTeacherFeedback) {
                preferences.add("direct feedback");
            }
            preferences.add("worked examples");
            preferences.add("short assessments");
            return List.copyOf(preferences);
        }

        private static String errorPattern(List<String> weakPoints, boolean mentionsSqlJoin, String normalized) {
            if (mentionsSqlJoin) {
                return "One-to-many JOIN duplication";
            }
            if (containsAny(normalized, "transaction boundary", "transaction boundaries", "transactional boundary")) {
                return "Transaction boundary reasoning";
            }
            return weakPoints.getFirst();
        }

        private static double errorConfidence(ProfileUpdateSourceType sourceType, boolean mentionsSqlJoin) {
            if (sourceType == ProfileUpdateSourceType.ASSESSMENT) {
                return mentionsSqlJoin ? 0.92 : 0.78;
            }
            if (sourceType == ProfileUpdateSourceType.TEACHER_NOTE) {
                return 0.90;
            }
            return mentionsSqlJoin ? 0.88 : 0.60;
        }

        private static String sourceEvidence(ProfileUpdateSourceType sourceType) {
            return switch (sourceType) {
                case CONVERSATION -> "Dialogue";
                case ASSESSMENT -> "Assessment result";
                case RESOURCE_STUDY -> "Resource study record";
                case TEACHER_NOTE -> "Teacher note";
            };
        }

        private static String paceAndFeedback(
                ProfileUpdateSourceType sourceType,
                boolean mentionsSlowPace,
                boolean mentionsTeacherFeedback
        ) {
            if (sourceType == ProfileUpdateSourceType.TEACHER_NOTE || mentionsTeacherFeedback) {
                return "Needs direct feedback after practice checkpoints";
            }
            if (sourceType == ProfileUpdateSourceType.RESOURCE_STUDY && mentionsSlowPace) {
                return "Needs slower pacing with frequent unblock checks";
            }
            if (sourceType == ProfileUpdateSourceType.ASSESSMENT) {
                return "Needs frequent mastery checks with quick remediation";
            }
            return "Needs frequent mastery checks with quick feedback";
        }

        private static String paceEvidence(
                ProfileUpdateSourceType sourceType,
                boolean mentionsSlowPace,
                boolean mentionsTeacherFeedback
        ) {
            if (sourceType == ProfileUpdateSourceType.TEACHER_NOTE || mentionsTeacherFeedback) {
                return "Teacher evidence requests direct feedback.";
            }
            if (sourceType == ProfileUpdateSourceType.RESOURCE_STUDY && mentionsSlowPace) {
                return "Resource study evidence indicates slow or blocked progress.";
            }
            return "Profile should update after each assessment result.";
        }

        private static List<String> followUpQuestions(boolean mentionsSqlJoin, boolean mentionsRag, boolean mentionsCodeLab) {
            if (mentionsSqlJoin && mentionsRag) {
                return List.of(
                        "Which backend project do you want to build first?",
                        "Should the first remediation focus on SQL joins or cited RAG responses?"
                );
            }
            if (mentionsSqlJoin) {
                return List.of(
                        "Which backend project do you want to build first?",
                        mentionsCodeLab
                                ? "Do you want SQL JOIN practice as a code lab or short assessment?"
                                : "Do you prefer code labs or conceptual diagrams?"
                );
            }
            return List.of(
                    "Which backend project do you want to build first?",
                    "What topic currently blocks your progress the most?"
            );
        }

        private static boolean containsAny(String value, String... needles) {
            for (String needle : needles) {
                if (value.contains(needle)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record GoalSignals(String prioritySummary, List<NodeSpec> nodeSpecs) {

        private static GoalSignals from(String goalId) {
            String normalized = goalId.toLowerCase(Locale.ROOT);
            if (normalized.contains("spring")) {
                return new GoalSignals(
                        "HTTP/API prerequisites, SQL persistence risks, then Spring Boot service assembly",
                        List.of(
                                new NodeSpec("prereq_http", "HTTP and controller basics", "READY", 0.68,
                                        "controller concepts are prerequisites for Spring Boot APIs"),
                                new NodeSpec("sql_join", "SQL JOIN duplication diagnosis", "READY", 0.42,
                                        "SQL cardinality errors block reliable persistence work"),
                                new NodeSpec("spring_service", "Build a Spring Boot service flow", "LOCKED", 0.30,
                                        "service implementation should follow controller and SQL grounding")
                        )
                );
            }
            if (normalized.contains("rag")) {
                return new GoalSignals(
                        "retrieval prerequisites, citation grounding, then answer quality checks",
                        List.of(
                                new NodeSpec("retrieval_basics", "Retrieval and chunking basics", "READY", 0.55,
                                        "retrieval concepts are prerequisites for cited answers"),
                                new NodeSpec("citation_grounding", "Cited answer grounding", "READY", 0.36,
                                        "grounding is the core risk in RAG learning goals"),
                                new NodeSpec("answer_quality", "RAG answer quality review", "LOCKED", 0.24,
                                        "quality review depends on retrieval and citation grounding")
                        )
                );
            }
            return new GoalSignals(
                    "goal clarification, prerequisite mapping, then applied project practice",
                    List.of(
                            new NodeSpec("goal_scope", "Clarify learning outcome", "READY", 0.50,
                                    "clear outcomes are prerequisites for adaptive path planning"),
                            new NodeSpec("prereq_map", "Map prerequisite knowledge", "READY", 0.40,
                                    "prerequisite mapping identifies weak points before practice"),
                            new NodeSpec("project_practice", "Apply the goal in a small project", "LOCKED", 0.25,
                                    "project practice should follow scoped prerequisites")
                    )
            );
        }

        private List<LearningPathNodeResponse> nodes(String learnerId, String goalId) {
            return nodeSpecs.stream()
                    .map(spec -> {
                        String nodeId = stableNodeId(learnerId, goalId, spec.key());
                        NodeRecommendationMetadata metadata = LearningWorkflowService
                                .recommendationMetadata(nodeId, spec.status(), spec.mastery());
                        return new LearningPathNodeResponse(
                                nodeId,
                                spec.title(),
                                spec.status(),
                                spec.mastery(),
                                "Selected because " + spec.reason() + ".",
                                metadata.recommendationReason(),
                                metadata.estimatedDurationMinutes(),
                                metadata.resourceType(),
                                metadata.assessmentBindingRelation()
                        );
                    })
                    .toList();
        }

        private static String stableNodeId(String learnerId, String goalId, String key) {
            return "node_" + UUID.nameUUIDFromBytes((learnerId + "|" + goalId + "|" + key)
                            .getBytes(StandardCharsets.UTF_8))
                    .toString()
                    .replace("-", "");
        }
    }

    private record NodeSpec(String key, String title, String status, double mastery, String reason) {
    }

    private record NodeRecommendationMetadata(
            String recommendationReason,
            int estimatedDurationMinutes,
            String resourceType,
            String assessmentBindingRelation
    ) {
    }
}
