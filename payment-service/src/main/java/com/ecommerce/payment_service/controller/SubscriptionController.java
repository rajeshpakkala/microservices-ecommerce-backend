package com.ecommerce.payment_service.controller;

import com.ecommerce.payment_service.dto.*;
import com.ecommerce.payment_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ecommerce/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // ===================== ADMIN — PLAN MANAGEMENT =====================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(201).body(subscriptionService.createPlan(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/plans/{planId}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivatePlan(@PathVariable Long planId) {
        return ResponseEntity.ok(subscriptionService.deactivatePlan(planId));
    }

    // ===================== CUSTOMER — SUBSCRIBE / MANAGE =====================

    // Browse available plans
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getActivePlans() {
        return ResponseEntity.ok(subscriptionService.getActivePlans());
    }

    // Subscribe to a plan — returns authorizationUrl to complete first payment
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/subscribe/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> subscribeToPlan(
            @PathVariable Long planId) {
        return ResponseEntity.status(201).body(subscriptionService.subscribeToPlan(planId));
    }

    // Cancel subscription immediately
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelSubscription(
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(subscriptionId));
    }

    // Get current active subscription
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMySubscription() {
        return ResponseEntity.ok(subscriptionService.getMySubscription());
    }

    // Full subscription history
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my/history")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptionHistory() {
        return ResponseEntity.ok(subscriptionService.getMySubscriptionHistory());
    }

    // Auto-debit renewal history for a subscription
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/{subscriptionId}/renewals")
    public ResponseEntity<ApiResponse<List<RenewalResponse>>> getRenewalHistory(
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getRenewalHistory(subscriptionId));
    }
}
