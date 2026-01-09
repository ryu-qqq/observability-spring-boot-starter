package com.ryuqq.observability.webflux.http;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.webflux.config.ReactiveHttpLoggingProperties;
import net.logstash.logback.marker.Markers;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * WebFlux HTTP 요청/응답을 자동으로 로깅하는 WebFilter.
 *
 * <p>ReactiveTraceIdFilter 다음에 실행되어야 합니다 (ORDER = -50).</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>요청 로깅: Method, URI, Headers, Body (선택)</li>
 *   <li>응답 로깅: Status, Duration, Headers, Body (선택)</li>
 *   <li>느린 요청 감지 및 [SLOW] 태그</li>
 *   <li>민감정보 마스킹 (LogMasker 연동)</li>
 *   <li>경로 정규화 (메트릭 폭발 방지)</li>
 * </ul>
 *
 * <p>WebFlux/Netty 환경에서 DataBuffer 기반으로 구현되어 있으며,
 * 본문 로깅 시 메모리 사용량에 주의해야 합니다.</p>
 */
public class ReactiveHttpLoggingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger("observability.http");
    private static final Logger internalLog = LoggerFactory.getLogger(ReactiveHttpLoggingFilter.class);

    /**
     * ReactiveTraceIdFilter(-100) 다음에 실행되어야 함.
     */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

    private final ReactiveHttpLoggingProperties properties;
    private final ReactivePathNormalizer pathNormalizer;
    private final LogMasker logMasker;
    private final AntPathMatcher pathMatcher;
    private final Set<String> excludeHeadersLower;
    private final Set<String> loggableContentTypesLower;
    private final DataBufferFactory bufferFactory;

    /**
     * ReactiveHttpLoggingFilter를 생성합니다.
     *
     * @param properties     HTTP 로깅 설정
     * @param pathNormalizer 경로 정규화기
     * @param logMasker      민감정보 마스킹 유틸리티
     */
    public ReactiveHttpLoggingFilter(ReactiveHttpLoggingProperties properties,
                                     ReactivePathNormalizer pathNormalizer,
                                     LogMasker logMasker) {
        this.properties = properties;
        this.pathNormalizer = pathNormalizer;
        this.logMasker = logMasker;
        this.pathMatcher = new AntPathMatcher();
        this.bufferFactory = new DefaultDataBufferFactory();

        // 제외 헤더를 소문자로 변환하여 Set에 저장 (대소문자 무관 비교)
        this.excludeHeadersLower = properties.getExcludeHeaders().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // 로깅 가능한 Content-Type을 소문자로 변환
        this.loggableContentTypesLower = properties.getLoggableContentTypes().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        internalLog.debug("ReactiveHttpLoggingFilter initialized with settings: " +
                        "logRequestBody={}, logResponseBody={}, maxBodyLength={}, excludePaths={}",
                properties.isLogRequestBody(), properties.isLogResponseBody(),
                properties.getMaxBodyLength(), properties.getExcludePaths());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 비활성화된 경우 바로 통과
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        // 제외 경로인 경우 바로 통과
        String path = exchange.getRequest().getURI().getPath();
        if (shouldExclude(path)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();

        // 요청/응답 데코레이터 생성
        ServerWebExchange decoratedExchange = decorateExchange(exchange, startTime);

        // 요청 로깅 (비동기)
        return logRequest(decoratedExchange.getRequest())
                .then(chain.filter(decoratedExchange))
                .doOnSuccess(aVoid -> logResponse(decoratedExchange, startTime))
                .doOnError(error -> logError(decoratedExchange, startTime, error));
    }

    /**
     * 제외 경로 여부를 확인합니다.
     */
    private boolean shouldExclude(String path) {
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 본문 로깅이 필요한 경우 요청/응답을 데코레이팅합니다.
     */
    private ServerWebExchange decorateExchange(ServerWebExchange exchange, long startTime) {
        ServerHttpRequest decoratedRequest = exchange.getRequest();
        ServerHttpResponse decoratedResponse = exchange.getResponse();

        // 요청 본문 로깅 필요 시 데코레이터 적용
        if (properties.isLogRequestBody() && isLoggableContentType(exchange.getRequest().getHeaders().getContentType())) {
            decoratedRequest = new LoggingRequestDecorator(exchange.getRequest());
        }

        // 응답 본문 로깅 필요 시 데코레이터 적용
        if (properties.isLogResponseBody()) {
            decoratedResponse = new LoggingResponseDecorator(exchange.getResponse(), startTime);
        }

        return exchange.mutate()
                .request(decoratedRequest)
                .response(decoratedResponse)
                .build();
    }

    /**
     * 요청 정보를 로깅합니다.
     */
    private Mono<Void> logRequest(ServerHttpRequest request) {
        return Mono.fromRunnable(() -> {
            String method = request.getMethod().name();
            String uri = request.getURI().getPath();
            String query = request.getURI().getQuery();
            String normalizedUri = pathNormalizer.normalize(uri);
            String clientIp = getClientIp(request);

            // 기본 요청 정보 (메시지)
            StringBuilder message = new StringBuilder();
            message.append("HTTP Request: ").append(method).append(" ").append(uri);
            if (query != null && !query.isEmpty()) {
                message.append("?").append(query);
            }

            // 구조화된 필드 (JSON 로그에서 별도 필드로 출력)
            Marker httpMarker = createRequestMarker(method, uri, normalizedUri, query, clientIp);
            log.info(httpMarker, "{}", message);

            // 헤더 로깅 (DEBUG 레벨)
            if (log.isDebugEnabled()) {
                Map<String, String> filteredHeaders = getFilteredHeaders(request.getHeaders());
                log.debug("Request Headers: {}", filteredHeaders);
            }

            // 클라이언트 IP 정보 (DEBUG 레벨)
            if (log.isDebugEnabled()) {
                log.debug("Client IP: {}, Normalized URI: {}", clientIp, normalizedUri);
            }
        });
    }

    private Marker createRequestMarker(String method, String uri, String normalizedUri,
                                        String query, String clientIp) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("http_method", method);
        fields.put("http_path", uri);
        fields.put("http_path_normalized", normalizedUri);
        if (query != null && !query.isEmpty()) {
            fields.put("http_query", query);
        }
        fields.put("http_client_ip", clientIp);
        fields.put("http_direction", "inbound");
        return Markers.appendEntries(fields);
    }

    /**
     * 응답 정보를 로깅합니다.
     */
    private void logResponse(ServerWebExchange exchange, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        String normalizedUri = pathNormalizer.normalize(uri);
        Integer statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

        // 느린 요청 여부 판단
        boolean isSlow = duration >= properties.getSlowRequestThresholdMs();

        // 메시지 구성
        String message = String.format("HTTP Response: %s %s | status=%d | duration=%dms%s",
                method, uri, statusCode, duration, isSlow ? " [SLOW]" : "");

        // 구조화된 필드 (JSON 로그에서 별도 필드로 출력)
        Marker httpMarker = createResponseMarker(method, uri, normalizedUri, statusCode, duration, isSlow);

        // 상태 코드에 따른 로그 레벨 결정
        if (statusCode >= 500) {
            log.error(httpMarker, "{}", message);
        } else if (statusCode >= 400 || isSlow) {
            log.warn(httpMarker, "{}", message);
        } else {
            log.info(httpMarker, "{}", message);
        }

        // 응답 헤더 로깅 (DEBUG 레벨) - LoggingResponseDecorator에서 캐싱된 헤더 사용
        if (log.isDebugEnabled()) {
            if (response instanceof LoggingResponseDecorator decorator) {
                Map<String, String> cachedHeaders = decorator.getCachedHeaders();
                if (cachedHeaders != null) {
                    log.debug("Response Headers: {}", cachedHeaders);
                }
            } else {
                // 데코레이터가 없는 경우 안전하게 시도 (예외 발생 가능)
                try {
                    Map<String, String> filteredHeaders = getFilteredHeaders(response.getHeaders());
                    log.debug("Response Headers: {}", filteredHeaders);
                } catch (Exception e) {
                    internalLog.debug("Cannot log response headers after commit: {}", e.getMessage());
                }
            }
        }

        // 응답 본문 로깅 (LoggingResponseDecorator에서 처리)
        if (response instanceof LoggingResponseDecorator decorator) {
            String body = decorator.getCachedBody();
            if (body != null && !body.isEmpty()) {
                String maskedBody = logMasker.mask(truncateBody(body));
                if (statusCode >= 400) {
                    log.info("Response Body: {}", maskedBody);
                } else if (log.isDebugEnabled()) {
                    log.debug("Response Body: {}", maskedBody);
                }
            }
        }
    }

    private Marker createResponseMarker(String method, String uri, String normalizedUri,
                                         int status, long duration, boolean isSlow) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("http_method", method);
        fields.put("http_path", uri);
        fields.put("http_path_normalized", normalizedUri);
        fields.put("http_status", status);
        fields.put("http_duration_ms", duration);
        fields.put("http_direction", "inbound");
        if (isSlow) {
            fields.put("http_slow", true);
        }
        return Markers.appendEntries(fields);
    }

    /**
     * 에러 발생 시 로깅합니다.
     */
    private void logError(ServerWebExchange exchange, long startTime, Throwable error) {
        long duration = System.currentTimeMillis() - startTime;
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        String normalizedUri = pathNormalizer.normalize(uri);

        // 구조화된 필드 (JSON 로그에서 별도 필드로 출력)
        Marker httpMarker = createErrorMarker(method, uri, normalizedUri, duration, error);

        log.error(httpMarker, "HTTP Error: {} {} | duration={}ms | error={}",
                method, uri, duration, error.getMessage());
    }

    private Marker createErrorMarker(String method, String uri, String normalizedUri,
                                      long duration, Throwable error) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("http_method", method);
        fields.put("http_path", uri);
        fields.put("http_path_normalized", normalizedUri);
        fields.put("http_status", 500);
        fields.put("http_duration_ms", duration);
        fields.put("http_direction", "inbound");
        fields.put("http_error", true);
        fields.put("http_error_type", error.getClass().getSimpleName());
        fields.put("http_error_message", error.getMessage());
        return Markers.appendEntries(fields);
    }

    /**
     * 필터링된 헤더를 반환합니다.
     */
    private Map<String, String> getFilteredHeaders(HttpHeaders headers) {
        Map<String, String> filteredHeaders = new HashMap<>();
        headers.forEach((name, values) -> {
            if (excludeHeadersLower.contains(name.toLowerCase())) {
                filteredHeaders.put(name, "[FILTERED]");
            } else {
                filteredHeaders.put(name, String.join(", ", values));
            }
        });
        return filteredHeaders;
    }

    /**
     * 클라이언트 IP를 추출합니다.
     */
    private String getClientIp(ServerHttpRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        HttpHeaders headers = request.getHeaders();
        for (String headerName : headerNames) {
            String value = headers.getFirst(headerName);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For는 여러 IP가 콤마로 구분될 수 있음
                return value.split(",")[0].trim();
            }
        }

        // 기본값: 직접 연결된 주소
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    /**
     * Content-Type이 로깅 가능한지 확인합니다.
     */
    private boolean isLoggableContentType(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        String typeSubtype = contentType.getType() + "/" + contentType.getSubtype();
        return loggableContentTypesLower.contains(typeSubtype.toLowerCase()) ||
               contentType.includes(MediaType.APPLICATION_JSON) ||
               contentType.includes(MediaType.APPLICATION_XML) ||
               contentType.includes(MediaType.TEXT_PLAIN);
    }

    /**
     * 본문을 최대 길이로 자릅니다.
     */
    private String truncateBody(String body) {
        int maxLength = properties.getMaxBodyLength();
        if (body.length() > maxLength) {
            return body.substring(0, maxLength) + "... [TRUNCATED, total=" + body.length() + "]";
        }
        return body;
    }


    /**
     * 요청 본문을 캐싱하는 데코레이터.
     */
    private class LoggingRequestDecorator extends ServerHttpRequestDecorator {

        private final StringBuilder cachedBody = new StringBuilder();

        LoggingRequestDecorator(ServerHttpRequest delegate) {
            super(delegate);
        }

        @Override
        public Flux<DataBuffer> getBody() {
            return super.getBody()
                    .doOnNext(buffer -> {
                        // 본문을 캐싱 (최대 길이까지만)
                        if (cachedBody.length() < properties.getMaxBodyLength()) {
                            byte[] content = new byte[buffer.readableByteCount()];
                            buffer.read(content);
                            buffer.readPosition(0); // 읽기 위치 복구
                            String chunk = new String(content, getCharset());
                            int remaining = properties.getMaxBodyLength() - cachedBody.length();
                            if (chunk.length() > remaining) {
                                cachedBody.append(chunk, 0, remaining);
                            } else {
                                cachedBody.append(chunk);
                            }
                        }
                    })
                    .doOnComplete(() -> {
                        if (cachedBody.length() > 0) {
                            String maskedBody = logMasker.mask(truncateBody(cachedBody.toString()));
                            if (log.isDebugEnabled()) {
                                log.debug("Request Body: {}", maskedBody);
                            }
                        }
                    });
        }

        private Charset getCharset() {
            MediaType contentType = getDelegate().getHeaders().getContentType();
            if (contentType != null && contentType.getCharset() != null) {
                return contentType.getCharset();
            }
            return StandardCharsets.UTF_8;
        }
    }


    /**
     * 응답 본문을 캐싱하는 데코레이터.
     *
     * <p>버퍼를 직접 수정하지 않고 안전하게 복사하여 캐싱합니다.
     * 이는 ReadOnlyHttpHeaders 예외를 방지합니다.</p>
     */
    private class LoggingResponseDecorator extends ServerHttpResponseDecorator {

        private final ByteArrayOutputStream cachedBodyStream = new ByteArrayOutputStream();
        private final long startTime;
        private volatile Charset cachedCharset = StandardCharsets.UTF_8;
        private volatile Map<String, String> cachedHeaders = null;

        LoggingResponseDecorator(ServerHttpResponse delegate, long startTime) {
            super(delegate);
            this.startTime = startTime;

            // beforeCommit 콜백에서 헤더 정보를 캐싱 (응답 커밋 전에 저장)
            delegate.beforeCommit(() -> {
                try {
                    HttpHeaders headers = delegate.getHeaders();
                    MediaType contentType = headers.getContentType();
                    if (contentType != null && contentType.getCharset() != null) {
                        cachedCharset = contentType.getCharset();
                    }
                    // 헤더를 미리 캐싱 (응답 커밋 후에는 ReadOnlyHttpHeaders가 됨)
                    cachedHeaders = getFilteredHeaders(headers);
                } catch (Exception e) {
                    internalLog.debug("Failed to cache response headers: {}", e.getMessage());
                }
                return Mono.empty();
            });
        }

        /**
         * 캐싱된 응답 헤더를 반환합니다.
         */
        Map<String, String> getCachedHeaders() {
            return cachedHeaders;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            // DataBufferUtils.join()을 사용하여 전체 본문을 안전하게 버퍼링
            // 이렇게 하면 Spring의 응답 파이프라인 타이밍을 방해하지 않음
            return DataBufferUtils.join(Flux.from(body))
                    .flatMap(dataBuffer -> {
                        try {
                            // 버퍼 캐싱 (로깅용)
                            cacheBuffer(dataBuffer);

                            // 새 버퍼 생성 - 원본 버퍼의 내용을 복사
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);

                            // 원본 버퍼 해제
                            DataBufferUtils.release(dataBuffer);

                            // 새 버퍼로 응답 작성
                            DataBuffer newBuffer = bufferFactory.wrap(content);
                            return getDelegate().writeWith(Mono.just(newBuffer));
                        } catch (Exception e) {
                            DataBufferUtils.release(dataBuffer);
                            return Mono.error(e);
                        }
                    })
                    .switchIfEmpty(getDelegate().writeWith(Mono.empty()));
        }

        /**
         * 버퍼 내용을 안전하게 캐싱합니다.
         */
        private void cacheBuffer(DataBuffer buffer) {
            try {
                if (cachedBodyStream.size() >= properties.getMaxBodyLength()) {
                    return;
                }

                java.nio.ByteBuffer byteBuffer = buffer.toByteBuffer();
                int readable = byteBuffer.remaining();
                int remaining = properties.getMaxBodyLength() - cachedBodyStream.size();
                int toRead = Math.min(readable, remaining);

                if (toRead > 0) {
                    byte[] content = new byte[toRead];
                    byteBuffer.get(content, 0, toRead);
                    cachedBodyStream.write(content, 0, toRead);
                }
            } catch (Exception e) {
                internalLog.debug("Failed to cache response body: {}", e.getMessage());
            }
        }

        @Override
        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            // 스트리밍 응답의 경우, 캐싱 없이 원본 그대로 전달
            return getDelegate().writeAndFlushWith(body);
        }

        /**
         * 캐싱된 응답 본문을 반환합니다.
         */
        String getCachedBody() {
            if (cachedBodyStream.size() == 0) {
                return null;
            }
            return cachedBodyStream.toString(cachedCharset);
        }
    }
}
