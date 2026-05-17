package com.ecommerce.notification_service.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEvent {
    private Long productId;
    private int quantity;
}
