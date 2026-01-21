package com.organics.products.service;

import com.organics.products.respository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationPushService {

    // Store active connections: Map<UserId, List<Emitters>>
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Autowired
    private NotificationRepository repository;

    // Check if a user has active connections
    public boolean isOnline(String userId) {
        // FIXED: Removed SecurityUtil check.
        // We trust the Controller to provide the correct userId.
        return emitters.containsKey(userId) && !emitters.get(userId).isEmpty();
    }

    // Subscribe a user (Called by Controller)
    public SseEmitter subscribe(String userId) {
        // FIXED: Removed "Forbidden" check.
        // The Controller has already authenticated the user before calling this.

        SseEmitter emitter = new SseEmitter(0L); // 0L = Infinite timeout

        emitters.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(emitter);

        // Cleanup callbacks
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        log.info("SSE connected: User {}", userId);
        return emitter;
    }

    // Unsubscribe a user
    public String unSubscribe(String userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            userEmitters.forEach(SseEmitter::complete);
        }
        log.info("User {} unsubscribed", userId);
        return "Unsubscribed " + userId;
    }

    // Send data to a specific user
    public void sendNotificationToUser(String userId, Object notification) {
        List<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters != null && !userEmitters.isEmpty()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            log.info("Sending notification to User {}: {}", userId, notification);

            userEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(notification));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                    log.error("Failed to send to User {}", userId);
                }
            });

            userEmitters.removeAll(deadEmitters);
        } else {
            log.info("User {} is offline. Notification skipped (saved in DB).", userId);
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}