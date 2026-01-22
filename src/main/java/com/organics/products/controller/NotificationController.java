package com.organics.products.controller;

import com.organics.products.config.SecurityUtil;
import com.organics.products.dto.NotificationDTO;
import com.organics.products.entity.Notification;
import com.organics.products.service.NotificationPushService;
import com.organics.products.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@Slf4j
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationPushService pushService;

    // ADMIN / SYSTEM ONLY
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody Notification notification) {

        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new RuntimeException("Forbidden: ADMIN only");
        }

        log.info("Notification received: {}", notification);

        notificationService.sendNotification(
                notification.getReceiver(),
                notification.getMessage(),
                notification.getSender(),
                notification.getType(),
                notification.getLink(),
                notification.getCategory(),
                notification.getKind(),
                notification.getSubject(),
                notification.getEntityType(),
                notification.getEntityId()
        );
        return ResponseEntity.ok("Notification sent");
    }

    // ADMIN / SYSTEM ONLY
    @PostMapping("/sendList")
    public ResponseEntity<String> sendNotificationList(@RequestBody List<Notification> notifications) {

        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new RuntimeException("Forbidden: ADMIN only");
        }

        log.info("Notification list received: {}", notifications);

        for (Notification notification : notifications) {
            notificationService.sendNotification(
                    notification.getReceiver(),
                    notification.getMessage(),
                    notification.getSender(),
                    notification.getType(),
                    notification.getLink(),
                    notification.getCategory(),
                    notification.getKind(),
                    notification.getSubject(),
                    notification.getEntityType(),
                    notification.getEntityId()
            );
        }

        return ResponseEntity.ok("Notifications sent");
    }

    // 🔁 DTO APPLIED
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnread(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(userId, page, size)
                        .stream()
                        .map(this::toDTO)
                        .toList()
        );
    }

    @PutMapping("/stared/{messageId}")
    public String stared(@PathVariable Long messageId) {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        notificationService.stardMessage(messageId, userId);
        return "done";
    }

    @PutMapping("/unStar/{messageId}")
    public String unStar(@PathVariable Long messageId) {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        notificationService.unstardMessage(messageId, userId);
        return "done";
    }

    @DeleteMapping("/delete/{messageId}")
    public String delete(@PathVariable Long messageId) {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        notificationService.deleteMessage(messageId, userId);
        return "done";
    }

    // 🔁 DTO APPLIED
    @GetMapping("/all")
    public ResponseEntity<List<NotificationDTO>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return ResponseEntity.ok(
                notificationService.getAllNotifications(userId, page, size)
                        .stream()
                        .map(this::toDTO)
                        .toList()
        );
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<String> markRead(@PathVariable Long id) {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok("Notification marked as read");
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return ResponseEntity.ok(
                notificationService.getUnreadCount(userId)
        );
    }

    @GetMapping("/isOnline")
    public boolean isOnline() {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return pushService.isOnline(userId);
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return pushService.subscribe(userId);
    }

    @GetMapping("/unSubscribe")
    public String unSubscribe() {

        String userId = SecurityUtil.getCurrentUserId()
                .map(String::valueOf)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return pushService.unSubscribe(userId);
    }

    // ADMIN / DEBUG ONLY
    @GetMapping("/deletedMessages")
    public List<Notification> getDeletedNotifications() {

        if (!SecurityUtil.hasRole("ADMIN")) {
            throw new RuntimeException("Forbidden: ADMIN only");
        }

        return notificationService.deletedMessage();
    }

    // ============================
    // 🔧 DTO MAPPER
    // ============================

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getReceiver(),
                n.getMessage(),
                n.getType(),
                n.isRead(),
                n.getSender(),
                n.getLink(),
                n.getCategory(),
                n.getKind(),
                n.isStared(),
                n.getSubject(),
                n.getDeleted(),
                n.getCreatedAt(),
                n.getEntityType(),
                n.getEntityId()
        );
    }
}
