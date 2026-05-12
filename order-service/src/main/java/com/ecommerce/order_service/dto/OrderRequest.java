package com.ecommerce.order_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    private String shippingAddress;
    private List<OrderItemRequest> items;
}
