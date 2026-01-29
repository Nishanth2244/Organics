////package com.organics.products.service;
////
////
////
////import com.organics.products.entity.Notification;
////import com.organics.products.respository.UserRepository;
////import lombok.extern.slf4j.Slf4j;
////
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.data.redis.core.RedisTemplate;
////import org.springframework.stereotype.Service;
////
////import java.util.concurrent.CompletableFuture;
////
////@Service
////@Slf4j
////public class NotificationProducer {
////
////    @Autowired
////    private FcmService fcmService;        // <--- Inject FCM Service
////
////    @Autowired
////    private UserRepository userRepository;
////
////    @Autowired
////    private RedisTemplate redisTemplate;
////
////    public void sendNotification(Notification notification) {
//////        String topic = "notifications";
//////        String key = notification.getReceiver();
////        log.info("Notification received and sending to redis: {}", notification);
////
//////        CompletableFuture<SendResult<String, Notification>> future = kafkaTemplate.send(topic, key, notification);
////        redisTemplate.convertAndSend("notifications:all", notification);
//////        future.whenComplete((result, ex) -> {
//////            if (ex != null) {
//////                System.err.printf(" Failed to send to %s: %s%n", key, ex.getMessage());
//////            } else {
//////                RecordMetadata metadata = result.getRecordMetadata();
//////                System.out.printf("Notification sent to %s | Partition: %d | Offset: %d%n",
//////                        key, metadata.partition(), metadata.offset());
//////            }
//////        });
////    }
////}
////package com.organics.products.service;
////
////import com.organics.products.entity.User;
////import com.organics.products.respository.UserRepository;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.data.redis.core.RedisTemplate;
////import org.springframework.stereotype.Service;
////
////@Service
////@Slf4j
////public class NotificationProducer {
////
////    @Autowired
////    private RedisTemplate<String, Object> redisTemplate;
////
////
////    @Autowired
////    private UserRepository userRepository;
////    public void sendNotification(com.organics.products.entity.Notification notification) {
////        // 1. Existing Logic: Send to Web Dashboard (Redis -> SSE)
////        log.info("Publishing notification to Redis for receiver: {}", notification.getReceiver());
////        redisTemplate.convertAndSend("notifications:all", notification);
////
////        // 2. New Logic: Send to Mobile App (FCM)
////        try {
////            // Check if receiver is a numeric User ID (skip if "ADMIN" or system)
////            if (isNumeric(notification.getReceiver())) {
////                Long userId = Long.parseLong(notification.getReceiver());
////
////                // Fetch user to get the stored FCM token
////                User user = userRepository.findById(userId).orElse(null);
////
////                if (user != null && user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty()) {
////                    log.info("Sending Mobile Push to User: {}", userId);
////                    fcmService.sendPushNotification(
////                            user.getFcmToken(),
////                            notification.getSubject(), // Title (e.g., "Order Placed")
////                            notification.getMessage()  // Body
////                    );
////                }
////            }
////        } catch (Exception e) {
////            log.error("Failed to process FCM push: {}", e.getMessage());
////        }
////    }
////
////    private boolean isNumeric(String str) {
////        return str != null && str.matches("\\d+");
////    }
////}
//
//package com.organics.products.service;
//
//import com.organics.products.entity.Notification;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//@Slf4j
//public class NotificationProducer {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    public void sendNotification(Notification notification) {
//        // 1. Existing Logic: Send to Web Dashboard (Redis -> SSE)
//        log.info("Publishing notification to Redis for receiver: {}", notification.getReceiver());
//        redisTemplate.convertAndSend("notifications:all", notification);
//
//        // NOTE: Mobile Push Logic (Expo) has been moved to NotificationService.java
//        // to consolidate user lookup and token handling.
//    }
//}
//
//package com.organics.products.service;
//
//import com.organics.products.entity.Notification;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//@Slf4j
//public class NotificationProducer {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    public void sendNotification(Notification notification) {
//        log.info("Publishing notification to Redis for receiver: {}", notification.getReceiver());
//        // Triggers the EventListener
//        redisTemplate.convertAndSend("notifications:all", notification);
//    }
//}
