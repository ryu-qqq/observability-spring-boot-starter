package com.ryuqq.observability.web.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CachedBodyResponseWrapper 테스트")
class CachedBodyResponseWrapperTest {

    private MockHttpServletResponse mockResponse;
    private CachedBodyResponseWrapper wrapper;

    @BeforeEach
    void setUp() {
        mockResponse = new MockHttpServletResponse();
        wrapper = new CachedBodyResponseWrapper(mockResponse);
    }

    @Nested
    @DisplayName("getOutputStream 테스트")
    class GetOutputStreamTest {

        @Test
        @DisplayName("출력 내용을 캐싱한다")
        void shouldCacheOutputContent() throws IOException {
            wrapper.getOutputStream().write("test output".getBytes());

            assertThat(wrapper.getBodyAsString()).isEqualTo("test output");
        }

        @Test
        @DisplayName("원본 응답에도 쓴다")
        void shouldWriteToOriginalResponse() throws IOException {
            wrapper.getOutputStream().write("test output".getBytes());
            wrapper.flushBuffer();

            assertThat(mockResponse.getContentAsString()).isEqualTo("test output");
        }

        @Test
        @DisplayName("동일한 OutputStream 인스턴스를 반환한다")
        void shouldReturnSameOutputStream() throws IOException {
            var stream1 = wrapper.getOutputStream();
            var stream2 = wrapper.getOutputStream();

            assertThat(stream1).isSameAs(stream2);
        }

        @Test
        @DisplayName("write(byte[], off, len)이 동작한다")
        void shouldSupportWriteWithOffsetAndLength() throws IOException {
            byte[] data = "ABCDE".getBytes();
            wrapper.getOutputStream().write(data, 1, 3);

            assertThat(wrapper.getBodyAsString()).isEqualTo("BCD");
        }

        @Test
        @DisplayName("isReady가 동작한다")
        void shouldSupportIsReady() throws IOException {
            assertThat(wrapper.getOutputStream().isReady()).isTrue();
        }
    }

    @Nested
    @DisplayName("getWriter 테스트")
    class GetWriterTest {

        @Test
        @DisplayName("PrintWriter를 통해 쓸 수 있다")
        void shouldWriteViaPrintWriter() throws IOException {
            PrintWriter writer = wrapper.getWriter();
            writer.print("writer output");
            writer.flush();

            assertThat(wrapper.getBodyAsString()).isEqualTo("writer output");
        }

        @Test
        @DisplayName("동일한 Writer 인스턴스를 반환한다")
        void shouldReturnSameWriter() throws IOException {
            PrintWriter writer1 = wrapper.getWriter();
            PrintWriter writer2 = wrapper.getWriter();

            assertThat(writer1).isSameAs(writer2);
        }

        @Test
        @DisplayName("한글을 올바르게 쓴다")
        void shouldWriteKoreanCorrectly() throws IOException {
            mockResponse.setCharacterEncoding("UTF-8");
            wrapper = new CachedBodyResponseWrapper(mockResponse);

            PrintWriter writer = wrapper.getWriter();
            writer.print("안녕하세요");
            writer.flush();

            assertThat(wrapper.getBodyAsString()).isEqualTo("안녕하세요");
        }
    }

    @Nested
    @DisplayName("flushBuffer 테스트")
    class FlushBufferTest {

        @Test
        @DisplayName("Writer를 flush한다")
        void shouldFlushWriter() throws IOException {
            PrintWriter writer = wrapper.getWriter();
            writer.print("buffered content");
            wrapper.flushBuffer();

            assertThat(mockResponse.getContentAsString()).isEqualTo("buffered content");
        }

        @Test
        @DisplayName("OutputStream을 flush한다")
        void shouldFlushOutputStream() throws IOException {
            wrapper.getOutputStream().write("stream content".getBytes());
            wrapper.flushBuffer();

            assertThat(mockResponse.getContentAsString()).isEqualTo("stream content");
        }

        @Test
        @DisplayName("Writer나 OutputStream이 없어도 예외가 발생하지 않는다")
        void shouldNotThrowWhenNoWriterOrStream() throws IOException {
            wrapper.flushBuffer(); // should not throw
        }
    }

    @Nested
    @DisplayName("getBodyAsString 테스트")
    class GetBodyAsStringTest {

        @Test
        @DisplayName("캐시된 본문을 반환한다")
        void shouldReturnCachedBody() throws IOException {
            wrapper.getOutputStream().write("cached body".getBytes());

            assertThat(wrapper.getBodyAsString()).isEqualTo("cached body");
        }

        @Test
        @DisplayName("빈 응답은 빈 문자열을 반환한다")
        void shouldReturnEmptyForNoContent() {
            assertThat(wrapper.getBodyAsString()).isEmpty();
        }

        @Test
        @DisplayName("최대 길이를 초과하면 잘라서 반환한다")
        void shouldTruncateIfExceedsMaxLength() throws IOException {
            wrapper.getOutputStream().write("this is a very long response body".getBytes());

            String truncated = wrapper.getBodyAsString(15);
            assertThat(truncated).isEqualTo("this is a very ... (truncated)");
        }

        @Test
        @DisplayName("최대 길이 이하면 그대로 반환한다")
        void shouldReturnFullIfBelowMaxLength() throws IOException {
            wrapper.getOutputStream().write("short".getBytes());

            assertThat(wrapper.getBodyAsString(100)).isEqualTo("short");
        }
    }

    @Nested
    @DisplayName("getBodyLength 테스트")
    class GetBodyLengthTest {

        @Test
        @DisplayName("본문 길이를 반환한다")
        void shouldReturnBodyLength() throws IOException {
            wrapper.getOutputStream().write("12345".getBytes());

            assertThat(wrapper.getBodyLength()).isEqualTo(5);
        }

        @Test
        @DisplayName("빈 응답은 0을 반환한다")
        void shouldReturnZeroForEmptyResponse() {
            assertThat(wrapper.getBodyLength()).isZero();
        }
    }
}
