package com.ecommerce.notification_service.consumer;

import com.ecommerce.notification_service.event.SubscriptionEvent;
import com.ecommerce.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "subscription-events",
        groupId = "notification-subscription-group",
        containerFactory = "subscriptionKafkaListenerContainerFactory"
    )
    public void consume(SubscriptionEvent event) {
        log.info("Notification service received {} event for subscriptionId={}", event.getEventType(), event.getSubscriptionId());
        notificationService.handleSubscriptionEvent(event);
    }
}
