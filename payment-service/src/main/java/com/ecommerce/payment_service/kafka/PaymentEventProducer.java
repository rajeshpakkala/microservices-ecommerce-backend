package com.ecommerce.payment_service.kafka;

import com.ecommerce.payment_service.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
        log.info("Published {} event for orderId={}", event.getEventType(), event.getOrderId());
    }
}
