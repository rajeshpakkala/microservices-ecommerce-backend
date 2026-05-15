package com.ecommerce.product_service.kafka;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.event.OrderEvent;
import com.ecommerce.product_service.event.OrderItemEvent;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductRepository productRepository;

    @KafkaListener(topics = "order-events", groupId = "product-service-group")
    public void consume(OrderEvent event) {
        log.info("Received {} event for orderId={}", event.getEventType(), event.getOrderId());

        switch (event.getEventType()) {
            case "ORDER_PLACED"    -> handleOrderPlaced(event);
            case "ORDER_CANCELLED" -> handleOrderCancelled(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleOrderPlaced(OrderEvent event) {
        for (OrderItemEvent item : event.getItems()) {
            productRepository.findById(item.getProductId()).ifPresentOrElse(product -> {
                if (product.getStock() < item.getQuantity()) {
                    log.error("Insufficient stock for productId={}. Available={}, Requested={}",
                            item.getProductId(), product.getStock(), item.getQuantity());
                    return;
                }
                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);
                log.info("Stock deducted for productId={}, remaining={}", product.getId(), product.getStock());
            }, () -> log.error("Product not found: {}", item.getProductId()));
        }
    }

    private void handleOrderCancelled(OrderEvent event) {
        for (OrderItemEvent item : event.getItems()) {
            productRepository.findById(item.getProductId()).ifPresentOrElse(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                log.info("Stock restored for productId={}, new stock={}", product.getId(), product.getStock());
            }, () -> log.error("Product not found: {}", item.getProductId()));
        }
    }
}
