package com.ryuqq.observability.client.feign;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceIdFeignRequestInterceptor 테스트")
class TraceIdFeignRequestInterceptorTest {

    private TraceIdFeignRequestInterceptor interceptor;
    private RequestTemplate template;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdFeignRequestInterceptor();
        template = new RequestTemplate();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("apply 테스트")
    class ApplyTest {

        @Test
        @DisplayName("TraceId를 헤더에 추가한다")
        void shouldAddTraceIdToHeaders() {
            TraceIdHolder.set("test-trace-id");

            interceptor.apply(template);

            Collection<String> values = template.headers().get(TraceIdHeaders.X_TRACE_ID);
            assertThat(values).contains("test-trace-id");
        }

        @Test
        @DisplayName("UserId를 헤더에 추가한다")
        void shouldAddUserIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-123");

            interceptor.apply(template);

            Collection<String> values = template.headers().get(TraceIdHeaders.X_USER_ID);
            assertThat(values).contains("user-123");
        }

        @Test
        @DisplayName("TenantId를 헤더에 추가한다")
        void shouldAddTenantIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setTenantId("tenant-456");

            interceptor.apply(template);

            Collection<String> values = template.headers().get(TraceIdHeaders.X_TENANT_ID);
            assertThat(values).contains("tenant-456");
        }

        @Test
        @DisplayName("OrganizationId를 헤더에 추가한다")
        void shouldAddOrganizationIdToHeaders() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setOrganizationId("org-789");

            interceptor.apply(template);

            Collection<String> values = template.headers().get(TraceIdHeaders.X_ORGANIZATION_ID);
            assertThat(values).contains("org-789");
        }

        @Test
        @DisplayName("모든 컨텍스트를 전파한다")
        void shouldPropagateAllContext() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-id");
            TraceIdHolder.setTenantId("tenant-id");
            TraceIdHolder.setOrganizationId("org-id");

            interceptor.apply(template);

            assertThat(template.headers().get(TraceIdHeaders.X_TRACE_ID)).contains("trace-id");
            assertThat(template.headers().get(TraceIdHeaders.X_USER_ID)).contains("user-id");
            assertThat(template.headers().get(TraceIdHeaders.X_TENANT_ID)).contains("tenant-id");
            assertThat(template.headers().get(TraceIdHeaders.X_ORGANIZATION_ID)).contains("org-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 헤더를 추가하지 않는다")
        void shouldNotAddHeaderWhenNoTraceId() {
            interceptor.apply(template);

            assertThat(template.headers().get(TraceIdHeaders.X_TRACE_ID)).isNull();
        }

        @Test
        @DisplayName("null 값은 헤더에 추가하지 않는다")
        void shouldNotAddNullValues() {
            TraceIdHolder.set("trace-id");

            interceptor.apply(template);

            assertThat(template.headers().get(TraceIdHeaders.X_USER_ID)).isNull();
            assertThat(template.headers().get(TraceIdHeaders.X_TENANT_ID)).isNull();
            assertThat(template.headers().get(TraceIdHeaders.X_ORGANIZATION_ID)).isNull();
        }
    }
}
