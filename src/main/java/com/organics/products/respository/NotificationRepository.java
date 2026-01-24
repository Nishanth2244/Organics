package com.organics.products.respository;

import com.organics.products.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
       SELECT n FROM Notification n 
       WHERE (n.receiver = :receiver OR n.receiver = 'ALL')
         AND n.read = false 
         AND n.deleted = false
    """)
    Page<Notification> findByReceiverAndReadFalse(@Param("receiver") String receiver, Pageable pageable);

    @Query("""
       SELECT n FROM Notification n 
       WHERE (n.receiver = :receiver OR n.receiver = 'ALL') 
         AND n.deleted = false 
         AND n.category <> 'CHAT'
    """)
    Page<Notification> findNonChatNotificationsByReceiver(@Param("receiver") String receiver,
                                                          Pageable pageable);

    @Query("""
       SELECT COUNT(n) FROM Notification n 
       WHERE (n.receiver = :receiver OR n.receiver = 'ALL')
         AND n.read = false 
         AND n.deleted = false 
         AND n.category <> 'CHAT'
    """)
    Long countNonChatUnreadByReceiver(@Param("receiver") String receiver);

    List<Notification> findByDeletedTrue();

    @Query("""
       SELECT COUNT(n) FROM Notification n 
       WHERE (n.receiver = :receiver OR n.receiver = 'ALL')
         AND n.deleted = false 
         AND n.category <> 'CHAT'
    """)
    long countNonChatByReceiver(@Param("receiver") String receiver);
}