package com.ecommerce.payment_service.repository;

import com.ecommerce.payment_service.entity.SubscriptionRenewal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRenewalRepository extends JpaRepository<SubscriptionRenewal, Long> {

    List<SubscriptionRenewal> findBySubscriptionIdOrderByChargedAtDesc(Long subscriptionId);
}
