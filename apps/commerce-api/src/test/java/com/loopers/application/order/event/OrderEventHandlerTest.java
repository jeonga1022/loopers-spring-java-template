package com.loopers.application.order.event;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentType;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.event.PaymentFailedEvent;
import com.loopers.domain.order.event.PaymentSucceededEvent;
import com.loopers.fixture.OrderFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock
    private OrderDomainService orderDomainService;

    @Mock
    private OrderFacade orderFacade;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderEventHandler orderEventHandler;

    @Test
    @DisplayName("결제 성공 이벤트를 받으면 주문을 확정하고 OrderCompletedEvent를 발행한다")
    void handlePaymentSucceededTest1() {
        // arrange
        String userId = "user-1";
        Long orderId = 1L;
        String pgTransactionId = "pg-tx-123";

        Payment payment = Payment.create(orderId, userId, 10000L, PaymentType.CARD_ONLY);
        PaymentSucceededEvent event = PaymentSucceededEvent.from(payment, pgTransactionId);

        Order order = OrderFixture.create(userId, 10000L);
        when(orderDomainService.confirmOrder(userId, orderId)).thenReturn(order);

        // act
        orderEventHandler.handlePaymentSucceeded(event);

        // assert
        verify(orderDomainService, times(1)).confirmOrder(userId, orderId);

        ArgumentCaptor<OrderCompletedEvent> captor = ArgumentCaptor.forClass(OrderCompletedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        OrderCompletedEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("결제 실패 이벤트를 받으면 OrderFacade의 handlePaymentFailure를 호출한다")
    void handlePaymentFailedTest1() {
        // arrange
        String userId = "user-1";
        Long orderId = 1L;
        Long paymentId = 100L;
        String reason = "잔액 부족";

        Payment payment = Payment.create(orderId, userId, 10000L, PaymentType.CARD_ONLY);
        PaymentFailedEvent event = PaymentFailedEvent.from(payment, reason);

        // act
        orderEventHandler.handlePaymentFailed(event);

        // assert
        verify(orderFacade, times(1)).handlePaymentFailure(userId, orderId);
    }

    @Test
    @DisplayName("결제 성공 이벤트의 orderId와 userId가 올바르게 전달된다")
    void handlePaymentSucceededTest2() {
        // arrange
        String userId = "specific-user";
        Long orderId = 999L;
        String pgTransactionId = "pg-tx-456";

        Payment payment = Payment.create(orderId, userId, 50000L, PaymentType.CARD_ONLY);
        PaymentSucceededEvent event = PaymentSucceededEvent.from(payment, pgTransactionId);

        Order order = OrderFixture.create(userId, 50000L);
        when(orderDomainService.confirmOrder(userId, orderId)).thenReturn(order);

        // act
        orderEventHandler.handlePaymentSucceeded(event);

        // assert
        verify(orderDomainService).confirmOrder(userId, orderId);
    }
}
