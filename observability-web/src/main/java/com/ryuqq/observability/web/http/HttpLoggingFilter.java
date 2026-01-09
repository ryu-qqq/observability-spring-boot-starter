package com.ryuqq.observability.web.http;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.web.config.HttpLoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HTTP 요청/응답을 자동으로 로깅하는 필터.
 *
 * <p>TraceIdFilter 다음에 실행되어야 합니다 (ORDER = -50).</p>
 *
 * <p>로깅 내용:</p>
 * <ul>
 *   <li>요청: Method, URI, Headers, Body (선택)</li>
 *   <li>응답: Status, Duration, Body (선택)</li>
 * </ul>
 */
public class HttpLoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger("observability.http");

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

    private final HttpLoggingProperties properties;
    private final PathNormalizer pathNormalizer;
    private final LogMasker logMasker;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Set<String> excludeHeadersLower;

    public HttpLoggingFilter(HttpLoggingProperties properties,
                             PathNormalizer pathNormalizer,
                             LogMasker logMasker) {
        this.properties = properties;
        this.pathNormalizer = pathNormalizer;
        this.logMasker = logMasker;
        this.excludeHeadersLower = properties.getExcludeHeaders().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Body 로깅이 필요한 경우에만 Wrapper 사용
        HttpServletRequest requestToUse = request;
        HttpServletResponse responseToUse = response;

        if (properties.isLogRequestBody() && isReadableContentType(request.getContentType())) {
            requestToUse = new CachedBodyRequestWrapper(request);
        }

        if (properties.isLogResponseBody()) {
            responseToUse = new CachedBodyResponseWrapper(response);
        }

        try {
            // 요청 로깅
            logRequest(requestToUse);

            // 다음 필터 실행
            filterChain.doFilter(requestToUse, responseToUse);

        } finally {
            // 응답 로깅
            long duration = System.currentTimeMillis() - startTime;
            logResponse(requestToUse, responseToUse, duration);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.getExcludePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String normalizedUri = pathNormalizer.normalize(uri);
        String clientIp = getClientIp(request);

        // 기본 정보 (메시지)
        StringBuilder message = new StringBuilder();
        message.append("HTTP Request: ").append(method).append(" ").append(uri);
        if (queryString != null) {
            message.append("?").append(queryString);
        }

        // 구조화 로깅을 위한 컨텍스트 추가 (MDC)
        TraceIdHolder.addContext("http.method", method);
        TraceIdHolder.addContext("http.uri", uri);
        TraceIdHolder.addContext("http.normalizedUri", normalizedUri);
        TraceIdHolder.addContext("http.clientIp", clientIp);

        // 구조화된 필드 (JSON 로그에서 별도 필드로 출력)
        Marker httpMarker = createRequestMarker(method, uri, normalizedUri, queryString, clientIp);
        log.info(httpMarker, "{}", message);

        // 헤더 로깅 (DEBUG 레벨)
        if (log.isDebugEnabled()) {
            Map<String, String> headers = getFilteredHeaders(request);
            log.debug("Request Headers: {}", headers);
        }

        // Body 로깅 (DEBUG 레벨)
        if (properties.isLogRequestBody() && request instanceof CachedBodyRequestWrapper wrapper) {
            String body = wrapper.getBodyAsString(properties.getMaxBodyLength());
            if (!body.isEmpty()) {
                String maskedBody = logMasker.mask(body);
                log.debug("Request Body: {}", maskedBody);
            }
        }
    }

    private Marker createRequestMarker(String method, String uri, String normalizedUri,
                                        String queryString, String clientIp) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("http_method", method);
        fields.put("http_path", uri);
        fields.put("http_path_normalized", normalizedUri);
        if (queryString != null) {
            fields.put("http_query", queryString);
        }
        fields.put("http_client_ip", clientIp);
        fields.put("http_direction", "inbound");
        return Markers.appendEntries(fields);
    }

    private void logResponse(HttpServletRequest request,
                             HttpServletResponse response,
                             long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String normalizedUri = pathNormalizer.normalize(uri);
        int status = response.getStatus();

        // 느린 요청 여부 판단
        boolean isSlow = duration >= properties.getSlowRequestThresholdMs();

        // 로그 레벨 결정
        String logLevel = determineLogLevel(status, isSlow);

        // 메시지 구성
        String message = String.format("HTTP Response: %s %s | status=%d | duration=%dms%s",
                method, uri, status, duration, isSlow ? " [SLOW]" : "");

        // 컨텍스트 추가 (MDC)
        TraceIdHolder.addContext("http.status", String.valueOf(status));
        TraceIdHolder.addContext("http.duration", String.valueOf(duration));

        // 구조화된 필드 (JSON 로그에서 별도 필드로 출력)
        Marker httpMarker = createResponseMarker(method, uri, normalizedUri, status, duration, isSlow);

        // 로그 레벨에 따라 출력
        switch (logLevel) {
            case "ERROR" -> log.error(httpMarker, "{}", message);
            case "WARN" -> log.warn(httpMarker, "{}", message);
            default -> log.info(httpMarker, "{}", message);
        }

        // Body 로깅 (DEBUG 레벨, 에러 시 INFO)
        if (properties.isLogResponseBody() && response instanceof CachedBodyResponseWrapper wrapper) {
            String body = wrapper.getBodyAsString(properties.getMaxBodyLength());
            if (!body.isEmpty()) {
                String maskedBody = logMasker.mask(body);
                if (status >= 400) {
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

    private String determineLogLevel(int status, boolean isSlow) {
        if (status >= 500) {
            return "ERROR";
        } else if (status >= 400 || isSlow) {
            return "WARN";
        }
        return "INFO";
    }

    private Map<String, String> getFilteredHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!excludeHeadersLower.contains(name.toLowerCase())) {
                headers.put(name, request.getHeader(name));
            } else {
                headers.put(name, "[FILTERED]");
            }
        }
        return headers;
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For는 여러 IP가 콤마로 구분될 수 있음
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isReadableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return lower.contains("json") ||
               lower.contains("xml") ||
               lower.contains("text") ||
               lower.contains("form-urlencoded");
    }
}
