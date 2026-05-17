package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.enums.PlanInterval;
import lombok.Data;

@Data
public class CreatePlanRequest {
    private String name;
    private String description;
    private double amount;
    private PlanInterval interval;
}
