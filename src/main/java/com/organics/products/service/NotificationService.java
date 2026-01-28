package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.entity.EntityType;
import com.organics.products.entity.Notification;
import com.organics.products.entity.User;
import com.organics.products.respository.NotificationRepository;
import com.organics.products.respository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationProducer producer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // Re-introduced for manual eviction

    // ==================================================================================
    // SENDING LOGIC
    // ==================================================================================

    public void sendNotification(String receiver, String message, String sender, String type,
                                 String link, String category, String kind, String subject,
                                 EntityType entityType, Long entityId) {

        Notification baseData = Notification.builder()
                .message(message).sender(sender).type(type).link(link)
                .read(false).createdAt(LocalDateTime.now()).category(category)
                .kind(kind).subject(subject).entityType(entityType).entityId(entityId)
                .stared(false).deleted(false)
                .build();

        if ("ALL".equalsIgnoreCase(receiver)) {
            sendBroadcastNotificationAsync(baseData);
        } else {
            baseData.setReceiver(receiver);
            sendSingleNotificationAsync(baseData);
        }
    }

    @Async("notificationExecutor")
    public void sendSingleNotificationAsync(Notification notification) {
        repository.save(notification);
        manualEvictUserCache(notification.getReceiver()); // Manual Eviction
        producer.sendNotification(notification);
    }

    @Async("notificationExecutor")
    public void sendBroadcastNotificationAsync(Notification baseData) {
        log.info("Processing Broadcast: {}", baseData.getSubject());

        int pageSize = 500;
        int page = 0;
        Page<User> userPage;

        do {
            userPage = userRepository.findAll(PageRequest.of(page, pageSize));
            List<User> users = userPage.getContent();

            List<Notification> batch = users.stream().map(user -> Notification.builder()
                    .receiver(String.valueOf(user.getId()))
                    .message(baseData.getMessage())
                    .sender(baseData.getSender())
                    .type(baseData.getType())
                    .link(baseData.getLink())
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .category(baseData.getCategory())
                    .kind(baseData.getKind())
                    .subject(baseData.getSubject())
                    .entityType(baseData.getEntityType())
                    .entityId(baseData.getEntityId())
                    .stared(false)
                    .deleted(false)
                    .build()
            ).collect(Collectors.toList());

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }
            page++;
        } while (userPage.hasNext());

        evictAllCaches(); // Clear global cache
        baseData.setReceiver("ALL");
        producer.sendNotification(baseData);
    }

    // ==================================================================================
    // FETCHING LOGIC
    // ==================================================================================

    @Cacheable(value = "unreadCount", key = "#receiver")
    public Long getUnreadCount(String receiver) {
        enforceOwnership(receiver);
        return repository.countByReceiverAndReadFalseAndDeletedFalse(receiver);
    }

    @Cacheable(value = "unreadNotifications", key = "#receiver + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String receiver, Integer page, Integer size) {
        enforceOwnership(receiver);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repository.findByReceiverAndReadFalseAndDeletedFalse(receiver, pageable).getContent();
    }

    @Cacheable(value = "getAllNotifications", key = "#receiver + '_' + #page + '_' + #size")
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(String receiver, Integer page, Integer size) {
        enforceOwnership(receiver);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repository.findNonChatNotificationsByReceiver(receiver, pageable).getContent();
    }

    // ==================================================================================
    // UPDATING / ACTIONS (Fixes Applied Here)
    // ==================================================================================

    public void markAsRead(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setRead(true);
        repository.save(notification);

        // FIX: Manual Call
        manualEvictUserCache(notification.getReceiver());
    }

    @Transactional
    public void markAllAsRead(String userId) {
        enforceOwnership(userId);
        repository.markAllAsRead(userId);
        manualEvictUserCache(userId);
    }

    public void deleteMessage(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setDeleted(true);
        repository.save(notification);

        manualEvictUserCache(notification.getReceiver());
    }

    @Transactional
    public void deleteAllMessages(String userId) {
        enforceOwnership(userId);
        repository.markAllAsDeleted(userId);
        manualEvictUserCache(userId);
    }

    public void stardMessage(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setStared(true);
        repository.save(notification);
        manualEvictUserCache(notification.getReceiver());
    }

    public void unstardMessage(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setStared(false);
        repository.save(notification);
        manualEvictUserCache(notification.getReceiver());
    }

    // ==================================================================================
    // CACHE & SECURITY UTILS
    // ==================================================================================

    @CacheEvict(value = {"unreadCount", "unreadNotifications", "getAllNotifications"}, allEntries = true)
    public void evictAllCaches() {
        log.info("Evicting ALL notification caches.");
    }

    public void manualEvictUserCache(String receiver) {
        try {
            String countKey = "unreadCount::" + receiver;
            redisTemplate.delete(countKey);

         Set<String> unreadKeys = redisTemplate.keys("unreadNotifications::" + receiver + "_*");
            if (unreadKeys != null && !unreadKeys.isEmpty()) redisTemplate.delete(unreadKeys);

            Set<String> allKeys = redisTemplate.keys("getAllNotifications::" + receiver + "_*");
            if (allKeys != null && !allKeys.isEmpty()) redisTemplate.delete(allKeys);

            log.info("Cache evicted for user: {}", receiver);
        } catch (Exception e) {
            log.error("Failed to evict cache for user {}", receiver, e);
        }
    }

    private void enforceOwnership(String receiver) {
        String currentUser = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        if (!currentUser.equals(receiver)) {
            throw new RuntimeException("Forbidden");
        }
    }

    public List<Notification> deletedMessage() {
        return repository.findByDeletedTrue();
    }

    public long getAllCount(String receiver) {
        enforceOwnership(receiver);
        return repository.countNonChatByReceiver(receiver);
    }
}