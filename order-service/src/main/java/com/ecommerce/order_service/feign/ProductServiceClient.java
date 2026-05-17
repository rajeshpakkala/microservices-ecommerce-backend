package com.ecommerce.order_service.feign;

import com.ecommerce.order_service.dto.ProductDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "PRODUCT-SERVICE", url = "${product.service.url:}")
public interface ProductServiceClient {

    @GetMapping("/ecommerce/api/products/internal/{id}")
    ProductDetailsResponse getProductDetails(@PathVariable("id") Long id);

    @PostMapping("/ecommerce/api/products/internal/{id}/deduct-stock")
    void deductStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @PostMapping("/ecommerce/api/products/internal/{id}/restore-stock")
    void restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
