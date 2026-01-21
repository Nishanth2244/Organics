package com.organics.products.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organics.products.config.ShiprocketConfigProperties;
import com.organics.products.dto.ShiprocketTrackingResponse;
import com.organics.products.service.OrderService;
import com.organics.products.service.ShiprocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/shiprocket")
public class ShiprocketController {

    @Autowired
    private ShiprocketService shiprocketService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiprocketConfigProperties config; // Add this

    @Autowired
    private ObjectMapper objectMapper; // Add this

    @PostMapping("/setup-test")
    public ResponseEntity<Map<String, Object>> setupTest(@RequestBody Map<String, String> credentials) {
        try {
            // Test with provided credentials
            String email = credentials.get("email");
            String password = credentials.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Email and password are required"
                ));
            }

            // Test authentication
            WebClient testClient = WebClient.builder()
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("email", email);
            authRequest.put("password", password);

            Map<String, Object> response = testClient.post()
                    .uri("/auth/login")
                    .bodyValue(authRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            boolean success = response != null && response.containsKey("token");

            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "Credentials are valid!" : "Invalid credentials",
                    "has_token", response != null && response.containsKey("token"),
                    "response_keys", response != null ? response.keySet() : "No response"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

//    @GetMapping("/check-shiprocket-config")
//    public ResponseEntity<Map<String, Object>> checkShiprocketConfig() {
//        Map<String, Object> configInfo = new HashMap<>();
//
//        // Get all config values
//        configInfo.put("email", config.getEmail());
//        configInfo.put("apiToken_set", config.getApiToken() != null);
//        configInfo.put("apiToken_length", config.getApiToken() != null ? config.getApiToken().length() : 0);
//        configInfo.put("apiToken_preview", config.getApiToken() != null ?
//                config.getApiToken().substring(0, Math.min(8, config.getApiToken().length())) + "..." : "null");
//        configInfo.put("baseUrl", config.getBaseUrl());
//        configInfo.put("enableSandbox", config.isEnableSandbox());
//
//        // Check if properties are loaded correctly
//        configInfo.put("email_empty", config.getEmail() == null || config.getEmail().trim().isEmpty());
//        configInfo.put("apiToken_empty", config.getApiToken() == null || config.getApiToken().trim().isEmpty());
//
//        log.info("Shiprocket Config Check:");
//        log.info("  Email: {}", config.getEmail());
//        log.info("  API Token: {} chars", config.getApiToken() != null ? config.getApiToken().length() : 0);
//        log.info("  Base URL: {}", config.getBaseUrl());
//
//        return ResponseEntity.ok(configInfo);
//    }

//    @GetMapping("/verify-shiprocket")
//    public ResponseEntity<Map<String, Object>> verifyShiprocketSetup() {
//        try {
//            // Test 1: Check if credentials are configured
//            Map<String, Object> result = new HashMap<>();
//
//            String email = config.getEmail();
//            String password = config.getPassword() != null ?
//                    "***" + config.getPassword().substring(Math.max(0, config.getPassword().length() - 3)) :
//                    "NOT_SET";
//
//            result.put("email_configured", email != null && !email.isEmpty());
//            result.put("email", email);
//            result.put("password_configured", config.getPassword() != null && !config.getPassword().isEmpty());
//            result.put("password_masked", password);
//            result.put("base_url", config.getBaseUrl());
//
//            // Test 2: Try authentication
//            try {
//                HttpClient client = HttpClient.newHttpClient();
//
//                String requestBody = String.format(
//                        "{\"email\":\"%s\",\"password\":\"%s\"}",
//                        email,
//                        config.getPassword()
//                );
//
//                HttpRequest request = HttpRequest.newBuilder()
//                        .uri(URI.create(config.getBaseUrl() + "/auth/login"))
//                        .header("Content-Type", "application/json")
//                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//                        .build();
//
//                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//                result.put("auth_test_status", response.statusCode());
//                result.put("auth_test_response", response.body());
//
//                if (response.statusCode() == 200) {
//                    // Parse response to get token
//                    Map<String, Object> authResponse = objectMapper.readValue(response.body(), Map.class);
//                    result.put("token_received", authResponse.containsKey("token"));
//                    result.put("response_keys", authResponse.keySet());
//                }
//
//            } catch (Exception e) {
//                result.put("auth_test_error", e.getMessage());
//            }
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Map.of(
//                    "error", e.getMessage()
//            ));
//        }
//    }
//
//    @GetMapping("/debug-auth")
//    public ResponseEntity<Map<String, Object>> debugAuthentication() {
//        try {
//            String email = config.getEmail();
//            String password = config.getPassword();
//
//            log.info("Testing Shiprocket authentication for: {}", email);
//
//            // Create a simple HTTP client for debugging
//            HttpClient client = HttpClient.newHttpClient();
//
//            String jsonBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}",
//                    email, password);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("https://apiv2.shiprocket.in/v1/external/auth/login"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                    .build();
//
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("statusCode", response.statusCode());
//            result.put("responseBody", response.body());
//            result.put("email", email);
//            result.put("url", "https://apiv2.shiprocket.in/v1/external/auth/login");
//
//            log.info("Debug response - Status: {}, Body: {}", response.statusCode(), response.body());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            log.error("Debug failed: {}", e.getMessage());
//            return ResponseEntity.badRequest().body(Map.of(
//                    "error", e.getMessage(),
//                    "stackTrace", Arrays.toString(e.getStackTrace())
//            ));
//        }
//    }
//
//    @GetMapping("/test-auth")
//    public ResponseEntity<Map<String, Object>> testAuthentication() {
//        try {
//            Map<String, Object> authTest = shiprocketService.testAuthentication();
//            return ResponseEntity.ok(authTest);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "error", e.getMessage()
//            ));
//        }
//    }



    @PostMapping("/order/{orderId}/create")
    public ResponseEntity<Map<String, Object>> createShiprocketOrder(@PathVariable Long orderId) {
        try {
            log.info("Manually creating Shiprocket order for order ID: {}", orderId);
            return ResponseEntity.ok(Map.of("message", "Shiprocket order creation initiated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/order/{orderId}/label")
    public ResponseEntity<byte[]> getShippingLabel(@PathVariable Long orderId) {
        try {
            byte[] label = orderService.getShippingLabel(orderId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"shipping_label_" + orderId + ".pdf\"")
                    .body(label);
        } catch (Exception e) {
            log.error("Error getting shipping label: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/order/{orderId}/track")
    public ResponseEntity<ShiprocketTrackingResponse> trackOrder(@PathVariable Long orderId) {
        try {
            ShiprocketTrackingResponse trackingInfo = orderService.trackOrder(orderId);
            return ResponseEntity.ok(trackingInfo);
        } catch (Exception e) {
            log.error("Error tracking order: {}", e.getMessage());
            ShiprocketTrackingResponse errorResponse = new ShiprocketTrackingResponse();
            //errorResponse.setMessage("Failed to track order: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

//    @GetMapping("/rates")
//    public ResponseEntity<List<Map<String, Object>>> getShippingRates(
//            @RequestParam String pickupPincode,
//            @RequestParam String deliveryPincode,
//            @RequestParam(required = false) Double weight,
//            @RequestParam(required = false) Integer length,
//            @RequestParam(required = false) Integer breadth,
//            @RequestParam(required = false) Integer height) {
//
//        try {
//            List<Map<String, Object>> rates = shiprocketService.getShippingRates(
//                    pickupPincode, deliveryPincode, weight, length, breadth, height);
//            return ResponseEntity.ok(rates);
//        } catch (Exception e) {
//            log.error("Error getting shipping rates: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//    // Add to ShiprocketController.java
//    @GetMapping("/check-config")
//    public ResponseEntity<Map<String, Object>> checkConfig() {
//        Map<String, Object> configInfo = new HashMap<>();
//
//        // Check what's configured
//        configInfo.put("email", config.getEmail());
//        configInfo.put("email_empty", config.getEmail() == null || config.getEmail().isEmpty());
//        configInfo.put("password_empty", config.getPassword() == null || config.getPassword().isEmpty());
//        configInfo.put("password_length", config.getPassword() != null ? config.getPassword().length() : 0);
//        configInfo.put("base_url", config.getBaseUrl());
//
//        // Mask password for logging
//        String maskedPassword = config.getPassword() != null ?
//                "***" + config.getPassword().substring(Math.max(0, config.getPassword().length() - 3)) :
//                "NULL";
//        configInfo.put("password_masked", maskedPassword);
//
//        log.info("Shiprocket Config - Email: {}, Password: {}, Base URL: {}",
//                config.getEmail(), maskedPassword, config.getBaseUrl());
//
//        return ResponseEntity.ok(configInfo);
//    }
//    // Add to ShiprocketController.java
//    @GetMapping("/current-config")
//    public ResponseEntity<Map<String, Object>> getCurrentConfig() {
//        Map<String, Object> configInfo = new HashMap<>();
//
//        // Get current email (mask for security)
//        String email = config.getEmail();
//        String maskedEmail = email != null ?
//                email.substring(0, Math.min(3, email.length())) + "***@" +
//                        email.substring(email.indexOf('@') + 1) : "null";
//
//        configInfo.put("email", maskedEmail);
//        configInfo.put("email_full", email);
//        configInfo.put("password_set", config.getPassword() != null && !config.getPassword().isEmpty());
//        configInfo.put("password_length", config.getPassword() != null ? config.getPassword().length() : 0);
//        configInfo.put("base_url", config.getBaseUrl());
//
//        // Check if email looks valid
//        if (email != null) {
//            configInfo.put("email_valid_format", email.contains("@") && email.contains("."));
//            configInfo.put("email_domain", email.substring(email.indexOf('@')));
//        }
//
//        log.info("Current Shiprocket config - Email: {}, Base URL: {}", maskedEmail, config.getBaseUrl());
//
//        return ResponseEntity.ok(configInfo);
//    }


    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        try {
            log.info("Received Shiprocket webhook: {}", webhookData);

            // Handle different webhook events
            String event = (String) webhookData.get("event");

            switch (event) {
                case "shipment_status":
                    handleShipmentStatus(webhookData);
                    break;
                case "order_delivered":
                    handleOrderDelivered(webhookData);
                    break;
                case "order_cancelled":
                    handleOrderCancelled(webhookData);
                    break;
                default:
                    log.warn("Unknown webhook event: {}", event);
            }

            return ResponseEntity.ok("Webhook received successfully");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error processing webhook");
        }
    }



    @GetMapping("/ip-check")
    public Map<String, Object> checkIp() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get public IP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            result.put("public_ip", response.body());
            result.put("local_hostname", InetAddress.getLocalHost().getHostName());
            result.put("local_ip", InetAddress.getLocalHost().getHostAddress());
            result.put("timestamp", new Date());

            // Instructions for Shiprocket
            result.put("shiprocket_instructions",
                    "Add this IP to Shiprocket:\n" +
                            "1. Login to https://app.shiprocket.in/\n" +
                            "2. Settings → API Credentials\n" +
                            "3. Find 'Allowed IPs for PII Access'\n" +
                            "4. Add IP: " + response.body() + "\n" +
                            "5. Also add: 127.0.0.1 (for local testing)\n" +
                            "6. Save and wait 5 minutes");

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    @GetMapping("/test-connection-direct")
    public Map<String, Object> testConnectionDirect() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get your server IP
            HttpClient ipClient = HttpClient.newHttpClient();
            HttpRequest ipRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> ipResponse = ipClient.send(ipRequest, HttpResponse.BodyHandlers.ofString());
            String serverIp = ipResponse.body();

            result.put("server_ip", serverIp);
            result.put("timestamp", LocalDateTime.now().toString());

            // Test Shiprocket authentication DIRECTLY
            String email = "ramyareddy2332@gmail.com";
            String password = "*cLAZY5@@3I4@#iA3sAu@I8WD1zo%DNV"; // Your password

            String requestBody = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\"}",
                    email, password
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://apiv2.shiprocket.in/v1/external/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            result.put("status_code", response.statusCode());
            result.put("response_type", response.body().contains("<html>") ? "HTML" : "JSON");
            result.put("response_length", response.body().length());
            result.put("has_token", response.body().contains("token"));

            // Detailed analysis
            if (response.statusCode() == 403) {
                result.put("error_analysis",
                        "⚠️ 403 FORBIDDEN - IP NOT WHITELISTED OR API ACCESS DISABLED ⚠️\n\n" +
                                "Server IP: " + serverIp + "\n\n" +
                                "IMMEDIATE ACTIONS:\n" +
                                "1. Add this EXACT IP to Shiprocket: " + serverIp + "\n" +
                                "2. Also add: 127.0.0.1\n" +
                                "3. Click SAVE (not just type and close)\n" +
                                "4. Wait 15 minutes\n" +
                                "5. Contact support@shiprocket.in if still failing\n\n" +
                                "Response preview: " + response.body().substring(0, Math.min(200, response.body().length()))
                );
            } else if (response.statusCode() == 200) {
                result.put("success", true);
                result.put("message", "Authentication successful!");
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
        }

        return result;
    }

    @PostMapping("/test-auth-manual")
    public Map<String, Object> testAuthManual(@RequestBody Map<String, String> credentials) {
        Map<String, Object> result = new HashMap<>();

        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            if (email == null || password == null) {
                result.put("error", "Email and password required");
                return result;
            }

            // Create WebClient for testing
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://apiv2.shiprocket.in/v1/external")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("User-Agent", "OrganicProducts-Test/1.0")
                    .build();

            Map<String, String> request = new HashMap<>();
            request.put("email", email);
            request.put("password", password);

            String response = webClient.post()
                    .uri("/auth/login")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "HTTP " + clientResponse.statusCode().value() + ": " + body)));
                    })
                    .bodyToMono(String.class)
                    .block();

            result.put("response", response);
            result.put("is_html", response.contains("<html>"));
            result.put("has_token", response.contains("token"));
            result.put("status", response.contains("<html>") ? "BLOCKED" : "SUCCESS");

            if (response.contains("<html>")) {
                result.put("instructions",
                        "1. Login to Shiprocket Dashboard\n" +
                                "2. Go to Settings → API Credentials\n" +
                                "3. Make sure 'API Access' is ENABLED\n" +
                                "4. Add your server IP to 'Allowed IPs for PII Access'\n" +
                                "5. Save and wait 15 minutes\n" +
                                "6. Email support@shiprocket.in if issue persists"
                );
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
        }

        return result;
    }

    private void handleShipmentStatus(Map<String, Object> data) {
        // Update order status based on shipment status
        String awbCode = (String) data.get("awb_code");
        String status = (String) data.get("status");

        log.info("Shipment status update - AWB: {}, Status: {}", awbCode, status);

        // Find order by AWB code and update status
        // You'll need to implement this based on your database structure
    }

    private void handleOrderDelivered(Map<String, Object> data) {
        String awbCode = (String) data.get("awb_code");
        log.info("Order delivered - AWB: {}", awbCode);

        // Update order status to DELIVERED
    }

    private void handleOrderCancelled(Map<String, Object> data) {
        String awbCode = (String) data.get("awb_code");
        log.info("Order cancelled - AWB: {}", awbCode);

        // Update order status to CANCELLED
    }
}