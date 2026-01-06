package com.ryuqq.observability.client.rest;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TraceIdRestTemplateInterceptor 테스트")
class TraceIdRestTemplateInterceptorTest {

    private TraceIdRestTemplateInterceptor interceptor;
    private HttpRequest request;
    private ClientHttpRequestExecution execution;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdRestTemplateInterceptor();
        request = mock(HttpRequest.class);
        execution = mock(ClientHttpRequestExecution.class);
        headers = new HttpHeaders();

        when(request.getHeaders()).thenReturn(headers);
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("intercept 테스트")
    class InterceptTest {

        @Test
        @DisplayName("TraceId를 헤더에 추가한다")
        void shouldAddTraceIdToHeaders() throws IOException {
            TraceIdHolder.set("test-trace-id");
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("UserId를 헤더에 추가한다")
        void shouldAddUserIdToHeaders() throws IOException {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-123");
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_USER_ID)).isEqualTo("user-123");
        }

        @Test
        @DisplayName("TenantId를 헤더에 추가한다")
        void shouldAddTenantIdToHeaders() throws IOException {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setTenantId("tenant-456");
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("OrganizationId를 헤더에 추가한다")
        void shouldAddOrganizationIdToHeaders() throws IOException {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setOrganizationId("org-789");
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-789");
        }

        @Test
        @DisplayName("모든 컨텍스트를 전파한다")
        void shouldPropagateAllContext() throws IOException {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-id");
            TraceIdHolder.setTenantId("tenant-id");
            TraceIdHolder.setOrganizationId("org-id");
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-id");
            assertThat(headers.getFirst(TraceIdHeaders.X_USER_ID)).isEqualTo("user-id");
            assertThat(headers.getFirst(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-id");
            assertThat(headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 헤더를 추가하지 않는다")
        void shouldNotAddHeaderWhenNoTraceId() throws IOException {
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_TRACE_ID)).isNull();
        }

        @Test
        @DisplayName("null 값은 헤더에 추가하지 않는다")
        void shouldNotAddNullValues() throws IOException {
            TraceIdHolder.set("trace-id");
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);

            interceptor.intercept(request, new byte[0], execution);

            assertThat(headers.getFirst(TraceIdHeaders.X_USER_ID)).isNull();
            assertThat(headers.getFirst(TraceIdHeaders.X_TENANT_ID)).isNull();
            assertThat(headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isNull();
        }

        @Test
        @DisplayName("execution을 호출한다")
        void shouldCallExecution() throws IOException {
            ClientHttpResponse mockResponse = mock(ClientHttpResponse.class);
            when(execution.execute(any(), any())).thenReturn(mockResponse);
            byte[] body = "test".getBytes();

            ClientHttpResponse response = interceptor.intercept(request, body, execution);

            verify(execution).execute(request, body);
            assertThat(response).isEqualTo(mockResponse);
        }
    }
}
