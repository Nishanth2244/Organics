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

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Autowired
    private NotificationRepository repository;

    public boolean isOnline(String userId) {
        return emitters.containsKey(userId) && !emitters.get(userId).isEmpty();
    }

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        log.info("SSE connected: User {}", userId);
        return emitter;
    }

    public void unSubscribe(String userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            userEmitters.forEach(SseEmitter::complete);
            log.info("User {} unsubscribed", userId);
        }
    }

    // UPDATED: Handle "ALL" for Broadcast
    public void sendNotificationToUser(String userId, Object notification) {

        if ("ALL".equalsIgnoreCase(userId)) {
            log.info("Broadcasting notification to ALL online users.");
            emitters.forEach((id, userEmitters) -> sendToEmitters(id, userEmitters, notification));
            return;
        }

        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null && !userEmitters.isEmpty()) {
            sendToEmitters(userId, userEmitters, notification);
        } else {
            log.info("User {} is offline. Notification skipped (saved in DB).", userId);
        }
    }

    // Helper method to reuse sending logic
    private void sendToEmitters(String userId, List<SseEmitter> userEmitters, Object notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        synchronized (userEmitters) { // Safe iteration
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