package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderItem;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.event.OrderEvent;
import com.ecommerce.order_service.event.OrderItemEvent;
import com.ecommerce.order_service.feign.ProductServiceClient;
import com.ecommerce.order_service.kafka.OrderEventProducer;
import com.ecommerce.order_service.repository.OrderItemRepository;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductServiceClient productServiceClient;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductServiceClient productServiceClient,
                        OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productServiceClient = productServiceClient;
        this.orderEventProducer = orderEventProducer;
    }

    // ===================== CUSTOMER OPERATIONS =====================

    @Transactional
    public ApiResponse<OrderResponse> placeOrder(OrderRequest request) {

        String customerId = getCurrentUsername();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        // Step 1: Validate all items via Feign — no side effects yet
        for (OrderItemRequest itemRequest : request.getItems()) {

            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }

            ProductDetailsResponse product =
                    productServiceClient.getProductDetails(itemRequest.getProductId());

            if (!"APPROVED".equals(product.getStatus())) {
                throw new IllegalArgumentException(
                        "Product '" + product.getName() + "' is not available for purchase");
            }

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for '" + product.getName() +
                        "'. Available: " + product.getStock() +
                        ", Requested: " + itemRequest.getQuantity());
            }

            double subtotal = product.getPrice() * itemRequest.getQuantity();
            totalAmount += subtotal;

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .vendorId(product.getVendorId())
                    .price(product.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .subtotal(subtotal)
                    .build());
        }

        // Step 2: Deduct stock via Feign — compensate already-deducted items if any fail
        List<OrderItem> deducted = new ArrayList<>();
        try {
            for (OrderItem item : orderItems) {
                productServiceClient.deductStock(item.getProductId(), item.getQuantity());
                deducted.add(item);
            }
        } catch (Exception e) {
            for (OrderItem item : deducted) {
                productServiceClient.restoreStock(item.getProductId(), item.getQuantity());
            }
            throw new IllegalArgumentException("Stock deduction failed: " + e.getMessage());
        }

        // Step 3: Save order
        Order order = Order.builder()
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .items(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(order);

        // Step 4: Save order items linked to order
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(savedItems);

        // Step 5: Publish ORDER_PLACED event to Kafka — for notifications/analytics
        List<OrderItemEvent> eventItems = savedItems.stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(savedOrder.getId())
                .eventType("ORDER_PLACED")
                .customerId(customerId)
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventItems)
                .build());

        return ApiResponse.<OrderResponse>builder()
                .responseCode(201)
                .responseMessage("Order placed successfully")
                .success(true)
                .responseData(mapToResponse(savedOrder))
                .build();
    }

    public ApiResponse<List<OrderResponse>> getMyOrders() {

        String customerId = getCurrentUsername();

        List<OrderResponse> orders = orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<OrderResponse>>builder()
                .responseCode(200)
                .responseMessage("Orders fetched successfully")
                .success(true)
                .responseData(orders)
                .build();
    }

    public ApiResponse<OrderResponse> getOrderById(Long id) {

        String currentUser = getCurrentUsername();
        String currentRole = getCurrentRole();

        Order order = findOrderById(id);

        // Customers can only view their own orders
        if ("CUSTOMER".equals(currentRole) && !order.getCustomerId().equals(currentUser)) {
            throw new IllegalArgumentException("You can only view your own orders");
        }

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order fetched successfully")
                .success(true)
                .responseData(mapToResponse(order))
                .build();
    }

    public ApiResponse<OrderResponse> cancelOrder(Long id) {

        String customerId = getCurrentUsername();
        Order order = findOrderById(id);

        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only cancel your own orders");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Restore stock via Feign — immediate and consistent
        for (OrderItem item : saved.getItems()) {
            productServiceClient.restoreStock(item.getProductId(), item.getQuantity());
        }

        // Publish ORDER_CANCELLED event to Kafka — for notifications/analytics
        List<OrderItemEvent> eventItems = saved.getItems().stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_CANCELLED")
                .customerId(customerId)
                .totalAmount(saved.getTotalAmount())
                .items(eventItems)
                .build());

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order cancelled successfully")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    // ===================== VENDOR OPERATIONS =====================

    public ApiResponse<List<OrderResponse>> getVendorOrders() {

        String vendorId = getCurrentUsername();

        List<OrderResponse> orders = orderRepository.findOrdersByVendorId(vendorId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<OrderResponse>>builder()
                .responseCode(200)
                .responseMessage("Vendor orders fetched successfully")
                .success(true)
                .responseData(orders)
                .build();
    }

    public ApiResponse<OrderResponse> confirmOrder(Long id) {

        Order order = findOrderById(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING orders can be confirmed. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_CONFIRMED")
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .items(List.of())
                .build());

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order confirmed successfully")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<OrderResponse> shipOrder(Long id) {

        Order order = findOrderById(id);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Only CONFIRMED orders can be shipped. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order saved = orderRepository.save(order);

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_SHIPPED")
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .items(List.of())
                .build());

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order marked as shipped")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    // ===================== ADMIN OPERATIONS =====================

    public ApiResponse<List<OrderResponse>> adminGetAllOrders() {

        List<OrderResponse> orders = orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<OrderResponse>>builder()
                .responseCode(200)
                .responseMessage("All orders fetched successfully")
                .success(true)
                .responseData(orders)
                .build();
    }

    public ApiResponse<OrderResponse> adminGetOrderById(Long id) {

        Order order = findOrderById(id);

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order fetched successfully")
                .success(true)
                .responseData(mapToResponse(order))
                .build();
    }

    public ApiResponse<OrderResponse> deliverOrder(Long id) {

        Order order = findOrderById(id);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalArgumentException(
                    "Only SHIPPED orders can be marked delivered. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        Order saved = orderRepository.save(order);

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_DELIVERED")
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .items(List.of())
                .build());

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order marked as delivered")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<OrderResponse> adminCancelOrder(Long id) {

        Order order = findOrderById(id);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Delivered orders cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_CANCELLED")
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .items(List.of())
                .build());

        return ApiResponse.<OrderResponse>builder()
                .responseCode(200)
                .responseMessage("Order force-cancelled by admin")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<List<OrderResponse>> getOrdersByStatus(OrderStatus status) {

        List<OrderResponse> orders = orderRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<OrderResponse>>builder()
                .responseCode(200)
                .responseMessage("Orders with status " + status + " fetched successfully")
                .success(true)
                .responseData(orders)
                .build();
    }

    // ===================== INTERNAL (service-to-service) =====================

    public OrderInternalResponse getOrderInternal(Long id) {
        Order order = findOrderById(id);
        return OrderInternalResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .build();
    }

    @Transactional
    public void confirmOrderInternal(Long id) {
        Order order = findOrderById(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("confirmOrderInternal: order {} is already {}", id, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_CONFIRMED")
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .items(List.of())
                .build());
        log.info("Order {} confirmed via payment success", id);
    }

    @Transactional
    public void cancelOrderInternal(Long id) {
        Order order = findOrderById(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("cancelOrderInternal: order {} is already {}", id, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        for (OrderItem item : saved.getItems()) {
            productServiceClient.restoreStock(item.getProductId(), item.getQuantity());
        }

        orderEventProducer.publish(OrderEvent.builder()
                .orderId(saved.getId())
                .eventType("ORDER_CANCELLED")
                .customerId(saved.getCustomerId())
                .totalAmount(saved.getTotalAmount())
                .items(List.of())
                .build());
        log.info("Order {} cancelled and stock restored via payment failure", id);
    }

    // ===================== HELPERS =====================

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Order not found with id: " + id));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String getCurrentRole() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
    }

    private OrderResponse mapToResponse(Order order) {

        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(this::mapItemToResponse).toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .vendorId(item.getVendorId())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
