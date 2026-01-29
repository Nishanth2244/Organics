//package com.organics.products.EventListner;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.organics.products.entity.Notification;
//import com.organics.products.respository.UserRepository;
//import com.organics.products.service.NotificationPushService;
//import com.organics.products.service.PushNotificationService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.connection.MessageListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.List;
//
//@Component
//@Slf4j
//public class EventListener implements MessageListener {
//
//    @Autowired private ObjectMapper objectMapper;
//    @Autowired private NotificationPushService notificationPushService; // For SSE
//    @Autowired private PushNotificationService pushNotificationService; // For Mobile Push
//    @Autowired private UserRepository userRepository;
//
//    @Override
//    public void onMessage(Message message, byte[] pattern) {
//        try {
//            Notification event = objectMapper.readValue(message.getBody(), Notification.class);
//
//            // 1. Send SSE (Web) - Handles "ALL" internally in NotificationPushService
//            notificationPushService.sendNotificationToUser(event.getReceiver(), event);
//
//            // 2. Send Mobile Push - Must be async to not block
//            handleMobilePushAsync(event);
//
//        } catch (IOException e) {
//            log.error("Error parsing notification", e);
//        }
//    }
//
//    @Async("notificationExecutor") // Run in background thread
//    public void handleMobilePushAsync(Notification event) {
//        try {
//            if ("ALL".equalsIgnoreCase(event.getReceiver())) {
//                // Fetch ALL tokens (Add this method to UserRepository if missing: findPushTokens())
//                // Or standard findAll loop if you don't have custom query yet
//                List<String> tokens = userRepository.findAllPushTokens();
//
//                for (String token : tokens) {
//                    try {
//                        pushNotificationService.sendNotification(token, event.getSubject(), event.getMessage());
//                    } catch (Exception e) {
//                        log.warn("Push failed for token: {}", token);
//                    }
//                }
//            } else if (isNumeric(event.getReceiver())) {
//                // Single User Push
//                Long userId = Long.parseLong(event.getReceiver());
//                userRepository.findById(userId).ifPresent(user -> {
//                    if (user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty()) {
//                        try {
//                            pushNotificationService.sendNotification(user.getExpoPushToken(), event.getSubject(), event.getMessage());
//                        } catch (Exception e) {
//                            log.error("Push failed for user {}", userId, e);
//                        }
//                    }
//                });
//            }
//        } catch (Exception e) {
//            log.error("Error in Mobile Push", e);
//        }
//    }
//
//    private boolean isNumeric(String str) {
//        return str != null && str.matches("\\d+");
//    }
//}