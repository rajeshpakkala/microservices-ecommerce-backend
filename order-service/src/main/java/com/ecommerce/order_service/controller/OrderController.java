package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.ApiResponse;
import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ecommerce/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ===================== CUSTOMER APIs =====================

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @RequestBody OrderRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.placeOrder(request));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {

        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    // ===================== VENDOR APIs =====================

    @PreAuthorize("hasRole('VENDOR')")
    @GetMapping("/vendor/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getVendorOrders() {

        return ResponseEntity.ok(orderService.getVendorOrders());
    }

    @PreAuthorize("hasRole('VENDOR')")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @PreAuthorize("hasRole('VENDOR')")
    @PutMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.shipOrder(id));
    }

    // ===================== ADMIN APIs =====================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> adminGetAllOrders() {

        return ResponseEntity.ok(orderService.adminGetAllOrders());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> adminGetOrderById(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.adminGetOrderById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.deliverOrder(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> adminCancelOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(orderService.adminCancelOrder(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status) {

        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }
}
