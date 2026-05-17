package com.ecommerce.payment_service.repository;

import com.ecommerce.payment_service.entity.Subscription;
import com.ecommerce.payment_service.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    Optional<Subscription> findByCustomerIdAndStatusIn(String customerId, List<SubscriptionStatus> statuses);

    List<Subscription> findByCustomerId(String customerId);
}
