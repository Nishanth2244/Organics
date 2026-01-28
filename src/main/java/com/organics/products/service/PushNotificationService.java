package com.organics.products.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organics.products.entity.User;
import com.organics.products.respository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PushNotificationService {

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    public void sendNotification(String expoPushToken, String title, String body) {
        if (expoPushToken == null || expoPushToken.isEmpty()) {
            log.warn("Expo token is null or empty. Cannot send notification.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = objectMapper.writeValueAsString(
                    new ExpoPushMessage[]{ new ExpoPushMessage(expoPushToken, title, body) }
            );

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    EXPO_PUSH_URL, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Notification sent. Expo Response: {}", response.getBody());
            } else {
                log.error("Failed to send. Status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to send push notification to token: {}", expoPushToken, e);
        }
    }

    private static class ExpoPushMessage {
        public String to;
        public String title;
        public String body;
        public String sound = "default";

        public ExpoPushMessage(String to, String title, String body) {
            this.to = to;
            this.title = title;
            this.body = body;
        }
    }

    public void sendBroadcastNotification(String title, String body) {
        List<User> allUsers = userRepository.findAll();
        List<ExpoPushMessage> messages = allUsers.stream()
                .filter(user -> user.getExpoPushToken() != null && !user.getExpoPushToken().isEmpty())
                .map(user -> new ExpoPushMessage(user.getExpoPushToken(), title, body))
                .collect(Collectors.toList());

        if (messages.isEmpty()) return;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = objectMapper.writeValueAsString(messages);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(EXPO_PUSH_URL, HttpMethod.POST, entity, String.class);
            log.info("Broadcast sent to {} users", messages.size());
        } catch (Exception e) {
            log.error("Failed to send broadcast", e);
        }
    }
}