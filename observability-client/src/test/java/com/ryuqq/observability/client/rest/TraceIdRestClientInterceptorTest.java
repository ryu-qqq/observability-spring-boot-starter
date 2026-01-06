package com.ryuqq.observability.client.rest;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TraceIdRestClientInterceptor 테스트")
class TraceIdRestClientInterceptorTest {

    private TraceIdRestClientInterceptor interceptor;
    private ClientHttpRequest request;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdRestClientInterceptor();
        request = mock(ClientHttpRequest.class);
        headers = new HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("initialize 테스트")
    class InitializeTest {

        @Test
        @DisplayName("TraceId를 헤더에 추가한다")
        void shouldAddTraceIdToHeaders() {
            TraceIdHolder.set("test-trace-id");

            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("UserId를 헤더에 추가한다")
        void shouldAddUserIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-123");

            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_USER_ID)).isEqualTo("user-123");
        }

        @Test
        @DisplayName("TenantId를 헤더에 추가한다")
        void shouldAddTenantIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setTenantId("tenant-456");

            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("OrganizationId를 헤더에 추가한다")
        void shouldAddOrganizationIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setOrganizationId("org-789");

            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-789");
        }

        @Test
        @DisplayName("모든 컨텍스트를 전파한다")
        void shouldPropagateAllContext() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-id");
            TraceIdHolder.setTenantId("tenant-id");
            TraceIdHolder.setOrganizationId("org-id");

            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-id");
            assertThat(headers.getFirst(TraceIdHeaders.X_USER_ID)).isEqualTo("user-id");
            assertThat(headers.getFirst(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-id");
            assertThat(headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 헤더를 추가하지 않는다")
        void shouldNotAddHeaderWhenNoTraceId() {
            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_TRACE_ID)).isNull();
        }

        @Test
        @DisplayName("null 값은 헤더에 추가하지 않는다")
        void shouldNotAddNullValues() {
            TraceIdHolder.set("trace-id");

            interceptor.initialize(request);

            assertThat(headers.getFirst(TraceIdHeaders.X_USER_ID)).isNull();
            assertThat(headers.getFirst(TraceIdHeaders.X_TENANT_ID)).isNull();
            assertThat(headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isNull();
        }
    }
}
