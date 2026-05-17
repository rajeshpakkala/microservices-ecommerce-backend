package com.ecommerce.notification_service.repository;

import com.ecommerce.notification_service.entity.NotificationRecord;
import com.ecommerce.notification_service.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByRecipientUsernameOrderBySentAtDesc(String username);

    List<NotificationRecord> findByStatus(NotificationStatus status);
}
