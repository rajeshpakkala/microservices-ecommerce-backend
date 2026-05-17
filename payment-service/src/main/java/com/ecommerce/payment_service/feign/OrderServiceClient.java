package com.ecommerce.payment_service.feign;

import com.ecommerce.payment_service.dto.OrderDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "ORDER-SERVICE", url = "${order.service.url:}")
public interface OrderServiceClient {

    @GetMapping("/ecommerce/api/orders/internal/{id}")
    OrderDetailsResponse getOrderDetails(@PathVariable("id") Long id);

    @PostMapping("/ecommerce/api/orders/internal/{id}/confirm")
    void confirmOrder(@PathVariable("id") Long id);

    @PostMapping("/ecommerce/api/orders/internal/{id}/cancel")
    void cancelOrder(@PathVariable("id") Long id);
}
