package com.ryuqq.observability.integration.gateway;

import com.ryuqq.observability.webflux.context.MdcContextLifter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.util.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * MdcContextLifter 단위 테스트.
 *
 * <p>검증 항목:</p>
 * <ul>
 *   <li>Scannable 인터페이스 구현 검증</li>
 *   <li>scanUnsafe 메서드 동작 검증</li>
 *   <li>Subscription 참조 저장 검증</li>
 * </ul>
 */
class MdcContextLifterTest {

    @Test
    @DisplayName("MdcContextLifter는 Scannable 인터페이스를 구현해야 한다")
    void shouldImplementScannable() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();

        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        assertThat(lifter).isInstanceOf(Scannable.class);
    }

    @Test
    @DisplayName("scanUnsafe(ACTUAL)은 delegate를 반환해야 한다")
    void shouldReturnDelegateForActualAttribute() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        Object actual = lifter.scanUnsafe(Scannable.Attr.ACTUAL);

        assertThat(actual).isSameAs(mockDelegate);
    }

    @Test
    @DisplayName("scanUnsafe(PARENT)은 onSubscribe 후 Subscription을 반환해야 한다")
    void shouldReturnSubscriptionForParentAttribute() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        Subscription mockSubscription = mock(Subscription.class);
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        // onSubscribe 호출 전에는 null
        Object parentBefore = lifter.scanUnsafe(Scannable.Attr.PARENT);
        assertThat(parentBefore).isNull();

        // onSubscribe 호출
        lifter.onSubscribe(mockSubscription);

        // onSubscribe 호출 후에는 Subscription 반환
        Object parentAfter = lifter.scanUnsafe(Scannable.Attr.PARENT);
        assertThat(parentAfter).isSameAs(mockSubscription);
    }

    @Test
    @DisplayName("scanUnsafe(RUN_STYLE)은 SYNC를 반환해야 한다")
    void shouldReturnSyncForRunStyleAttribute() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        Object runStyle = lifter.scanUnsafe(Scannable.Attr.RUN_STYLE);

        assertThat(runStyle).isEqualTo(Scannable.Attr.RunStyle.SYNC);
    }

    @Test
    @DisplayName("scanUnsafe(PREFETCH)은 Integer.MAX_VALUE를 반환해야 한다")
    void shouldReturnMaxValueForPrefetchAttribute() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        Object prefetch = lifter.scanUnsafe(Scannable.Attr.PREFETCH);

        assertThat(prefetch).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("onSubscribe는 delegate에 전파되어야 한다")
    void shouldPropagateOnSubscribeToDelegate() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        Subscription mockSubscription = mock(Subscription.class);
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        lifter.onSubscribe(mockSubscription);

        verify(mockDelegate).onSubscribe(mockSubscription);
    }

    @Test
    @DisplayName("onNext는 delegate에 전파되어야 한다")
    void shouldPropagateOnNextToDelegate() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        lifter.onNext("test-value");

        verify(mockDelegate).onNext("test-value");
    }

    @Test
    @DisplayName("onError는 delegate에 전파되어야 한다")
    void shouldPropagateOnErrorToDelegate() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);
        RuntimeException testException = new RuntimeException("test error");

        lifter.onError(testException);

        verify(mockDelegate).onError(testException);
    }

    @Test
    @DisplayName("onComplete는 delegate에 전파되어야 한다")
    void shouldPropagateOnCompleteToDelegate() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        lifter.onComplete();

        verify(mockDelegate).onComplete();
    }

    @Test
    @DisplayName("currentContext는 delegate의 context를 반환해야 한다")
    void shouldReturnDelegateContext() {
        CoreSubscriber<String> mockDelegate = createMockSubscriber();
        Context expectedContext = Context.of("key", "value");
        when(mockDelegate.currentContext()).thenReturn(expectedContext);

        MdcContextLifter<String> lifter = new MdcContextLifter<>(mockDelegate);

        Context actualContext = lifter.currentContext();

        assertThat(actualContext).isSameAs(expectedContext);
    }

    @SuppressWarnings("unchecked")
    private CoreSubscriber<String> createMockSubscriber() {
        CoreSubscriber<String> mock = mock(CoreSubscriber.class);
        when(mock.currentContext()).thenReturn(Context.empty());
        return mock;
    }
}
