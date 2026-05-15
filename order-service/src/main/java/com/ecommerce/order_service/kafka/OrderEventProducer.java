package com.ecommerce.order_service.kafka;

import com.ecommerce.order_service.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
        log.info("Published {} event for orderId={}", event.getEventType(), event.getOrderId());
    }
}
