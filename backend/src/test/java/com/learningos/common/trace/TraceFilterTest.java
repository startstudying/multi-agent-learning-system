package com.learningos.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFilterTest {

    private final TraceFilter filter = new TraceFilter();

    @Test
    void usesProvidedTraceIdAndClearsContextAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Trace-Id", "trace-from-client");

        filter.doFilter(request, response, assertingCurrentTrace("trace-from-client"));

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-from-client");
        assertThat(TraceContext.currentTraceId()).isEmpty();
    }

    @Test
    void generatesTraceIdWhenMissingAndClearsContextAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.traceIdDuringRequest).isNotBlank();
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(chain.traceIdDuringRequest);
        assertThat(TraceContext.currentTraceId()).isEmpty();
    }

    @Test
    void replacesUnsafeTraceIdBeforeItCanEnterLogs() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();
        request.addHeader("X-Trace-Id", "trace\ninjected");

        filter.doFilter(request, response, chain);

        assertThat(chain.traceIdDuringRequest)
                .isNotBlank()
                .isNotEqualTo("trace\ninjected");
        assertThat(response.getHeader("X-Trace-Id"))
                .isEqualTo(chain.traceIdDuringRequest)
                .doesNotContain("\n");
        assertThat(TraceContext.currentTraceId()).isEmpty();
    }

    private FilterChain assertingCurrentTrace(String expectedTraceId) {
        return (request, response) -> assertThat(TraceContext.currentTraceId()).contains(expectedTraceId);
    }

    private static class CapturingFilterChain implements FilterChain {
        private String traceIdDuringRequest;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            traceIdDuringRequest = TraceContext.currentTraceId().orElse(null);
        }
    }
}
