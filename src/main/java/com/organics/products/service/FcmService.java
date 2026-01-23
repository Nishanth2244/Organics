package com.organics.products.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmService {

    public void sendPushNotification(String token, String title, String body) {
        if (token == null || token.isEmpty()) {
            log.warn("Cannot send FCM notification: Token is null or empty");
            return;
        }

        try {
            // Build the message
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    // Optional: Add data for deep linking in app
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK") 
                    .build();

            // Send via Firebase
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ FCM Notification sent successfully: {}", response);
            
        } catch (Exception e) {
            log.error("❌ Error sending FCM notification: {}", e.getMessage());
        }
    }
}