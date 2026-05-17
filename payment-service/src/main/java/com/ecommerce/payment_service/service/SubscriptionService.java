package com.ecommerce.payment_service.service;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.entity.Subscription;
import com.ecommerce.payment_service.entity.SubscriptionPlan;
import com.ecommerce.payment_service.entity.SubscriptionRenewal;
import com.ecommerce.payment_service.enums.PlanInterval;
import com.ecommerce.payment_service.enums.SubscriptionStatus;
import com.ecommerce.payment_service.event.SubscriptionEvent;
import com.ecommerce.payment_service.kafka.SubscriptionEventProducer;
import com.ecommerce.payment_service.repository.SubscriptionPlanRepository;
import com.ecommerce.payment_service.repository.SubscriptionRepository;
import com.ecommerce.payment_service.repository.SubscriptionRenewalRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionRenewalRepository renewalRepository;
    private final SubscriptionEventProducer eventProducer;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public SubscriptionService(SubscriptionPlanRepository planRepository,
                                SubscriptionRepository subscriptionRepository,
                                SubscriptionRenewalRepository renewalRepository,
                                SubscriptionEventProducer eventProducer) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.renewalRepository = renewalRepository;
        this.eventProducer = eventProducer;
    }

    // ===================== ADMIN — PLAN MANAGEMENT =====================

    public ApiResponse<PlanResponse> createPlan(CreatePlanRequest request) {

        // Create plan in Razorpay first
        String razorpayPlanId = createRazorpayPlan(request);

        int totalCycles = defaultBillingCycles(request.getInterval());

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency("INR")
                .interval(request.getInterval())
                .razorpayPlanId(razorpayPlanId)
                .totalBillingCycles(totalCycles)
                .active(true)
                .build();

        SubscriptionPlan saved = planRepository.save(plan);
        log.info("Plan created: {} (razorpayPlanId={})", saved.getName(), razorpayPlanId);

        return ApiResponse.<PlanResponse>builder()
                .responseCode(201)
                .responseMessage("Plan created successfully")
                .success(true)
                .responseData(mapPlanToResponse(saved))
                .build();
    }

    public ApiResponse<String> deactivatePlan(Long planId) {

        SubscriptionPlan plan = findPlanById(planId);
        plan.setActive(false);
        planRepository.save(plan);

        return ApiResponse.<String>builder()
                .responseCode(200)
                .responseMessage("Plan deactivated. Existing subscribers are unaffected.")
                .success(true)
                .responseData("Plan '" + plan.getName() + "' deactivated")
                .build();
    }

    // ===================== CUSTOMER — SUBSCRIBE =====================

    public ApiResponse<List<PlanResponse>> getActivePlans() {

        List<PlanResponse> plans = planRepository.findByActiveTrue()
                .stream()
                .map(this::mapPlanToResponse)
                .toList();

        return ApiResponse.<List<PlanResponse>>builder()
                .responseCode(200)
                .responseMessage("Active plans fetched successfully")
                .success(true)
                .responseData(plans)
                .build();
    }

    @Transactional
    public ApiResponse<SubscriptionResponse> subscribeToPlan(Long planId) {

        String customerId = getCurrentUsername();

        // Block if customer already has an active subscription
        subscriptionRepository.findByCustomerIdAndStatusIn(customerId,
                List.of(SubscriptionStatus.CREATED, SubscriptionStatus.AUTHENTICATED,
                        SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "You already have an active subscription (id=" + existing.getId() +
                            "). Cancel it before subscribing to a new plan.");
                });

        SubscriptionPlan plan = findPlanById(planId);

        if (!plan.isActive()) {
            throw new IllegalArgumentException("This plan is no longer available");
        }

        // Create Razorpay subscription
        com.razorpay.Subscription razorpaySubscription = createRazorpaySubscription(plan);

        Subscription subscription = Subscription.builder()
                .customerId(customerId)
                .plan(plan)
                .razorpaySubscriptionId(razorpaySubscription.get("id"))
                .authorizationUrl(razorpaySubscription.get("short_url"))
                .status(SubscriptionStatus.CREATED)
                .paidCount(0)
                .remainingCount(plan.getTotalBillingCycles())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);

        return ApiResponse.<SubscriptionResponse>builder()
                .responseCode(201)
                .responseMessage("Subscription created. Visit authorizationUrl to complete first payment and activate auto-debit.")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    @Transactional
    public ApiResponse<String> cancelSubscription(Long subscriptionId) {

        String customerId = getCurrentUsername();

        Subscription subscription = findSubscriptionById(subscriptionId);

        if (!subscription.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only cancel your own subscription");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED ||
            subscription.getStatus() == SubscriptionStatus.COMPLETED ||
            subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new IllegalArgumentException("Subscription is already " + subscription.getStatus());
        }

        // Cancel in Razorpay — cancel_at_cycle_end=0 means cancel immediately
        cancelRazorpaySubscription(subscription.getRazorpaySubscriptionId(), false);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        publishSubscriptionEvent(subscription, "SUBSCRIPTION_CANCELLED");
        log.info("Subscription {} cancelled by customer {}", subscriptionId, customerId);

        return ApiResponse.<String>builder()
                .responseCode(200)
                .responseMessage("Subscription cancelled successfully")
                .success(true)
                .responseData("Your premium access will remain until end of current billing period")
                .build();
    }

    public ApiResponse<SubscriptionResponse> getMySubscription() {

        String customerId = getCurrentUsername();

        Subscription subscription = subscriptionRepository
                .findByCustomerIdAndStatusIn(customerId,
                        List.of(SubscriptionStatus.CREATED, SubscriptionStatus.AUTHENTICATED,
                                SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING,
                                SubscriptionStatus.HALTED))
                .orElseThrow(() -> new NoSuchElementException("No active subscription found"));

        return ApiResponse.<SubscriptionResponse>builder()
                .responseCode(200)
                .responseMessage("Subscription fetched successfully")
                .success(true)
                .responseData(mapToResponse(subscription))
                .build();
    }

    public ApiResponse<List<SubscriptionResponse>> getMySubscriptionHistory() {

        String customerId = getCurrentUsername();

        List<SubscriptionResponse> history = subscriptionRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<SubscriptionResponse>>builder()
                .responseCode(200)
                .responseMessage("Subscription history fetched successfully")
                .success(true)
                .responseData(history)
                .build();
    }

    public ApiResponse<List<RenewalResponse>> getRenewalHistory(Long subscriptionId) {

        String customerId = getCurrentUsername();
        Subscription subscription = findSubscriptionById(subscriptionId);

        if (!subscription.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only view your own renewal history");
        }

        List<RenewalResponse> renewals = renewalRepository
                .findBySubscriptionIdOrderByChargedAtDesc(subscriptionId)
                .stream()
                .map(this::mapRenewalToResponse)
                .toList();

        return ApiResponse.<List<RenewalResponse>>builder()
                .responseCode(200)
                .responseMessage("Renewal history fetched successfully")
                .success(true)
                .responseData(renewals)
                .build();
    }

    // ===================== WEBHOOK HANDLER (called from PaymentService) =====================

    @Transactional
    public void handleSubscriptionWebhook(JSONObject event, String eventType) {

        JSONObject subscriptionEntity = event.getJSONObject("payload")
                .getJSONObject("subscription")
                .getJSONObject("entity");

        String razorpaySubscriptionId = subscriptionEntity.getString("id");

        Subscription subscription = subscriptionRepository
                .findByRazorpaySubscriptionId(razorpaySubscriptionId)
                .orElse(null);

        if (subscription == null) {
            log.warn("Webhook received for unknown subscriptionId={}", razorpaySubscriptionId);
            return;
        }

        switch (eventType) {

            case "subscription.authenticated" -> {
                // Customer completed first payment + e-mandate authorized
                subscription.setStatus(SubscriptionStatus.AUTHENTICATED);
                subscriptionRepository.save(subscription);
                log.info("Subscription {} authenticated by customer", subscriptionId(subscription));
            }

            case "subscription.activated" -> {
                // Subscription is now live — auto-debit will start
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                updateBillingPeriod(subscription, subscriptionEntity);
                subscriptionRepository.save(subscription);
                publishSubscriptionEvent(subscription, "SUBSCRIPTION_ACTIVATED");
                log.info("Subscription {} activated — customer {} is now PREMIUM", subscriptionId(subscription), subscription.getCustomerId());
            }

            case "subscription.charged" -> {
                // Auto-debit successful — renewal payment collected
                handleRenewalCharged(subscription, subscriptionEntity, event);
            }

            case "subscription.pending" -> {
                // Renewal payment pending — Razorpay will retry
                subscription.setStatus(SubscriptionStatus.PENDING);
                subscriptionRepository.save(subscription);
                log.warn("Subscription {} renewal payment pending", subscriptionId(subscription));
            }

            case "subscription.halted" -> {
                // Too many failed retries — customer must re-authorize
                subscription.setStatus(SubscriptionStatus.HALTED);
                subscriptionRepository.save(subscription);
                publishSubscriptionEvent(subscription, "SUBSCRIPTION_HALTED");
                log.warn("Subscription {} halted — customer {} lost premium access", subscriptionId(subscription), subscription.getCustomerId());
            }

            case "subscription.cancelled" -> {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscriptionRepository.save(subscription);
                publishSubscriptionEvent(subscription, "SUBSCRIPTION_CANCELLED");
                log.info("Subscription {} cancelled via webhook", subscriptionId(subscription));
            }

            case "subscription.completed" -> {
                // All billing cycles completed
                subscription.setStatus(SubscriptionStatus.COMPLETED);
                subscriptionRepository.save(subscription);
                publishSubscriptionEvent(subscription, "SUBSCRIPTION_CANCELLED"); // treat as cancelled — premium ends
                log.info("Subscription {} completed all billing cycles", subscriptionId(subscription));
            }

            default -> log.warn("Unhandled subscription event type: {}", eventType);
        }
    }

    // ===================== PRIVATE HELPERS =====================

    private void handleRenewalCharged(Subscription subscription, JSONObject subscriptionEntity, JSONObject fullEvent) {

        // Extract payment details from the webhook payload
        String razorpayPaymentId = null;
        try {
            razorpayPaymentId = fullEvent.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity")
                    .getString("id");
        } catch (Exception e) {
            log.warn("Could not extract payment id from subscription.charged event");
        }

        int paidCount = subscriptionEntity.optInt("paid_count", subscription.getPaidCount() + 1);
        int remainingCount = subscriptionEntity.optInt("remaining_count", subscription.getRemainingCount() - 1);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaidCount(paidCount);
        subscription.setRemainingCount(remainingCount);
        updateBillingPeriod(subscription, subscriptionEntity);
        subscriptionRepository.save(subscription);

        // Record the renewal charge
        SubscriptionRenewal renewal = SubscriptionRenewal.builder()
                .subscriptionId(subscription.getId())
                .customerId(subscription.getCustomerId())
                .razorpayPaymentId(razorpayPaymentId)
                .amount(subscription.getPlan().getAmount())
                .currency(subscription.getPlan().getCurrency())
                .cycleNumber(paidCount)
                .build();

        renewalRepository.save(renewal);

        publishSubscriptionEvent(subscription, "SUBSCRIPTION_CHARGED");
        log.info("Subscription {} auto-charged for cycle {} — customer {}", subscriptionId(subscription), paidCount, subscription.getCustomerId());
    }

    private void updateBillingPeriod(Subscription subscription, JSONObject subscriptionEntity) {
        try {
            long currentStart = subscriptionEntity.optLong("current_start", 0);
            long currentEnd = subscriptionEntity.optLong("current_end", 0);
            long nextBilling = subscriptionEntity.optLong("charge_at", 0);

            if (currentStart > 0) {
                subscription.setCurrentPeriodStart(toDateTime(currentStart));
            }
            if (currentEnd > 0) {
                subscription.setCurrentPeriodEnd(toDateTime(currentEnd));
            }
            if (nextBilling > 0) {
                subscription.setNextBillingAt(toDateTime(nextBilling));
            }
        } catch (Exception e) {
            log.warn("Could not parse billing period from webhook: {}", e.getMessage());
        }
    }

    private String createRazorpayPlan(CreatePlanRequest request) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();

            // Map our interval to Razorpay period + interval
            options.put("period", razorpayPeriod(request.getInterval()));
            options.put("interval", razorpayInterval(request.getInterval()));

            JSONObject item = new JSONObject();
            item.put("name", request.getName());
            item.put("amount", (int) (request.getAmount() * 100)); // paise
            item.put("currency", "INR");
            item.put("description", request.getDescription() != null ? request.getDescription() : "");
            options.put("item", item);

            com.razorpay.Plan plan = client.plans.create(options);
            return plan.get("id");

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay plan: {}", e.getMessage());
            throw new RuntimeException("Failed to create plan in Razorpay: " + e.getMessage());
        }
    }

    private com.razorpay.Subscription createRazorpaySubscription(SubscriptionPlan plan) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("plan_id", plan.getRazorpayPlanId());
            options.put("total_count", plan.getTotalBillingCycles());
            options.put("quantity", 1);
            options.put("customer_notify", 1); // Razorpay sends billing emails to customer

            return client.subscriptions.create(options);

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay subscription for planId={}: {}", plan.getId(), e.getMessage());
            throw new RuntimeException("Failed to create subscription in Razorpay: " + e.getMessage());
        }
    }

    private void cancelRazorpaySubscription(String razorpaySubscriptionId, boolean atCycleEnd) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("cancel_at_cycle_end", atCycleEnd ? 1 : 0);
            client.subscriptions.cancel(razorpaySubscriptionId, options);
        } catch (RazorpayException e) {
            log.error("Failed to cancel Razorpay subscription {}: {}", razorpaySubscriptionId, e.getMessage());
            throw new RuntimeException("Failed to cancel subscription in Razorpay: " + e.getMessage());
        }
    }

    private void publishSubscriptionEvent(Subscription subscription, String eventType) {
        eventProducer.publish(SubscriptionEvent.builder()
                .subscriptionId(subscription.getId())
                .customerId(subscription.getCustomerId())
                .planName(subscription.getPlan().getName())
                .amount(subscription.getPlan().getAmount())
                .eventType(eventType)
                .build());
    }

    // Razorpay period mapping
    private String razorpayPeriod(PlanInterval interval) {
        return switch (interval) {
            case MONTHLY, QUARTERLY, HALF_YEARLY -> "monthly";
            case ANNUAL -> "yearly";
        };
    }

    // Razorpay interval multiplier
    private int razorpayInterval(PlanInterval interval) {
        return switch (interval) {
            case MONTHLY    -> 1;
            case QUARTERLY  -> 3;
            case HALF_YEARLY -> 6;
            case ANNUAL     -> 1;
        };
    }

    // Default max billing cycles per interval (1 year worth by default)
    private int defaultBillingCycles(PlanInterval interval) {
        return switch (interval) {
            case MONTHLY    -> 12;
            case QUARTERLY  -> 4;
            case HALF_YEARLY -> 2;
            case ANNUAL     -> 1;
        };
    }

    private LocalDateTime toDateTime(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String subscriptionId(Subscription s) {
        return s.getId() + " (rzp=" + s.getRazorpaySubscriptionId() + ")";
    }

    private SubscriptionPlan findPlanById(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found with id: " + planId));
    }

    private Subscription findSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found with id: " + subscriptionId));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PlanResponse mapPlanToResponse(SubscriptionPlan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .interval(plan.getInterval())
                .totalBillingCycles(plan.getTotalBillingCycles())
                .active(plan.isActive())
                .build();
    }

    private SubscriptionResponse mapToResponse(Subscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId())
                .customerId(s.getCustomerId())
                .plan(mapPlanToResponse(s.getPlan()))
                .razorpaySubscriptionId(s.getRazorpaySubscriptionId())
                .authorizationUrl(s.getAuthorizationUrl())
                .status(s.getStatus())
                .paidCount(s.getPaidCount())
                .remainingCount(s.getRemainingCount())
                .currentPeriodStart(s.getCurrentPeriodStart())
                .currentPeriodEnd(s.getCurrentPeriodEnd())
                .nextBillingAt(s.getNextBillingAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private RenewalResponse mapRenewalToResponse(SubscriptionRenewal r) {
        return RenewalResponse.builder()
                .id(r.getId())
                .subscriptionId(r.getSubscriptionId())
                .razorpayPaymentId(r.getRazorpayPaymentId())
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .cycleNumber(r.getCycleNumber())
                .chargedAt(r.getChargedAt())
                .build();
    }
}
