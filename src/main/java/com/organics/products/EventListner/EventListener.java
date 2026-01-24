package com.organics.products.EventListner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organics.products.entity.Notification;
import com.organics.products.entity.User;
import com.organics.products.respository.UserRepository;
import com.organics.products.service.NotificationPushService;
import com.organics.products.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class EventListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationPushService notificationPushService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("New message received from Redis: {}", message);

            Notification event = objectMapper.readValue(message.getBody(), Notification.class);

            // 1. Send to Web Dashboard (SSE) - Handled by updated NotificationPushService
            notificationPushService.sendNotificationToUser(event.getReceiver(), event);

            // 2. Send to Mobile App (Push)
            sendMobilePush(event);

        } catch (IOException e) {
            log.error("Error while parsing message or sending notification", e);
        }
    }

    private void sendMobilePush(Notification event) {
        try {
            // CASE 1: Broadcast to ALL Users
            if ("ALL".equalsIgnoreCase(event.getReceiver())) {
                log.info("Broadcasting Mobile Push to ALL users with tokens.");
                List<User> allUsers = userRepository.findAll(); // Optimization: use a custom query for users with tokens

                for (User user : allUsers) {
                    if (user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty()) {
                        pushNotificationService.sendNotification(
                                user.getExpoPushToken(),
                                event.getSubject(),
                                event.getMessage()
                        );
                    }
                }
            }
            // CASE 2: Single User
            else if (isNumeric(event.getReceiver())) {
                Long userId = Long.parseLong(event.getReceiver());
                User user = userRepository.findById(userId).orElse(null);

                if (user != null && user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty()) {
                    pushNotificationService.sendNotification(
                            user.getExpoPushToken(),
                            event.getSubject(),
                            event.getMessage()
                    );
                    log.info("✅ Mobile Push sent to User ID: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process Mobile Push inside EventListener", e);
        }
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }
}