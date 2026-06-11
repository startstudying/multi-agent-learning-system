package com.learningos.health.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.health.application.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ApiResponse<HealthDtos.HealthResponse> health() {
        return ApiResponse.success(healthService.currentHealth());
    }
}
