package com.ryuqq.observability.web.http;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Response Body를 캐싱하여 로깅할 수 있게 해주는 Wrapper.
 */
public class CachedBodyResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream cachedContent = new ByteArrayOutputStream();
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public CachedBodyResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CachedServletOutputStream(getResponse().getOutputStream());
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        if (outputStream != null) {
            outputStream.flush();
        }
        super.flushBuffer();
    }

    /**
     * 캐시된 Body를 문자열로 반환합니다.
     *
     * @return Body 문자열
     */
    public String getBodyAsString() {
        return cachedContent.toString(StandardCharsets.UTF_8);
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
        return cachedContent.size();
    }


    private class CachedServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream originalStream;

        public CachedServletOutputStream(ServletOutputStream originalStream) {
            this.originalStream = originalStream;
        }

        @Override
        public void write(int b) throws IOException {
            cachedContent.write(b);
            originalStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            cachedContent.write(b, off, len);
            originalStream.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return originalStream.isReady();
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            originalStream.setWriteListener(listener);
        }

        @Override
        public void flush() throws IOException {
            originalStream.flush();
        }
    }
}
