package com.ecommerce.payment_service.enums;

public enum SubscriptionStatus {
    CREATED,        // subscription created, awaiting customer authorization
    AUTHENTICATED,  // customer authorized first payment + e-mandate
    ACTIVE,         // live and auto-debiting
    PENDING,        // renewal payment pending (Razorpay retrying)
    HALTED,         // too many failed retries — needs customer action
    CANCELLED,      // cancelled by customer or admin
    COMPLETED,      // all billing cycles done
    EXPIRED         // subscription period ended
}
