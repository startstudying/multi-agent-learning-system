package com.learningos.common.trace;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.DevAuthFilter;
import com.learningos.common.auth.UserContext;
import com.learningos.common.auth.UserContextHolder;
import com.learningos.common.exception.ApiException;
import com.learningos.common.exception.GlobalExceptionHandler;
import com.learningos.common.observability.LearningOsMetrics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(
        controllers = StructuredRequestLoggingFilterTest.TestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import({
        TraceFilter.class,
        DevAuthFilter.class,
        StructuredRequestLoggingFilter.class,
        GlobalExceptionHandler.class,
        CurrentUserService.class,
        StructuredRequestLoggingFilterTest.TestController.class,
        StructuredRequestLoggingFilterTest.MetricsTestConfig.class
})
class StructuredRequestLoggingFilterTest {

    private final MockMvc mockMvc;
    private final MeterRegistry meterRegistry;

    StructuredRequestLoggingFilterTest(MockMvc mockMvc, MeterRegistry meterRegistry) {
        this.mockMvc = mockMvc;
        this.meterRegistry = meterRegistry;
    }

    @Test
    void logsStructuredFieldsForSuccessfulRequest(CapturedOutput output) throws Exception {
        Timer before = httpRequestTimer("/test/structured-log", "200", "OK");
        long beforeCount = before == null ? 0 : before.count();

        mockMvc.perform(get("/test/structured-log")
                        .header(TraceFilter.TRACE_HEADER, "trace-123")
                        .header(DevAuthFilter.USER_ID_HEADER, "alice"))
                .andExpect(status().isOk());

        assertThat(output.getOut())
                .contains("http_request_completed")
                .contains("traceId=trace-123")
                .contains("userId=alice")
                .contains("route=/test/structured-log")
                .contains("status=200")
                .contains("errorCode=OK");
        Timer timer = httpRequestTimer("/test/structured-log", "200", "OK");
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(beforeCount + 1);
        assertNoSensitiveMetricTags();
    }

    @Test
    void replacesUnsafeTraceIdBeforeStructuredLogging(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/structured-log")
                        .header(TraceFilter.TRACE_HEADER, "trace\ninjected")
                        .header(DevAuthFilter.USER_ID_HEADER, "alice"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceFilter.TRACE_HEADER, org.hamcrest.Matchers.not("trace\ninjected")));

        assertThat(output.getOut())
                .contains("http_request_completed")
                .doesNotContain("trace\ninjected")
                .doesNotContain("traceId=trace");
    }

    @Test
    void logsNotFoundErrorCodeForApiException(CapturedOutput output) throws Exception {
        double beforeFailures = httpFailureCount("/test/structured-log/not-found", "404", "NOT_FOUND");

        mockMvc.perform(get("/test/structured-log/not-found")
                        .header(TraceFilter.TRACE_HEADER, "trace-404")
                        .header(DevAuthFilter.USER_ID_HEADER, "alice"))
                .andExpect(status().isNotFound());

        assertThat(output.getOut())
                .contains("http_request_completed")
                .contains("traceId=trace-404")
                .contains("errorCode=NOT_FOUND")
                .contains("status=404")
                .doesNotContain("course not found");
        assertThat(httpFailureCount("/test/structured-log/not-found", "404", "NOT_FOUND"))
                .isEqualTo(beforeFailures + 1.0);
        assertNoSensitiveMetricTags();
    }

    private Timer httpRequestTimer(String route, String status, String errorCode) {
        return meterRegistry.find("learningos.http.server.requests")
                .tag("method", "GET")
                .tag("route", route)
                .tag("status", status)
                .tag("error_code", errorCode)
                .timer();
    }

    private double httpFailureCount(String route, String status, String errorCode) {
        var counter = meterRegistry.find("learningos.http.server.failures")
                .tag("method", "GET")
                .tag("route", route)
                .tag("status", status)
                .tag("error_code", errorCode)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    void logsValidationAndInternalErrorCodes(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/test/structured-log/validate")
                        .header(DevAuthFilter.USER_ID_HEADER, "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/test/structured-log/fail")
                        .header(DevAuthFilter.USER_ID_HEADER, "alice"))
                .andExpect(status().isInternalServerError());

        String log = output.getOut();
        assertThat(log)
                .contains("errorCode=VALIDATION_ERROR")
                .contains("errorCode=INTERNAL_ERROR")
                .doesNotContain("boom");
    }

    @Test
    void fallsBackToMethodAndRequestUriWhenRoutePatternIsMissing(CapturedOutput output) throws Exception {
        StructuredRequestLoggingFilter filter = new StructuredRequestLoggingFilter(new CurrentUserService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unmatched/path");
        request.setQueryString("question=secret-learning-question");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> response.setStatus(204);

        TraceContext.setCurrentTraceId("trace-fallback");
        UserContextHolder.setCurrentUser(new UserContext("bob", "Bob", List.of("USER")));
        try {
            filter.doFilter(request, response, chain);
        } finally {
            TraceContext.clear();
            UserContextHolder.clear();
        }

        assertThat(output.getOut())
                .contains("traceId=trace-fallback")
                .contains("userId=bob")
                .contains("route=GET /unmatched/path")
                .contains("status=204")
                .contains("errorCode=OK")
                .doesNotContain("secret-learning-question");
    }

    private void assertNoSensitiveMetricTags() {
        assertThat(meterRegistry.getMeters().stream()
                .map(Meter::getId)
                .flatMap(id -> id.getTags().stream())
                .map(tag -> tag.getKey())
                .toList())
                .doesNotContain("traceId", "userId", "requestId", "query", "question", "prompt", "errorMessage");
    }

    @TestConfiguration
    static class MetricsTestConfig {

        @Bean
        SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        @Primary
        LearningOsMetrics learningOsMetrics(MeterRegistry meterRegistry) {
            return new LearningOsMetrics(meterRegistry);
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/test/structured-log")
        String ok() {
            return "ok";
        }

        @GetMapping("/test/structured-log/not-found")
        String notFound() {
            throw new ApiException(ErrorCode.NOT_FOUND, "course not found");
        }

        @PostMapping("/test/structured-log/validate")
        void validate(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/test/structured-log/fail")
        void fail() {
            throw new IllegalStateException("boom");
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }
}
