package com.ecommerce.payment_service.kafka;

import com.ecommerce.payment_service.event.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventProducer {

    private static final String TOPIC = "subscription-events";

    private final KafkaTemplate<String, SubscriptionEvent> kafkaTemplate;

    public void publish(SubscriptionEvent event) {
        kafkaTemplate.send(TOPIC, event.getCustomerId(), event);
        log.info("Published {} event for subscriptionId={}", event.getEventType(), event.getSubscriptionId());
    }
}
