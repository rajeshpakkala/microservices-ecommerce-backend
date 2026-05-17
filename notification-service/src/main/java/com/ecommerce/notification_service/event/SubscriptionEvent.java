package com.ecommerce.notification_service.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEvent {
    private Long subscriptionId;
    private String customerId;
    private String planName;
    private double amount;
    private String eventType;
}
