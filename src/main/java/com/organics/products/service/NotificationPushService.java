
package com.organics.products.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationPushService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(1800000L);

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        log.info("SSE Connected for User: {}", userId);
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

    public void sendNotificationToUser(String userId, Object notification) {
        if ("ALL".equalsIgnoreCase(userId)) {
            emitters.forEach((id, userEmitters) -> sendToEmitters(id, userEmitters, notification));
        } else {
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters != null) {
                sendToEmitters(userId, userEmitters, notification);
            }
        }
    }

    private void sendToEmitters(String userId, List<SseEmitter> userEmitters, Object notification) {
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification)
                        .reconnectTime(5000));
            } catch (IOException e) {
                log.warn("Failed to send SSE to user {}, removing emitter", userId);
                removeEmitter(userId, emitter);
            }
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