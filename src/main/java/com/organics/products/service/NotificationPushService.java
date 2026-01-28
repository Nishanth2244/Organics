package com.organics.products.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationPushService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(userId, k -> new ArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        log.info("SSE Connected: User {}", userId);
        return emitter;
    }

    public void unSubscribe(String userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            userEmitters.forEach(SseEmitter::complete);
        }
    }

    public boolean isOnline(String userId) {
        return emitters.containsKey(userId) && !emitters.get(userId).isEmpty();
    }

    // UPDATED: Handles "ALL" to broadcast to everyone
    public void sendNotificationToUser(String userId, Object notification) {
        if ("ALL".equalsIgnoreCase(userId)) {
            // Broadcast to ALL connected users
            emitters.forEach((id, userEmitters) -> sendToEmitters(id, userEmitters, notification));
        } else {
            // Send to specific user
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters != null) {
                sendToEmitters(userId, userEmitters, notification);
            }
        }
    }

    private void sendToEmitters(String userId, List<SseEmitter> userEmitters, Object notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        synchronized (userEmitters) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(notification));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            userEmitters.removeAll(deadEmitters);
        }
        if (userEmitters.isEmpty()) emitters.remove(userId);
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            synchronized (userEmitters) {
                userEmitters.remove(emitter);
                if (userEmitters.isEmpty()) emitters.remove(userId);
            }
        }
    }
}