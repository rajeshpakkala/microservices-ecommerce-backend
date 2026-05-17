package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.entity.Payment;
import com.ecommerce.payment_service.enums.PaymentStatus;
import com.ecommerce.payment_service.event.PaymentEvent;
import com.ecommerce.payment_service.feign.OrderServiceClient;
import com.ecommerce.payment_service.kafka.PaymentEventProducer;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final PaymentEventProducer paymentEventProducer;
    private final SubscriptionService subscriptionService;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderServiceClient orderServiceClient,
                          PaymentEventProducer paymentEventProducer,
                          SubscriptionService subscriptionService) {
        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
        this.paymentEventProducer = paymentEventProducer;
        this.subscriptionService = subscriptionService;
    }

    public ApiResponse<PaymentInitiateResponse> initiatePayment(PaymentInitiateRequest request) {

        String customerId = getCurrentUsername();

        paymentRepository.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                throw new IllegalArgumentException("Payment already completed for order: " + request.getOrderId());
            }
        });

        OrderDetailsResponse order = orderServiceClient.getOrderDetails(request.getOrderId());

        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only pay for your own orders");
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException(
                    "Payment can only be initiated for PENDING orders. Current status: " + order.getStatus());
        }

        com.razorpay.Order razorpayOrder = createRazorpayOrder(request.getOrderId(), order.getTotalAmount());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(customerId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .razorpayOrderId(razorpayOrder.get("id"))
                .clientVerified(false)
                .webhookConfirmed(false)
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);

        return ApiResponse.<PaymentInitiateResponse>builder()
                .responseCode(201)
                .responseMessage("Payment initiated. Complete payment using Razorpay.")
                .success(true)
                .responseData(PaymentInitiateResponse.builder()
                        .paymentId(saved.getId())
                        .razorpayOrderId(razorpayOrder.get("id"))
                        .amount(order.getTotalAmount())
                        .currency("INR")
                        .keyId(keyId)
                        .build())
                .build();
    }

    // Called by frontend after customer completes payment on Razorpay popup
    @Transactional
    public ApiResponse<PaymentResponse> verifyPayment(PaymentVerifyRequest request) {

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Payment not found for razorpayOrderId: " + request.getRazorpayOrderId()));

        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new IllegalArgumentException("This payment has already failed");
        }

        if (payment.isClientVerified()) {
            throw new IllegalArgumentException("Client verification already done for this payment");
        }

        boolean isValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            // Cancel order and restore stock synchronously via Feign
            orderServiceClient.cancelOrder(payment.getOrderId());
            publishEvent(payment, "PAYMENT_FAILED");
            log.warn("Client verification failed — invalid signature for orderId={}", payment.getOrderId());
            throw new IllegalArgumentException("Payment verification failed. Invalid signature.");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setClientVerified(true);
        log.info("Client verified payment for orderId={}", payment.getOrderId());

        // Confirm only if webhook has also arrived
        checkAndConfirm(payment);

        paymentRepository.save(payment);

        String message = payment.getStatus() == PaymentStatus.SUCCESS
                ? "Payment fully confirmed by both client and webhook"
                : "Client verification successful. Awaiting Razorpay webhook confirmation.";

        return ApiResponse.<PaymentResponse>builder()
                .responseCode(200)
                .responseMessage(message)
                .success(true)
                .responseData(mapToResponse(payment))
                .build();
    }

    // Called by Razorpay servers directly — always fires regardless of frontend state
    @Transactional
    public void handleWebhook(String payload, String razorpaySignature) {

        if (!verifyWebhookSignature(payload, razorpaySignature)) {
            log.warn("Invalid Razorpay webhook signature received");
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        // Route subscription events to SubscriptionService
        if (eventType.startsWith("subscription.")) {
            subscriptionService.handleSubscriptionWebhook(event, eventType);
            return;
        }

        JSONObject paymentEntity = event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId = paymentEntity.getString("order_id");
        String razorpayPaymentId = paymentEntity.getString("id");

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (payment == null) {
            log.warn("Webhook received for unknown razorpayOrderId={}", razorpayOrderId);
            return;
        }

        if ("payment.captured".equals(eventType)) {

            if (payment.isWebhookConfirmed()) {
                log.info("Webhook already processed for orderId={}", payment.getOrderId());
                return;
            }

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setWebhookConfirmed(true);
            log.info("Webhook confirmed payment for orderId={}", payment.getOrderId());

            // Confirm only if client has also verified
            checkAndConfirm(payment);

            paymentRepository.save(payment);

        } else if ("payment.failed".equals(eventType)) {

            if (payment.getStatus() == PaymentStatus.FAILED) {
                log.info("Payment already marked failed for orderId={}", payment.getOrderId());
                return;
            }

            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            // Cancel order and restore stock synchronously via Feign
            orderServiceClient.cancelOrder(payment.getOrderId());
            publishEvent(payment, "PAYMENT_FAILED");
            log.warn("Webhook: Payment failed for orderId={}", payment.getOrderId());
        }
    }

    public ApiResponse<PaymentResponse> getPaymentByOrderId(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No payment found for orderId: " + orderId));

        return ApiResponse.<PaymentResponse>builder()
                .responseCode(200)
                .responseMessage("Payment fetched successfully")
                .success(true)
                .responseData(mapToResponse(payment))
                .build();
    }

    // ===================== HELPERS =====================

    // Fires SUCCESS only when BOTH client and webhook have confirmed
    private void checkAndConfirm(Payment payment) {
        if (payment.isClientVerified() && payment.isWebhookConfirmed()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            // Confirm order synchronously via Feign — immediate, no Kafka delay
            orderServiceClient.confirmOrder(payment.getOrderId());
            publishEvent(payment, "PAYMENT_SUCCESS");
            log.info("Dual confirmation complete — order {} confirmed via Feign", payment.getOrderId());
        }
    }

    private void publishEvent(Payment payment, String eventType) {
        paymentEventProducer.publish(PaymentEvent.builder()
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .eventType(eventType)
                .build());
    }

    private com.razorpay.Order createRazorpayOrder(Long orderId, double amount) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount", (int) (amount * 100));
            options.put("currency", "INR");
            options.put("receipt", "order_" + orderId);
            options.put("payment_capture", 1);
            return client.orders.create(options);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for orderId={}: {}", orderId, e.getMessage());
            throw new RuntimeException("Failed to create payment order. Please try again.");
        }
    }

    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(razorpaySignature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyWebhookSignature(String payload, String razorpaySignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(razorpaySignature);
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .clientVerified(payment.isClientVerified())
                .webhookConfirmed(payment.isWebhookConfirmed())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
