package com.ecommerce.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderEvent {
    private Long orderId;
    private String eventType;   // ORDER_PLACED | ORDER_CANCELLED
    private String customerId;
    private List<OrderItemEvent> items;
}
