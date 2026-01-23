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

@Component
public class EventListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationPushService notificationPushService; // For Web (SSE)

    @Autowired
    private PushNotificationService pushNotificationService; // For Mobile (Expo)

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            log.info("New message received from Redis: {}", message);

            Notification event = objectMapper.readValue(message.getBody(), Notification.class);

            // 1. Send to Web Dashboard (SSE)
            notificationPushService.sendNotificationToUser(event.getReceiver(), event);
            log.info("✅ SSE sent to receiver: {}", event.getReceiver());

            // 2. Send to Mobile App (Push)
            sendMobilePush(event);

        } catch (IOException e) {
            log.error("Error while parsing message or sending notification", e);
        }
    }

    private void sendMobilePush(Notification event) {
        try {
            // Only try to send push if receiver is a User ID (numeric)
            if (isNumeric(event.getReceiver())) {
                Long userId = Long.parseLong(event.getReceiver());
                User user = userRepository.findById(userId).orElse(null);

                if (user != null && user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty()) {
                    pushNotificationService.sendNotification(
                            user.getExpoPushToken(),
                            event.getSubject(),
                            event.getMessage()
                    );
                    log.info("✅ Mobile Push sent to User ID: {}", userId);
                } else {
                    log.debug("Skipping Push: User {} has no Expo Token", userId);
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