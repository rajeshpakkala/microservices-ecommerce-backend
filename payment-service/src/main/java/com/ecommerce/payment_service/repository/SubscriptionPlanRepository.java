package com.ecommerce.payment_service.repository;

import com.ecommerce.payment_service.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByActiveTrue();

    Optional<SubscriptionPlan> findByRazorpayPlanId(String razorpayPlanId);
}
