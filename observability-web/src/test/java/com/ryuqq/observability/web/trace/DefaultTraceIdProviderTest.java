package com.ryuqq.observability.web.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultTraceIdProvider 테스트")
class DefaultTraceIdProviderTest {

    private DefaultTraceIdProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultTraceIdProvider(List.of(
                "X-Trace-Id",
                "X-Request-Id",
                "traceparent",
                "X-Amzn-Trace-Id"
        ));
    }

    @Nested
    @DisplayName("generate 테스트")
    class GenerateTest {

        @Test
        @DisplayName("UUID 형식의 TraceId를 생성한다")
        void shouldGenerateUuidTraceId() {
            String traceId = provider.generate();

            assertThat(traceId).isNotNull();
            assertThat(traceId).hasSize(32); // UUID without dashes
            assertThat(traceId).matches("[0-9a-f]{32}");
        }

        @Test
        @DisplayName("매번 다른 TraceId를 생성한다")
        void shouldGenerateUniqueTraceId() {
            String traceId1 = provider.generate();
            String traceId2 = provider.generate();

            assertThat(traceId1).isNotEqualTo(traceId2);
        }
    }

    @Nested
    @DisplayName("extractFromRequest 테스트")
    class ExtractFromRequestTest {

        @Test
        @DisplayName("X-Trace-Id 헤더에서 TraceId를 추출한다")
        void shouldExtractFromXTraceId() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Trace-Id", "abc123");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("abc123");
        }

        @Test
        @DisplayName("X-Request-Id 헤더에서 TraceId를 추출한다")
        void shouldExtractFromXRequestId() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-Id", "req-456");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("req-456");
        }

        @Test
        @DisplayName("헤더 우선순위대로 추출한다")
        void shouldRespectHeaderPriority() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-Id", "second");
            request.addHeader("X-Trace-Id", "first");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("first");
        }

        @Test
        @DisplayName("헤더가 없으면 null을 반환한다")
        void shouldReturnNullWhenNoHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isNull();
        }

        @Test
        @DisplayName("빈 헤더 값은 무시한다")
        void shouldIgnoreEmptyHeaderValue() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Trace-Id", "");
            request.addHeader("X-Request-Id", "valid-id");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("valid-id");
        }
    }

    @Nested
    @DisplayName("W3C traceparent 파싱 테스트")
    class W3CTraceParentTest {

        @Test
        @DisplayName("traceparent 헤더에서 TraceId를 추출한다")
        void shouldExtractFromTraceparent() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        }

        @Test
        @DisplayName("짧은 traceparent 형식은 파싱하여 두 번째 부분을 반환한다")
        void shouldReturnSecondPartForShortFormat() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            // "invalid-format"은 하이픈으로 분리되어 "format"이 반환됨
            request.addHeader("traceparent", "invalid-format");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("format");
        }
    }

    @Nested
    @DisplayName("AWS X-Ray 헤더 파싱 테스트")
    class XRayTest {

        @Test
        @DisplayName("X-Amzn-Trace-Id 헤더에서 Root TraceId를 추출한다")
        void shouldExtractFromXRayHeader() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Amzn-Trace-Id", "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("1-5759e988-bd862e3fe1be46a994272793");
        }

        @Test
        @DisplayName("Root가 없는 X-Ray 헤더는 원본 값을 반환한다")
        void shouldReturnOriginalWhenNoRoot() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Amzn-Trace-Id", "Parent=53995c3f42cd8ad8;Sampled=1");

            String traceId = provider.extractFromRequest(request);

            assertThat(traceId).isEqualTo("Parent=53995c3f42cd8ad8;Sampled=1");
        }
    }

    @Nested
    @DisplayName("parseW3CTraceId default 메서드 테스트")
    class ParseW3CTraceIdTest {

        @Test
        @DisplayName("null 입력에 대해 null을 반환한다")
        void shouldReturnNullForNullInput() {
            String result = provider.parseW3CTraceId(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열에 대해 null을 반환한다")
        void shouldReturnNullForEmptyInput() {
            String result = provider.parseW3CTraceId("");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("유효한 traceparent에서 traceId를 추출한다")
        void shouldExtractTraceIdFromValidTraceparent() {
            String result = provider.parseW3CTraceId("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
            assertThat(result).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        }

        @Test
        @DisplayName("하이픈이 하나도 없으면 null을 반환한다")
        void shouldReturnNullForNoHyphens() {
            String result = provider.parseW3CTraceId("invalid");
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("parseXRayTraceId default 메서드 테스트")
    class ParseXRayTraceIdTest {

        @Test
        @DisplayName("null 입력에 대해 null을 반환한다")
        void shouldReturnNullForNullInput() {
            String result = provider.parseXRayTraceId(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열에 대해 null을 반환한다")
        void shouldReturnNullForEmptyInput() {
            String result = provider.parseXRayTraceId("");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("유효한 X-Ray 헤더에서 Root를 추출한다")
        void shouldExtractRootFromValidXRayHeader() {
            String result = provider.parseXRayTraceId("Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8");
            assertThat(result).isEqualTo("1-5759e988-bd862e3fe1be46a994272793");
        }

        @Test
        @DisplayName("Root가 없는 헤더에 대해 null을 반환한다")
        void shouldReturnNullWhenNoRoot() {
            String result = provider.parseXRayTraceId("Parent=53995c3f42cd8ad8;Sampled=1");
            assertThat(result).isNull();
        }
    }
}
