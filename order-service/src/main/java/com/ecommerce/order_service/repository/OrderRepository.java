package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT DISTINCT oi.order FROM OrderItem oi WHERE oi.vendorId = :vendorId")
    List<Order> findOrdersByVendorId(@Param("vendorId") String vendorId);
}
