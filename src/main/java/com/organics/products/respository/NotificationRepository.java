// src/main/java/com/example/notifications/repository/NotificationRepository.java

package com.organics.products.respository;

import com.organics.products.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByReceiverAndReadFalse(String receiver, Pageable pageable);

    Page<Notification> findNonChatNotificationsByReceiver(String receiver, Pageable pageable);

    Long countNonChatUnreadByReceiver(String receiver);

    List<Notification> findByDeletedTrue();
}
