package com.organics.products.service;

import com.organics.products.config.SecurityUtil;
import com.organics.products.entity.EntityType;
import com.organics.products.entity.Notification;
import com.organics.products.entity.User;
import com.organics.products.respository.NotificationRepository;
import com.organics.products.respository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPushService notificationPushService;

    @Autowired
    private PushNotificationService pushNotificationService;


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

        notificationPushService.sendNotificationToUser(notification.getReceiver(), notification);
        handleMobilePush(notification);
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

        baseData.setReceiver("ALL");
        notificationPushService.sendNotificationToUser("ALL", baseData);
        handleMobilePush(baseData);
    }

    private void handleMobilePush(Notification event) {
        try {
            if ("ALL".equalsIgnoreCase(event.getReceiver())) {
                List<String> tokens = userRepository.findAllPushTokens();
                for (String token : tokens) {
                    pushNotificationService.sendNotification(token, event.getSubject(), event.getMessage());
                }
            } else {
                Long userId = Long.parseLong(event.getReceiver());
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty()) {
                        pushNotificationService.sendNotification(user.getExpoPushToken(), event.getSubject(), event.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Mobile Push failed", e);
        }
    }


    public Long getUnreadCount(String receiver) {
        enforceOwnership(receiver);
        return repository.countByReceiverAndReadFalseAndDeletedFalse(receiver);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String receiver, Integer page, Integer size) {
        enforceOwnership(receiver);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repository.findByReceiverAndReadFalseAndDeletedFalse(receiver, pageable).getContent();
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(String receiver, Integer page, Integer size) {
        enforceOwnership(receiver);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return repository.findNonChatNotificationsByReceiver(receiver, pageable).getContent();
    }


    public void markAsRead(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setRead(true);
        repository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        enforceOwnership(userId);
        repository.markAllAsRead(userId);
    }

    public void deleteMessage(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setDeleted(true);
        repository.save(notification);
    }

    @Transactional
    public void deleteAllMessages(String userId) {
        enforceOwnership(userId);
        repository.markAllAsDeleted(userId);
    }

    public void stardMessage(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setStared(true);
        repository.save(notification);
    }

    public void unstardMessage(Long id, String userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        enforceOwnership(notification.getReceiver());

        notification.setStared(false);
        repository.save(notification);
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