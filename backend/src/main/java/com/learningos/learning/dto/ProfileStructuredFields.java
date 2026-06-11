package com.learningos.learning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProfileStructuredFields(
        @JsonProperty("baseline_level")
        String baselineLevel,
        @JsonProperty("learning_goal")
        String learningGoal,
        @JsonProperty("preference")
        List<String> preference,
        @JsonProperty("weak_point")
        List<String> weakPoint,
        @JsonProperty("pace_and_feedback")
        String paceAndFeedback,
        @JsonProperty("recent_error_pattern")
        String recentErrorPattern,
        @JsonProperty("teacher_note")
        String teacherNote,
        List<ProfileUpdateSourceType> sources
) {
}
