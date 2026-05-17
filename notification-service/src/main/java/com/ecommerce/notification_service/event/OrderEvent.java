package com.ecommerce.notification_service.event;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    private Long orderId;
    private String eventType;
    private String customerId;
    private double totalAmount;
    private List<OrderItemEvent> items;
}
