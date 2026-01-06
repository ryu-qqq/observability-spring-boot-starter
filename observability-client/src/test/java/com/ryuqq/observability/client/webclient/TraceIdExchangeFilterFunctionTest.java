package com.ryuqq.observability.client.webclient;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TraceIdExchangeFilterFunction 테스트")
class TraceIdExchangeFilterFunctionTest {

    private TraceIdExchangeFilterFunction filterFunction;
    private ExchangeFunction exchangeFunction;
    private ClientRequest originalRequest;

    @BeforeEach
    void setUp() {
        filterFunction = new TraceIdExchangeFilterFunction();
        exchangeFunction = mock(ExchangeFunction.class);
        originalRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost:8080/test"))
                .build();

        ClientResponse mockResponse = mock(ClientResponse.class);
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(mockResponse));
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("filter 테스트")
    class FilterTest {

        @Test
        @DisplayName("TraceId를 헤더에 추가한다")
        void shouldAddTraceIdToHeaders() {
            TraceIdHolder.set("test-trace-id");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TRACE_ID))
                    .isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("UserId를 헤더에 추가한다")
        void shouldAddUserIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-123");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_USER_ID))
                    .isEqualTo("user-123");
        }

        @Test
        @DisplayName("TenantId를 헤더에 추가한다")
        void shouldAddTenantIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setTenantId("tenant-456");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TENANT_ID))
                    .isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("OrganizationId를 헤더에 추가한다")
        void shouldAddOrganizationIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setOrganizationId("org-789");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_ORGANIZATION_ID))
                    .isEqualTo("org-789");
        }

        @Test
        @DisplayName("모든 컨텍스트를 전파한다")
        void shouldPropagateAllContext() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-id");
            TraceIdHolder.setTenantId("tenant-id");
            TraceIdHolder.setOrganizationId("org-id");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-id");
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_USER_ID)).isEqualTo("user-id");
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-id");
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 헤더를 추가하지 않는다")
        void shouldNotAddHeaderWhenNoTraceId() {
            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TRACE_ID)).isNull();
        }

        @Test
        @DisplayName("null 값은 헤더에 추가하지 않는다")
        void shouldNotAddNullValues() {
            TraceIdHolder.set("trace-id");
            // userId, tenantId, organizationId를 설정하지 않음

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-id");
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_USER_ID)).isNull();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_TENANT_ID)).isNull();
            assertThat(capturedRequest.headers().getFirst(TraceIdHeaders.X_ORGANIZATION_ID)).isNull();
        }

        @Test
        @DisplayName("원본 요청의 URI를 유지한다")
        void shouldPreserveOriginalUri() {
            TraceIdHolder.set("trace-id");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.url()).isEqualTo(URI.create("http://localhost:8080/test"));
        }

        @Test
        @DisplayName("원본 요청의 메서드를 유지한다")
        void shouldPreserveOriginalMethod() {
            TraceIdHolder.set("trace-id");

            filterFunction.filter(originalRequest, exchangeFunction).block();

            ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
            verify(exchangeFunction).exchange(captor.capture());

            ClientRequest capturedRequest = captor.getValue();
            assertThat(capturedRequest.method()).isEqualTo(HttpMethod.GET);
        }
    }

    @Nested
    @DisplayName("create 테스트")
    class CreateTest {

        @Test
        @DisplayName("새 인스턴스를 생성한다")
        void shouldCreateNewInstance() {
            TraceIdExchangeFilterFunction instance = TraceIdExchangeFilterFunction.create();

            assertThat(instance).isNotNull();
            assertThat(instance).isInstanceOf(TraceIdExchangeFilterFunction.class);
        }

        @Test
        @DisplayName("매번 새로운 인스턴스를 반환한다")
        void shouldReturnNewInstanceEachTime() {
            TraceIdExchangeFilterFunction instance1 = TraceIdExchangeFilterFunction.create();
            TraceIdExchangeFilterFunction instance2 = TraceIdExchangeFilterFunction.create();

            assertThat(instance1).isNotSameAs(instance2);
        }
    }
}
