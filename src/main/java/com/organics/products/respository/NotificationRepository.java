package com.organics.products.respository;

import com.organics.products.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverAndReadFalseAndDeletedFalse(String receiver, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.receiver = :receiver AND n.deleted = false AND n.category <> 'CHAT'")
    Page<Notification> findNonChatNotificationsByReceiver(@Param("receiver") String receiver, Pageable pageable);

    Long countByReceiverAndReadFalseAndDeletedFalse(String receiver);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver = :receiver AND n.deleted = false AND n.category <> 'CHAT'")
    long countNonChatByReceiver(@Param("receiver") String receiver);

    @Modifying
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.receiver = :receiver")
    void markAllAsDeleted(@Param("receiver") String receiver);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiver = :receiver AND n.read = false")
    void markAllAsRead(@Param("receiver") String receiver);

    List<Notification> findByDeletedTrue();
}