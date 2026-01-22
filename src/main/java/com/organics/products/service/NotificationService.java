package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.entity.EntityType;
import com.organics.products.entity.Notification;
import com.organics.products.respository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private NotificationProducer producer;

    // SYSTEM / ADMIN
    public void sendNotification(String receiver,
                                 String message,
                                 String sender,
                                 String type,
                                 String link,
                                 String category,
                                 String kind,
                                 String subject,
                                 EntityType entityType,
                                 Long entityId) {

        Notification notification = Notification.builder()
                .receiver(receiver)
                .message(message)
                .sender(sender)
                .type(type)
                .link(link)
                .read(false)
                .createdAt(LocalDateTime.now())
                .category(category)
                .kind(kind)
                .subject(subject)
                .entityType(entityType)
                .entityId(entityId)
                .stared(false)
                .deleted(false)
                .build();

        sendNotificationAsync(notification);
    }

    @Async("notificationExecutor")
    @Transactional
    public void sendNotificationAsync(Notification notification) {

        evictCache(notification.getReceiver());
        repository.save(notification);
        producer.sendNotification(notification);

        log.info("Notification {} saved & published for {}",
                notification.getId(), notification.getReceiver());
    }

    @Cacheable(value = "unreadNotifications", key = "#receiver")
    @Transactional
    public List<Notification> getUnreadNotifications(String receiver,
                                                     Integer page,
                                                     Integer size) {

        enforceOwnership(receiver);

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notifications =
                repository.findByReceiverAndReadFalse(receiver, pageable);

        return notifications.getContent();
    }

    public void stardMessage(Long id, String userId) {

        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        enforceOwnership(notification.getReceiver());

        notification.setStared(true);
        repository.save(notification);
        evictCache(notification.getReceiver());
    }

    public void unstardMessage(Long id, String userId) {

        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        enforceOwnership(notification.getReceiver());

        notification.setStared(false);
        repository.save(notification);
        evictCache(notification.getReceiver());
    }

    @Transactional
    public boolean deleteMessage(Long id, String userId) {

        Notification notification =
                repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Notification not found"));

        enforceOwnership(notification.getReceiver());

        notification.setDeleted(true);
        repository.save(notification);
        evictCache(notification.getReceiver());

        return true;
    }

    @Cacheable(value = "getAllNotifications", key = "#receiver + #page + #size")
    @Transactional
    public List<Notification> getAllNotifications(String receiver,
                                                  Integer page,
                                                  Integer size) {

        enforceOwnership(receiver);

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notification =
                repository.findNonChatNotificationsByReceiver(receiver, pageable);

        return notification.getContent();
    }

    public void markAsRead(Long id, String userId) {

        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        enforceOwnership(notification.getReceiver());

        notification.setRead(true);
        repository.save(notification);
        evictCache(notification.getReceiver());
    }

    @CachePut(value = "unreadCount", key = "#receiver")
    @Transactional
    public Long getUnreadCount(String receiver) {

        enforceOwnership(receiver);

        return repository.countNonChatUnreadByReceiver(receiver);
    }

    @CacheEvict(value = { "unreadNotifications",
            "getAllNotifications",
            "unreadCount" },
            key = "#receiver")
    public void evictCache(String receiver) {

        Set<String> keys =
                redisTemplate.keys("getAllNotifications::" + receiver + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        keys =
                redisTemplate.keys("unreadNotifications::" + receiver + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        keys =
                redisTemplate.keys("unreadCount::" + receiver + "*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        log.info("Deleted Redis cache for receiver: {}", receiver);
    }

    public List<Notification> deletedMessage() {
        return repository.findByDeletedTrue();
    }

    // 🔐 Ownership Guard
    private void enforceOwnership(String receiver) {

        String currentUser = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        if (!currentUser.equals(receiver)) {
            throw new RuntimeException("Forbidden");
        }
    }
}
