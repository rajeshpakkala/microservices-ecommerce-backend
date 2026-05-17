package com.ecommerce.notification_service.service;

import com.ecommerce.notification_service.entity.NotificationRecord;
import com.ecommerce.notification_service.enums.NotificationStatus;
import com.ecommerce.notification_service.enums.NotificationType;
import com.ecommerce.notification_service.event.OrderEvent;
import com.ecommerce.notification_service.event.PaymentEvent;
import com.ecommerce.notification_service.event.SubscriptionEvent;
import com.ecommerce.notification_service.feign.AuthServiceClient;
import com.ecommerce.notification_service.dto.UserInternalResponse;
import com.ecommerce.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;
    private final NotificationRepository notificationRepository;

    // ===================== ORDER NOTIFICATIONS =====================

    public void handleOrderEvent(OrderEvent event) {
        String username = event.getCustomerId();

        switch (event.getEventType()) {
            case "ORDER_PLACED"    -> notify(username, NotificationType.ORDER_PLACED,
                    "Order Placed - #" + event.getOrderId(),
                    buildOrderPlacedEmail(event),
                    String.valueOf(event.getOrderId()));

            case "ORDER_CONFIRMED" -> notify(username, NotificationType.ORDER_CONFIRMED,
                    "Order Confirmed - #" + event.getOrderId(),
                    buildOrderStatusEmail(event, "Confirmed",
                            "Your order has been confirmed by the vendor and is being prepared.",
                            "#2196F3"),
                    String.valueOf(event.getOrderId()));

            case "ORDER_SHIPPED"   -> notify(username, NotificationType.ORDER_SHIPPED,
                    "Order Shipped - #" + event.getOrderId(),
                    buildOrderStatusEmail(event, "Shipped",
                            "Great news! Your order is on its way to you.",
                            "#FF9800"),
                    String.valueOf(event.getOrderId()));

            case "ORDER_DELIVERED" -> notify(username, NotificationType.ORDER_DELIVERED,
                    "Order Delivered - #" + event.getOrderId(),
                    buildOrderStatusEmail(event, "Delivered",
                            "Your order has been delivered. Enjoy your purchase!",
                            "#4CAF50"),
                    String.valueOf(event.getOrderId()));

            case "ORDER_CANCELLED" -> notify(username, NotificationType.ORDER_CANCELLED,
                    "Order Cancelled - #" + event.getOrderId(),
                    buildOrderStatusEmail(event, "Cancelled",
                            "Your order has been cancelled. Any payment will be refunded within 5-7 business days.",
                            "#F44336"),
                    String.valueOf(event.getOrderId()));

            default -> log.warn("Unhandled order event type: {}", event.getEventType());
        }
    }

    // ===================== PAYMENT NOTIFICATIONS =====================

    public void handlePaymentEvent(PaymentEvent event) {
        String username = event.getCustomerId();

        switch (event.getEventType()) {
            case "PAYMENT_SUCCESS" -> notify(username, NotificationType.PAYMENT_SUCCESS,
                    "Payment Successful - Order #" + event.getOrderId(),
                    buildPaymentSuccessEmail(event),
                    String.valueOf(event.getPaymentId()));

            case "PAYMENT_FAILED"  -> notify(username, NotificationType.PAYMENT_FAILED,
                    "Payment Failed - Order #" + event.getOrderId(),
                    buildPaymentFailedEmail(event),
                    String.valueOf(event.getPaymentId()));

            default -> log.warn("Unhandled payment event type: {}", event.getEventType());
        }
    }

    // ===================== SUBSCRIPTION NOTIFICATIONS =====================

    public void handleSubscriptionEvent(SubscriptionEvent event) {
        String username = event.getCustomerId();

        switch (event.getEventType()) {
            case "SUBSCRIPTION_ACTIVATED"  -> notify(username, NotificationType.SUBSCRIPTION_ACTIVATED,
                    "Premium Subscription Activated!",
                    buildSubscriptionActivatedEmail(event),
                    String.valueOf(event.getSubscriptionId()));

            case "SUBSCRIPTION_CHARGED"    -> notify(username, NotificationType.SUBSCRIPTION_CHARGED,
                    "Subscription Renewed - " + event.getPlanName(),
                    buildSubscriptionChargedEmail(event),
                    String.valueOf(event.getSubscriptionId()));

            case "SUBSCRIPTION_HALTED"     -> notify(username, NotificationType.SUBSCRIPTION_HALTED,
                    "Action Required: Subscription Payment Failed",
                    buildSubscriptionHaltedEmail(event),
                    String.valueOf(event.getSubscriptionId()));

            case "SUBSCRIPTION_CANCELLED"  -> notify(username, NotificationType.SUBSCRIPTION_CANCELLED,
                    "Subscription Cancelled - " + event.getPlanName(),
                    buildSubscriptionCancelledEmail(event),
                    String.valueOf(event.getSubscriptionId()));

            default -> log.warn("Unhandled subscription event type: {}", event.getEventType());
        }
    }

    // ===================== CORE NOTIFICATION SENDER =====================

    private void notify(String username, NotificationType type, String subject, String htmlBody, String referenceId) {

        String email = null;
        try {
            UserInternalResponse user = authServiceClient.getUserByUsername(username);
            email = user.getEmail();
        } catch (Exception e) {
            log.error("Could not fetch email for user {}: {}", username, e.getMessage());
            saveRecord(username, "unknown@unknown.com", type, subject, referenceId,
                    NotificationStatus.FAILED, "Could not fetch user email: " + e.getMessage());
            return;
        }

        try {
            emailService.sendHtmlEmail(email, subject, htmlBody);
            saveRecord(username, email, type, subject, referenceId, NotificationStatus.SENT, null);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", email, e.getMessage());
            saveRecord(username, email, type, subject, referenceId,
                    NotificationStatus.FAILED, e.getMessage());
        }
    }

    private void saveRecord(String username, String email, NotificationType type,
                             String subject, String referenceId,
                             NotificationStatus status, String errorMessage) {
        notificationRepository.save(NotificationRecord.builder()
                .recipientUsername(username)
                .recipientEmail(email)
                .type(type)
                .subject(subject)
                .referenceId(referenceId)
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    // ===================== EMAIL TEMPLATES =====================

    private String buildOrderPlacedEmail(OrderEvent event) {
        return baseTemplate("Order Placed Successfully!", "#4CAF50",
                "Hi there,",
                "Your order <strong>#" + event.getOrderId() + "</strong> has been placed successfully.",
                "Order Total: <strong>₹" + String.format("%.2f", event.getTotalAmount()) + "</strong>",
                "We will notify you once the vendor confirms your order.",
                "View Order");
    }

    private String buildOrderStatusEmail(OrderEvent event, String status, String message, String color) {
        return baseTemplate("Order " + status, color,
                "Hi there,",
                "Your order <strong>#" + event.getOrderId() + "</strong> has been " + status.toLowerCase() + ".",
                "Order Total: <strong>₹" + String.format("%.2f", event.getTotalAmount()) + "</strong>",
                message,
                "View Order");
    }

    private String buildPaymentSuccessEmail(PaymentEvent event) {
        return baseTemplate("Payment Successful!", "#4CAF50",
                "Hi there,",
                "Your payment for Order <strong>#" + event.getOrderId() + "</strong> was successful.",
                "Amount Paid: <strong>₹" + String.format("%.2f", event.getAmount()) + "</strong>",
                "Your order will be confirmed and prepared shortly.",
                "View Order");
    }

    private String buildPaymentFailedEmail(PaymentEvent event) {
        return baseTemplate("Payment Failed", "#F44336",
                "Hi there,",
                "Unfortunately, your payment for Order <strong>#" + event.getOrderId() + "</strong> failed.",
                "Amount: <strong>₹" + String.format("%.2f", event.getAmount()) + "</strong>",
                "Your order has been cancelled and any reserved stock has been released. Please try ordering again.",
                "Try Again");
    }

    private String buildSubscriptionActivatedEmail(SubscriptionEvent event) {
        return baseTemplate("Welcome to Premium!", "#9C27B0",
                "Hi there,",
                "Your <strong>" + event.getPlanName() + "</strong> subscription is now active!",
                "Amount: <strong>₹" + String.format("%.2f", event.getAmount()) + "</strong> per cycle",
                "You now have full access to all premium features. Your subscription will auto-renew at the end of each billing cycle.",
                "Explore Premium");
    }

    private String buildSubscriptionChargedEmail(SubscriptionEvent event) {
        return baseTemplate("Subscription Renewed", "#9C27B0",
                "Hi there,",
                "Your <strong>" + event.getPlanName() + "</strong> subscription has been successfully renewed.",
                "Amount Charged: <strong>₹" + String.format("%.2f", event.getAmount()) + "</strong>",
                "Your premium access continues uninterrupted. Thank you for your continued trust.",
                "View Subscription");
    }

    private String buildSubscriptionHaltedEmail(SubscriptionEvent event) {
        return baseTemplate("Action Required: Subscription Halted", "#F44336",
                "Hi there,",
                "Your <strong>" + event.getPlanName() + "</strong> subscription has been halted due to repeated payment failures.",
                "",
                "Your premium access has been paused. Please update your payment method and re-authorize the subscription to continue enjoying premium benefits.",
                "Update Payment");
    }

    private String buildSubscriptionCancelledEmail(SubscriptionEvent event) {
        return baseTemplate("Subscription Cancelled", "#607D8B",
                "Hi there,",
                "Your <strong>" + event.getPlanName() + "</strong> subscription has been cancelled.",
                "",
                "You will continue to have access to premium features until the end of your current billing period. We hope to see you again!",
                "Resubscribe");
    }

    // ===================== BASE HTML TEMPLATE =====================

    private String baseTemplate(String title, String accentColor,
                                 String greeting, String mainLine,
                                 String detail, String bodyMessage,
                                 String ctaText) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:30px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                        <!-- Header -->
                        <tr>
                          <td style="background:%s;padding:30px 40px;text-align:center;">
                            <h1 style="color:#ffffff;margin:0;font-size:24px;">%s</h1>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:40px;">
                            <p style="color:#333;font-size:16px;margin:0 0 16px;">%s</p>
                            <p style="color:#333;font-size:16px;margin:0 0 16px;">%s</p>
                            %s
                            <p style="color:#555;font-size:14px;margin:24px 0 0;">%s</p>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#f9f9f9;padding:20px 40px;text-align:center;border-top:1px solid #eee;">
                            <p style="color:#999;font-size:12px;margin:0;">
                              © 2025 E-Commerce Platform. All rights reserved.<br/>
                              This is an automated notification. Please do not reply to this email.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                accentColor, title, greeting, mainLine,
                detail.isEmpty() ? "" : "<p style=\"color:#333;font-size:16px;font-weight:bold;margin:0 0 16px;\">" + detail + "</p>",
                bodyMessage
        );
    }
}
