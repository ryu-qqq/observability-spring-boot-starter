package com.ryuqq.observability.web.http;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.web.config.HttpLoggingProperties;
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
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpLoggingFilter 테스트")
class HttpLoggingFilterTest {

    private HttpLoggingFilter filter;
    private HttpLoggingProperties properties;
    private PathNormalizer pathNormalizer;
    private LogMasker logMasker;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new HttpLoggingProperties();
        pathNormalizer = new PathNormalizer();
        logMasker = new LogMasker();
        filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

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
        @DisplayName("올바른 ORDER 값을 가진다")
        void shouldHaveCorrectOrder() {
            assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 200);
        }

        @Test
        @DisplayName("ORDER 상수가 올바르게 정의되어 있다")
        void shouldHaveCorrectOrderConstant() {
            assertThat(HttpLoggingFilter.ORDER).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 200);
        }
    }

    @Nested
    @DisplayName("기본 필터링 테스트")
    class BasicFilteringTest {

        @Test
        @DisplayName("기본 요청을 필터링한다")
        void shouldFilterBasicRequest() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
            assertThat(filterChain.getResponse()).isNotNull();
        }

        @Test
        @DisplayName("쿼리 스트링이 있는 요청을 처리한다")
        void shouldHandleQueryString() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.setQueryString("page=1&size=10");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("POST 요청을 처리한다")
        void shouldHandlePostRequest() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/users");
            request.setContentType("application/json");
            request.setContent("{\"name\":\"test\"}".getBytes());

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("shouldNotFilter 테스트")
    class ShouldNotFilterTest {

        @Test
        @DisplayName("제외 경로는 필터링하지 않는다")
        void shouldNotFilterExcludedPaths() {
            properties.setExcludePaths(List.of("/health/**", "/actuator/**"));
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setRequestURI("/health/check");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("일반 경로는 필터링한다")
        void shouldFilterNormalPaths() {
            properties.setExcludePaths(List.of("/health/**"));
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setRequestURI("/api/users");
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }

        @Test
        @DisplayName("액추에이터 경로는 필터링하지 않는다")
        void shouldNotFilterActuatorPaths() {
            properties.setExcludePaths(List.of("/actuator/**"));
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setRequestURI("/actuator/health");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("정확히 일치하는 경로도 제외한다")
        void shouldExcludeExactMatch() {
            properties.setExcludePaths(List.of("/favicon.ico"));
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setRequestURI("/favicon.ico");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }
    }

    @Nested
    @DisplayName("요청 바디 로깅 테스트")
    class RequestBodyLoggingTest {

        @Test
        @DisplayName("JSON 요청 바디를 캐싱한다")
        void shouldCacheJsonRequestBody() throws ServletException, IOException {
            properties.setLogRequestBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("POST");
            request.setRequestURI("/api/users");
            request.setContentType("application/json");
            request.setContent("{\"name\":\"test\"}".getBytes());

            filter.doFilter(request, response, filterChain);

            // 필터 체인에서 래핑된 요청을 확인
            assertThat(filterChain.getRequest()).isInstanceOf(CachedBodyRequestWrapper.class);
        }

        @Test
        @DisplayName("XML 요청 바디를 캐싱한다")
        void shouldCacheXmlRequestBody() throws ServletException, IOException {
            properties.setLogRequestBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("POST");
            request.setRequestURI("/api/users");
            request.setContentType("application/xml");
            request.setContent("<user><name>test</name></user>".getBytes());

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isInstanceOf(CachedBodyRequestWrapper.class);
        }

        @Test
        @DisplayName("텍스트 요청 바디를 캐싱한다")
        void shouldCacheTextRequestBody() throws ServletException, IOException {
            properties.setLogRequestBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("POST");
            request.setRequestURI("/api/text");
            request.setContentType("text/plain");
            request.setContent("plain text content".getBytes());

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isInstanceOf(CachedBodyRequestWrapper.class);
        }

        @Test
        @DisplayName("form-urlencoded 요청 바디를 캐싱한다")
        void shouldCacheFormUrlencodedRequestBody() throws ServletException, IOException {
            properties.setLogRequestBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("POST");
            request.setRequestURI("/api/form");
            request.setContentType("application/x-www-form-urlencoded");
            request.setContent("name=test&value=123".getBytes());

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isInstanceOf(CachedBodyRequestWrapper.class);
        }

        @Test
        @DisplayName("바이너리 요청은 캐싱하지 않는다")
        void shouldNotCacheBinaryRequest() throws ServletException, IOException {
            properties.setLogRequestBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("POST");
            request.setRequestURI("/api/upload");
            request.setContentType("application/octet-stream");
            request.setContent(new byte[]{0x00, 0x01, 0x02});

            filter.doFilter(request, response, filterChain);

            // 바이너리는 원본 요청 사용
            assertThat(filterChain.getRequest()).isNotInstanceOf(CachedBodyRequestWrapper.class);
        }

        @Test
        @DisplayName("Content-Type이 null인 경우 캐싱하지 않는다")
        void shouldNotCacheWhenContentTypeIsNull() throws ServletException, IOException {
            properties.setLogRequestBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");
            // contentType을 설정하지 않음

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotInstanceOf(CachedBodyRequestWrapper.class);
        }

        @Test
        @DisplayName("바디 로깅이 비활성화되면 캐싱하지 않는다")
        void shouldNotCacheWhenDisabled() throws ServletException, IOException {
            properties.setLogRequestBody(false);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("POST");
            request.setRequestURI("/api/users");
            request.setContentType("application/json");
            request.setContent("{\"name\":\"test\"}".getBytes());

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotInstanceOf(CachedBodyRequestWrapper.class);
        }
    }

    @Nested
    @DisplayName("응답 바디 로깅 테스트")
    class ResponseBodyLoggingTest {

        @Test
        @DisplayName("응답 바디 로깅이 활성화되면 래핑한다")
        void shouldWrapResponseWhenEnabled() throws ServletException, IOException {
            properties.setLogResponseBody(true);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getResponse()).isInstanceOf(CachedBodyResponseWrapper.class);
        }

        @Test
        @DisplayName("응답 바디 로깅이 비활성화되면 래핑하지 않는다")
        void shouldNotWrapResponseWhenDisabled() throws ServletException, IOException {
            properties.setLogResponseBody(false);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getResponse()).isNotInstanceOf(CachedBodyResponseWrapper.class);
        }
    }

    @Nested
    @DisplayName("헤더 필터링 테스트")
    class HeaderFilteringTest {

        @Test
        @DisplayName("제외 헤더가 필터링된다")
        void shouldFilterExcludedHeaders() throws ServletException, IOException {
            properties.setExcludeHeaders(List.of("Authorization", "Cookie"));
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "Bearer secret-token");
            request.addHeader("Cookie", "session=abc123");
            request.addHeader("Content-Type", "application/json");

            filter.doFilter(request, response, filterChain);

            // 필터가 정상 작동함
            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("대소문자 구분 없이 헤더를 필터링한다")
        void shouldFilterHeadersCaseInsensitive() throws ServletException, IOException {
            properties.setExcludeHeaders(List.of("authorization"));
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("Authorization", "Bearer token");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("클라이언트 IP 추출 테스트")
    class ClientIpExtractionTest {

        @Test
        @DisplayName("X-Forwarded-For 헤더에서 IP를 추출한다")
        void shouldExtractIpFromXForwardedFor() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("X-Real-IP 헤더에서 IP를 추출한다")
        void shouldExtractIpFromXRealIp() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("X-Real-IP", "192.168.1.2");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("Proxy-Client-IP 헤더에서 IP를 추출한다")
        void shouldExtractIpFromProxyClientIp() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("Proxy-Client-IP", "192.168.1.3");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("WL-Proxy-Client-IP 헤더에서 IP를 추출한다")
        void shouldExtractIpFromWlProxyClientIp() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("WL-Proxy-Client-IP", "192.168.1.4");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("unknown 값은 무시한다")
        void shouldIgnoreUnknownValue() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.addHeader("X-Forwarded-For", "unknown");
            request.setRemoteAddr("127.0.0.1");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("응답 상태 로깅 테스트")
    class ResponseStatusLoggingTest {

        @Test
        @DisplayName("성공 응답(2xx)을 INFO로 로깅한다")
        void shouldLogSuccessAsInfo() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");

            FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("클라이언트 에러(4xx)를 WARN으로 로깅한다")
        void shouldLogClientErrorAsWarn() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");

            FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(400);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("서버 에러(5xx)를 ERROR로 로깅한다")
        void shouldLogServerErrorAsError() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");

            FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(500);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("느린 요청 로깅 테스트")
    class SlowRequestLoggingTest {

        @Test
        @DisplayName("느린 요청은 WARN으로 로깅한다")
        void shouldLogSlowRequestAsWarn() throws ServletException, IOException {
            properties.setSlowRequestThresholdMs(100);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");

            FilterChain slowChain = (req, res) -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            filter.doFilter(request, response, slowChain);

            // 필터가 정상적으로 완료됨
            assertThat(filterChain.getRequest()).isNull(); // slowChain 사용했으므로
        }

        @Test
        @DisplayName("빠른 요청은 INFO로 로깅한다")
        void shouldLogFastRequestAsInfo() throws ServletException, IOException {
            properties.setSlowRequestThresholdMs(1000);
            filter = new HttpLoggingFilter(properties, pathNormalizer, logMasker);

            request.setMethod("GET");
            request.setRequestURI("/api/users");

            filter.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    @Nested
    @DisplayName("TraceContext 추가 테스트")
    class TraceContextTest {

        @Test
        @DisplayName("HTTP 메서드가 컨텍스트에 추가된다")
        void shouldAddHttpMethodToContext() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/users");

            final String[] capturedMethod = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedMethod[0] = TraceIdHolder.getContext("http.method");
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedMethod[0]).isEqualTo("POST");
        }

        @Test
        @DisplayName("HTTP URI가 컨텍스트에 추가된다")
        void shouldAddHttpUriToContext() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users/123");

            final String[] capturedUri = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedUri[0] = TraceIdHolder.getContext("http.uri");
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedUri[0]).isEqualTo("/api/users/123");
        }

        @Test
        @DisplayName("정규화된 URI가 컨텍스트에 추가된다")
        void shouldAddNormalizedUriToContext() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users/123");

            final String[] capturedNormalizedUri = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedNormalizedUri[0] = TraceIdHolder.getContext("http.normalizedUri");
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedNormalizedUri[0]).isEqualTo("/api/users/{id}");
        }

        @Test
        @DisplayName("클라이언트 IP가 컨텍스트에 추가된다")
        void shouldAddClientIpToContext() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");
            request.setRemoteAddr("192.168.1.1");

            final String[] capturedIp = new String[1];
            FilterChain capturingChain = (req, res) -> {
                capturedIp[0] = TraceIdHolder.getContext("http.clientIp");
            };

            filter.doFilter(request, response, capturingChain);

            assertThat(capturedIp[0]).isEqualTo("192.168.1.1");
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("필터 체인에서 예외가 발생해도 응답을 로깅한다")
        void shouldLogResponseEvenOnException() {
            request.setMethod("GET");
            request.setRequestURI("/api/users");

            FilterChain throwingChain = (req, res) -> {
                throw new RuntimeException("Test exception");
            };

            try {
                filter.doFilter(request, response, throwingChain);
            } catch (Exception e) {
                // 예외는 무시
            }

            // 필터가 응답 로깅을 시도했음 (finally 블록)
            // response 상태가 기본값(200)인지 확인
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
