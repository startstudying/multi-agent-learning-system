package com.learningos.learning.dto;

import java.util.List;

public record ProfileDraft(
        String learnerId,
        String target,
        List<String> weakPoints,
        List<String> preferences,
        List<ProfileDimension> dimensions,
        ProfileStructuredFields structuredProfile,
        String updatePolicy
) {
    public ProfileDraft(
            String learnerId,
            String target,
            List<String> weakPoints,
            List<String> preferences,
            List<ProfileDimension> dimensions,
            String updatePolicy
    ) {
        this(
                learnerId,
                target,
                weakPoints,
                preferences,
                dimensions,
                new ProfileStructuredFields(
                        "unknown",
                        target,
                        preferences,
                        weakPoints,
                        "Needs frequent mastery checks with quick feedback",
                        "No recent error pattern is available yet",
                        "No teacher note is available yet",
                        List.of(ProfileUpdateSourceType.CONVERSATION)
                ),
                updatePolicy
        );
    }
}
