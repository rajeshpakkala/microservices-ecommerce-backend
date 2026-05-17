package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.enums.PlanInterval;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {
    private Long id;
    private String name;
    private String description;
    private double amount;
    private String currency;
    private PlanInterval interval;
    private int totalBillingCycles;
    private boolean active;
}
