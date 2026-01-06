package com.ryuqq.observability.web.trace;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.web.config.TraceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceIdFilter 테스트")
class TraceIdFilterTest {

    private TraceIdFilter filter;
    private TraceIdProvider traceIdProvider;
    private TraceProperties properties;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new TraceProperties();
        traceIdProvider = new DefaultTraceIdProvider(properties.getHeaderNames());
        filter = new TraceIdFilter(traceIdProvider, properties, "test-service");

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("Order 테스트")
    class OrderTest {

        @Test
        @DisplayName("높은 우선순위를 가진다")
        void shouldHaveHighPriority() {
            assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        }

        @Test
        @DisplayName("ORDER 상수가 올바르게 정의되어 있다")
        void shouldHaveCorrectOrderConstant() {
            assertThat(TraceIdFilter.ORDER).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        }
    }

    @Nested
    @DisplayName("TraceId 추출 테스트")
    class TraceIdExtractionTest {

        @Test
        @DisplayName("요청 헤더에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromHeader() throws ServletException, IOException {
            request.addHeader("X-Trace-Id", "existing-trace-id");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("existing-trace-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 새로 생성한다")
        void shouldGenerateTraceIdIfMissing() throws ServletException, IOException {
            filter.doFilter(request, response, filterChain);

            String responseTraceId = response.getHeader("X-Trace-Id");
            assertThat(responseTraceId).isNotNull();
            assertThat(responseTraceId).hasSize(32);
        }

        @Test
        @DisplayName("generateIfMissing이 false면 TraceId를 생성하지 않는다")
        void shouldNotGenerateTraceIdWhenDisabled() throws ServletException, IOException {
            properties.setGenerateIfMissing(false);
            filter = new TraceIdFilter(traceIdProvider, properties, "test-service");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id")).isNull();
        }
    }

    @Nested
    @DisplayName("응답 헤더 테스트")
    class ResponseHeaderTest {

        @Test
        @DisplayName("응답 헤더에 TraceId를 포함한다")
        void shouldIncludeTraceIdInResponse() throws ServletException, IOException {
            request.addHeader("X-Trace-Id", "test-trace-id");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("includeInResponse가 false면 응답 헤더에 포함하지 않는다")
        void shouldNotIncludeTraceIdWhenDisabled() throws ServletException, IOException {
            properties.setIncludeInResponse(false);
            filter = new TraceIdFilter(traceIdProvider, properties, "test-service");
            request.addHeader("X-Trace-Id", "test-trace-id");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id")).isNull();
        }

        @Test
        @DisplayName("커스텀 응답 헤더 이름을 사용할 수 있다")
        void shouldUseCustomResponseHeaderName() throws ServletException, IOException {
            properties.setResponseHeaderName("X-Custom-Trace-Id");
            filter = new TraceIdFilter(traceIdProvider, properties, "test-service");
            request.addHeader("X-Trace-Id", "test-trace-id");

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("X-Custom-Trace-Id")).isEqualTo("test-trace-id");
        }
    }

    @Nested
    @DisplayName("사용자 컨텍스트 추출 테스트")
    class UserContextExtractionTest {

        @Test
        @DisplayName("X-User-Id 헤더를 추출한다")
        void shouldExtractUserId() throws ServletException, IOException {
            request.addHeader("X-User-Id", "user-123");

            final String[] capturedUserId = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedUserId[0] = TraceIdHolder.getUserId();
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedUserId[0]).isEqualTo("user-123");
        }

        @Test
        @DisplayName("X-Tenant-Id 헤더를 추출한다")
        void shouldExtractTenantId() throws ServletException, IOException {
            request.addHeader("X-Tenant-Id", "tenant-456");

            final String[] capturedTenantId = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedTenantId[0] = TraceIdHolder.getTenantId();
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedTenantId[0]).isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("X-Organization-Id 헤더를 추출한다")
        void shouldExtractOrganizationId() throws ServletException, IOException {
            request.addHeader("X-Organization-Id", "org-789");

            final String[] capturedOrgId = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedOrgId[0] = TraceIdHolder.getOrganizationId();
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedOrgId[0]).isEqualTo("org-789");
        }

        @Test
        @DisplayName("X-User-Roles 헤더를 추출한다")
        void shouldExtractUserRoles() throws ServletException, IOException {
            request.addHeader("X-User-Roles", "ADMIN,USER");

            final String[] capturedRoles = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedRoles[0] = TraceIdHolder.getUserRoles();
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedRoles[0]).isEqualTo("ADMIN,USER");
        }

        @Test
        @DisplayName("빈 헤더 값은 무시한다")
        void shouldIgnoreEmptyHeaders() throws ServletException, IOException {
            request.addHeader("X-User-Id", "");

            final String[] capturedUserId = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedUserId[0] = TraceIdHolder.getUserId();
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedUserId[0]).isNull();
        }
    }

    @Nested
    @DisplayName("서비스 이름 테스트")
    class ServiceNameTest {

        @Test
        @DisplayName("서비스 이름이 설정된다")
        void shouldSetServiceName() throws ServletException, IOException {
            final String[] capturedServiceName = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedServiceName[0] = org.slf4j.MDC.get(TraceIdHeaders.MDC_SERVICE_NAME);
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedServiceName[0]).isEqualTo("test-service");
        }

        @Test
        @DisplayName("null 서비스 이름은 무시된다")
        void shouldIgnoreNullServiceName() throws ServletException, IOException {
            filter = new TraceIdFilter(traceIdProvider, properties, null);

            final String[] capturedServiceName = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedServiceName[0] = org.slf4j.MDC.get(TraceIdHeaders.MDC_SERVICE_NAME);
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedServiceName[0]).isNull();
        }
    }

    @Nested
    @DisplayName("MDC 정리 테스트")
    class MdcCleanupTest {

        @Test
        @DisplayName("필터 종료 후 MDC가 정리된다")
        void shouldClearMdcAfterFilter() throws ServletException, IOException {
            request.addHeader("X-Trace-Id", "test-trace-id");
            request.addHeader("X-User-Id", "user-123");

            filter.doFilter(request, response, filterChain);

            assertThat(TraceIdHolder.get()).isEqualTo("unknown");
            assertThat(TraceIdHolder.getUserId()).isNull();
        }

        @Test
        @DisplayName("예외 발생 시에도 MDC가 정리된다")
        void shouldClearMdcOnException() {
            request.addHeader("X-Trace-Id", "test-trace-id");
            FilterChain throwingChain = (req, res) -> {
                throw new RuntimeException("Test exception");
            };

            try {
                filter.doFilter(request, response, throwingChain);
            } catch (Exception ignored) {
            }

            assertThat(TraceIdHolder.get()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("shouldNotFilter 테스트")
    class ShouldNotFilterTest {

        @Test
        @DisplayName("모든 요청에 대해 필터링한다")
        void shouldFilterAllRequests() {
            boolean result = filter.shouldNotFilter(request);
            assertThat(result).isFalse();
        }
    }
}
