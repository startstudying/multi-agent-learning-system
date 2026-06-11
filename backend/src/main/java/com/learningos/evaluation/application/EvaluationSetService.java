package com.learningos.evaluation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.evaluation.domain.EvaluationSample;
import com.learningos.evaluation.domain.EvaluationSet;
import com.learningos.evaluation.dto.EvaluationSampleRequest;
import com.learningos.evaluation.dto.EvaluationSampleResponse;
import com.learningos.evaluation.dto.EvaluationSetResponse;
import com.learningos.evaluation.dto.EvaluationSetUpsertRequest;
import com.learningos.evaluation.repository.EvaluationSampleRepository;
import com.learningos.evaluation.repository.EvaluationSetRepository;
import com.learningos.knowledge.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EvaluationSetService {

    private static final String TYPE_RAG_QUESTION = "RAG_QUESTION";
    private static final String TYPE_GRADING_SAMPLE = "GRADING_SAMPLE";
    private static final String TYPE_RESOURCE_GENERATION_SAMPLE = "RESOURCE_GENERATION_SAMPLE";
    private static final List<String> ALLOWED_TYPES = List.of(
            TYPE_RAG_QUESTION,
            TYPE_GRADING_SAMPLE,
            TYPE_RESOURCE_GENERATION_SAMPLE
    );
    private static final List<String> ALLOWED_STATUSES = List.of("DRAFT", "ACTIVE", "ARCHIVED");
    private static final String DEFAULT_STATUS = "DRAFT";

    private final EvaluationSetRepository evaluationSetRepository;
    private final EvaluationSampleRepository evaluationSampleRepository;
    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EvaluationSetService(
            EvaluationSetRepository evaluationSetRepository,
            EvaluationSampleRepository evaluationSampleRepository,
            CourseRepository courseRepository
    ) {
        this.evaluationSetRepository = evaluationSetRepository;
        this.evaluationSampleRepository = evaluationSampleRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public EvaluationSetResponse upsert(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            EvaluationSetUpsertRequest request
    ) {
        assertCanManage(currentUserId, currentUserAdmin, currentUserTeacher, request.courseId());
        String code = requiredTrimmed(request.code(), "Evaluation set code is required");
        String version = requiredTrimmed(request.version(), "Evaluation set version is required");
        String name = requiredTrimmed(request.name(), "Evaluation set name is required");
        String type = normalizeType(request.type());
        String status = normalizeStatus(request.status());

        EvaluationSet evaluationSet = evaluationSetRepository
                .findByCreatedByAndCodeAndVersionAndDeletedAtIsNull(currentUserId, code, version)
                .orElseGet(EvaluationSet::new);
        evaluationSet.setCode(code);
        evaluationSet.setVersion(version);
        evaluationSet.setName(name);
        evaluationSet.setDescription(blankToNull(request.description()));
        evaluationSet.setType(type);
        evaluationSet.setStatus(status);
        evaluationSet.setCourseId(blankToNull(request.courseId()));
        evaluationSet.setKbId(blankToNull(request.kbId()));
        evaluationSet.setPromptCode(blankToNull(request.promptCode()));
        evaluationSet.setPromptVersion(blankToNull(request.promptVersion()));
        evaluationSet.setCreatedBy(currentUserId);
        EvaluationSet savedSet = evaluationSetRepository.save(evaluationSet);

        evaluationSampleRepository.deleteBySetId(savedSet.getId());
        List<EvaluationSample> samples = request.samples().stream()
                .map(sample -> toEntity(savedSet.getId(), type, sample))
                .toList();
        evaluationSampleRepository.saveAll(samples);

        return toResponse(savedSet, evaluationSampleRepository.findBySetIdOrderByCreatedAtAsc(savedSet.getId()));
    }

    @Transactional(readOnly = true)
    public List<EvaluationSetResponse> list(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String type
    ) {
        assertCanUseManagementApi(currentUserAdmin, currentUserTeacher);
        String normalizedType = type == null || type.isBlank() ? null : normalizeType(type);
        List<EvaluationSet> sets = normalizedType == null
                ? evaluationSetRepository.findByDeletedAtIsNullOrderByCreatedAtAsc()
                : evaluationSetRepository.findByTypeAndDeletedAtIsNullOrderByCreatedAtAsc(normalizedType);
        return sets.stream()
                .filter(set -> canRead(currentUserId, currentUserAdmin, currentUserTeacher, set))
                .map(set -> toResponse(set, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationSetResponse get(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String setId
    ) {
        assertCanUseManagementApi(currentUserAdmin, currentUserTeacher);
        EvaluationSet evaluationSet = evaluationSetRepository.findById(setId)
                .filter(set -> set.getDeletedAt() == null)
                .orElseThrow(() -> evaluationSetNotVisible(currentUserAdmin));
        if (!canRead(currentUserId, currentUserAdmin, currentUserTeacher, evaluationSet)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
        }
        return toResponse(evaluationSet, evaluationSampleRepository.findBySetIdOrderByCreatedAtAsc(evaluationSet.getId()));
    }

    private EvaluationSample toEntity(String setId, String type, EvaluationSampleRequest sample) {
        validateSample(type, sample);
        EvaluationSample entity = new EvaluationSample();
        entity.setSetId(setId);
        entity.setSampleType(type);
        entity.setSampleKey(blankToNull(sample.sampleKey()));
        entity.setInputJson(inputJson(type, sample));
        entity.setExpectedJson(expectedJson(type, sample));
        entity.setMetadataJson(metadataJson(sample));
        return entity;
    }

    private void validateSample(String type, EvaluationSampleRequest sample) {
        boolean valid = switch (type) {
            case TYPE_RAG_QUESTION -> notBlank(sample.question()) && !normalizedList(sample.expectedSourceIds()).isEmpty();
            case TYPE_GRADING_SAMPLE -> notBlank(sample.answerText())
                    && notBlank(sample.rubric())
                    && sample.humanScore() != null;
            case TYPE_RESOURCE_GENERATION_SAMPLE -> notBlank(sample.learnerProfileSnapshot())
                    && notBlank(sample.learningGoal())
                    && notBlank(sample.expectedResourceType());
            default -> false;
        };
        if (!valid) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Evaluation sample payload does not match set type");
        }
    }

    private String inputJson(String type, EvaluationSampleRequest sample) {
        Map<String, Object> input = new LinkedHashMap<>();
        switch (type) {
            case TYPE_RAG_QUESTION -> {
                input.put("question", sample.question().trim());
                input.put("topK", sample.topK());
            }
            case TYPE_GRADING_SAMPLE -> {
                input.put("questionId", blankToNull(sample.questionId()));
                input.put("answerText", sample.answerText().trim());
                input.put("rubric", sample.rubric().trim());
            }
            case TYPE_RESOURCE_GENERATION_SAMPLE -> {
                input.put("learnerProfileSnapshot", sample.learnerProfileSnapshot().trim());
                input.put("learningGoal", sample.learningGoal().trim());
            }
            default -> throw new ApiException(ErrorCode.VALIDATION_ERROR, "Evaluation set type is invalid");
        }
        return toJson(input);
    }

    private String expectedJson(String type, EvaluationSampleRequest sample) {
        Map<String, Object> expected = new LinkedHashMap<>();
        switch (type) {
            case TYPE_RAG_QUESTION -> expected.put("expectedSourceIds", normalizedList(sample.expectedSourceIds()));
            case TYPE_GRADING_SAMPLE -> expected.put("humanScore", sample.humanScore());
            case TYPE_RESOURCE_GENERATION_SAMPLE -> expected.put("expectedResourceType", sample.expectedResourceType().trim());
            default -> throw new ApiException(ErrorCode.VALIDATION_ERROR, "Evaluation set type is invalid");
        }
        return toJson(expected);
    }

    private String metadataJson(EvaluationSampleRequest sample) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("qualityCriteria", normalizedList(sample.qualityCriteria()));
        return toJson(metadata);
    }

    private EvaluationSetResponse toResponse(EvaluationSet set, List<EvaluationSample> samples) {
        int sampleCount = Math.toIntExact(evaluationSampleRepository.countBySetId(set.getId()));
        return new EvaluationSetResponse(
                set.getId(),
                set.getCode(),
                set.getVersion(),
                set.getName(),
                set.getDescription(),
                set.getType(),
                set.getStatus(),
                set.getCourseId(),
                set.getKbId(),
                set.getPromptCode(),
                set.getPromptVersion(),
                set.getCreatedBy(),
                sampleCount,
                samples.stream().map(this::toSampleResponse).toList(),
                set.getCreatedAt(),
                set.getUpdatedAt()
        );
    }

    private EvaluationSampleResponse toSampleResponse(EvaluationSample sample) {
        JsonNode input = readJson(sample.getInputJson());
        JsonNode expected = readJson(sample.getExpectedJson());
        JsonNode metadata = readJson(sample.getMetadataJson());
        return new EvaluationSampleResponse(
                sample.getId(),
                sample.getSampleKey(),
                text(input, "question"),
                stringList(expected, "expectedSourceIds"),
                integer(input, "topK"),
                text(input, "questionId"),
                text(input, "answerText"),
                text(input, "rubric"),
                decimal(expected, "humanScore"),
                text(input, "learnerProfileSnapshot"),
                text(input, "learningGoal"),
                text(expected, "expectedResourceType"),
                stringList(metadata, "qualityCriteria")
        );
    }

    private boolean canRead(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            EvaluationSet set
    ) {
        if (currentUserAdmin) {
            return true;
        }
        return currentUserTeacher
                && (currentUserId.equals(set.getCreatedBy()) || isCourseTeacher(currentUserId, set.getCourseId()));
    }

    private void assertCanManage(
            String currentUserId,
            boolean currentUserAdmin,
            boolean currentUserTeacher,
            String courseId
    ) {
        assertCanUseManagementApi(currentUserAdmin, currentUserTeacher);
        if (currentUserAdmin) {
            return;
        }
        if (!currentUserTeacher) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
        }
        if (courseId != null && !courseId.isBlank() && !isCourseTeacher(currentUserId, courseId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
        }
    }

    private void assertCanUseManagementApi(boolean currentUserAdmin, boolean currentUserTeacher) {
        if (currentUserAdmin || currentUserTeacher) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
    }

    private ApiException evaluationSetNotVisible(boolean currentUserAdmin) {
        if (currentUserAdmin) {
            return new ApiException(ErrorCode.NOT_FOUND, "Evaluation set not found");
        }
        return new ApiException(ErrorCode.FORBIDDEN, "Evaluation set access denied");
    }

    private boolean isCourseTeacher(String userId, String courseId) {
        if (userId == null || courseId == null || courseId.isBlank()) {
            return false;
        }
        return courseRepository.findById(courseId)
                .map(course -> userId.equals(course.getTeacherId()))
                .orElse(false);
    }

    private String normalizeType(String type) {
        String normalized = requiredTrimmed(type, "Evaluation set type is required").toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Evaluation set type must be RAG_QUESTION, GRADING_SAMPLE, or RESOURCE_GENERATION_SAMPLE");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return DEFAULT_STATUS;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Evaluation set status must be DRAFT, ACTIVE, or ARCHIVED");
        }
        return normalized;
    }

    private String requiredTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> normalizedList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Evaluation sample serialization failed");
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Evaluation sample parsing failed");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private Double decimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asDouble();
    }

    private List<String> stringList(JsonNode node, String fieldName) {
        JsonNode array = node.path(fieldName);
        if (!array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        array.forEach(value -> {
            if (!value.isNull() && !value.asText().isBlank()) {
                values.add(value.asText().trim());
            }
        });
        return values;
    }
}
