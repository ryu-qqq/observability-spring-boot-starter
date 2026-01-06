package com.ryuqq.observability.web.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CachedBodyRequestWrapper 테스트")
class CachedBodyRequestWrapperTest {

    @Nested
    @DisplayName("생성 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("요청 본문을 캐싱한다")
        void shouldCacheRequestBody() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("test body content".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyAsString()).isEqualTo("test body content");
        }

        @Test
        @DisplayName("빈 본문도 처리한다")
        void shouldHandleEmptyBody() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent(new byte[0]);

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyAsString()).isEmpty();
        }

        @Test
        @DisplayName("요청의 문자 인코딩을 사용한다")
        void shouldUseRequestCharacterEncoding() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCharacterEncoding("UTF-8");
            request.setContent("한글 테스트".getBytes("UTF-8"));

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyAsString()).isEqualTo("한글 테스트");
        }

        @Test
        @DisplayName("잘못된 인코딩은 UTF-8로 대체된다")
        void shouldFallbackToUtf8ForInvalidEncoding() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCharacterEncoding("INVALID-ENCODING");
            request.setContent("test".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyAsString()).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("getInputStream 테스트")
    class GetInputStreamTest {

        @Test
        @DisplayName("여러 번 읽을 수 있다")
        void shouldAllowMultipleReads() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("test body".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            // 첫 번째 읽기
            byte[] buffer1 = new byte[9];
            wrapper.getInputStream().read(buffer1);

            // 두 번째 읽기 (새 InputStream)
            byte[] buffer2 = new byte[9];
            wrapper.getInputStream().read(buffer2);

            assertThat(new String(buffer1)).isEqualTo("test body");
            assertThat(new String(buffer2)).isEqualTo("test body");
        }

        @Test
        @DisplayName("isFinished가 올바르게 동작한다")
        void shouldReportIsFinished() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("ab".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
            var inputStream = wrapper.getInputStream();

            assertThat(inputStream.isFinished()).isFalse();
            inputStream.read();
            inputStream.read();
            assertThat(inputStream.isFinished()).isTrue();
        }

        @Test
        @DisplayName("isReady는 항상 true를 반환한다")
        void shouldAlwaysBeReady() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("test".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getInputStream().isReady()).isTrue();
        }

        @Test
        @DisplayName("setReadListener는 지원되지 않는다")
        void shouldNotSupportSetReadListener() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("test".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThatThrownBy(() -> wrapper.getInputStream().setReadListener(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("getReader 테스트")
    class GetReaderTest {

        @Test
        @DisplayName("BufferedReader를 반환한다")
        void shouldReturnBufferedReader() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("test line".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            BufferedReader reader = wrapper.getReader();
            assertThat(reader.readLine()).isEqualTo("test line");
        }

        @Test
        @DisplayName("한글을 올바르게 읽는다")
        void shouldReadKoreanCorrectly() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCharacterEncoding("UTF-8");
            request.setContent("안녕하세요".getBytes("UTF-8"));

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getReader().readLine()).isEqualTo("안녕하세요");
        }
    }

    @Nested
    @DisplayName("getBodyAsString 테스트")
    class GetBodyAsStringTest {

        @Test
        @DisplayName("전체 본문을 문자열로 반환한다")
        void shouldReturnFullBody() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("full body content".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyAsString()).isEqualTo("full body content");
        }

        @Test
        @DisplayName("최대 길이를 초과하면 잘라서 반환한다")
        void shouldTruncateBodyIfExceedsMaxLength() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("this is a long body content".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            String truncated = wrapper.getBodyAsString(10);
            assertThat(truncated).isEqualTo("this is a ... (truncated)");
        }

        @Test
        @DisplayName("최대 길이 이하면 그대로 반환한다")
        void shouldReturnFullBodyIfBelowMaxLength() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("short".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            String result = wrapper.getBodyAsString(100);
            assertThat(result).isEqualTo("short");
        }
    }

    @Nested
    @DisplayName("getBodyLength 테스트")
    class GetBodyLengthTest {

        @Test
        @DisplayName("본문 바이트 길이를 반환한다")
        void shouldReturnBodyLength() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent("12345".getBytes());

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyLength()).isEqualTo(5);
        }

        @Test
        @DisplayName("빈 본문은 0을 반환한다")
        void shouldReturnZeroForEmptyBody() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContent(new byte[0]);

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyLength()).isZero();
        }

        @Test
        @DisplayName("한글 본문은 바이트 길이를 반환한다")
        void shouldReturnByteLengthForKorean() throws IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            byte[] koreanBytes = "한글".getBytes("UTF-8");
            request.setContent(koreanBytes);

            CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

            assertThat(wrapper.getBodyLength()).isEqualTo(koreanBytes.length);
        }
    }
}
