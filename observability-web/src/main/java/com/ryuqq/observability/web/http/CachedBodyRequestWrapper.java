package com.ryuqq.observability.web.http;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Request Body를 캐싱하여 여러 번 읽을 수 있게 해주는 Wrapper.
 *
 * <p>HTTP Request Body는 InputStream으로 한 번만 읽을 수 있는데,
 * 로깅과 실제 처리 모두에서 Body가 필요한 경우 이 Wrapper를 사용합니다.</p>
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final Charset charset;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        this.charset = determineCharset(request);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    /**
     * 캐시된 Body를 문자열로 반환합니다.
     *
     * @return Body 문자열
     */
    public String getBodyAsString() {
        return new String(cachedBody, charset);
    }

    /**
     * 캐시된 Body를 최대 길이만큼 잘라서 반환합니다.
     *
     * @param maxLength 최대 길이
     * @return 잘린 Body 문자열
     */
    public String getBodyAsString(int maxLength) {
        String body = getBodyAsString();
        if (body.length() <= maxLength) {
            return body;
        }
        return body.substring(0, maxLength) + "... (truncated)";
    }

    /**
     * Body 길이를 반환합니다.
     *
     * @return Body 바이트 길이
     */
    public int getBodyLength() {
        return cachedBody.length;
    }

    private Charset determineCharset(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        if (encoding != null) {
            try {
                return Charset.forName(encoding);
            } catch (Exception ignored) {
            }
        }
        return StandardCharsets.UTF_8;
    }


    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("setReadListener is not supported");
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
