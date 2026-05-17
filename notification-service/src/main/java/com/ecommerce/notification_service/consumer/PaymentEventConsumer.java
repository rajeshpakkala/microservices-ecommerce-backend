package com.ecommerce.notification_service.consumer;

import com.ecommerce.notification_service.event.PaymentEvent;
import com.ecommerce.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "payment-events",
        groupId = "notification-payment-group",
        containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consume(PaymentEvent event) {
        log.info("Notification service received {} event for orderId={}", event.getEventType(), event.getOrderId());
        notificationService.handlePaymentEvent(event);
    }
}
