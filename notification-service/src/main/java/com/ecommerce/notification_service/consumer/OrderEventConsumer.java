package com.ecommerce.notification_service.consumer;

import com.ecommerce.notification_service.event.OrderEvent;
import com.ecommerce.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "order-events",
        groupId = "notification-order-group",
        containerFactory = "orderKafkaListenerContainerFactory"
    )
    public void consume(OrderEvent event) {
        log.info("Notification service received {} event for orderId={}", event.getEventType(), event.getOrderId());
        notificationService.handleOrderEvent(event);
    }
}
