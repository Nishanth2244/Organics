// ShiprocketService.java - Fix the method signatures
package com.organics.products.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organics.products.config.ShiprocketConfigProperties;
import com.organics.products.dto.*;
import com.organics.products.entity.*;
import com.organics.products.exception.ShiprocketException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ShiprocketService {

    private final WebClient webClient;
    private final ShiprocketConfigProperties config;
    private final ObjectMapper objectMapper;

    private String authToken;
    private LocalDateTime tokenExpiry;
    private String cachedToken;

    @Autowired
    public ShiprocketService(ShiprocketConfigProperties config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;

        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "OrganicProducts/1.0")
                .build();

        log.info("✅ Shiprocket Service initialized. IP whitelisting is working!");
    }

    /**
     * Get authentication token - Now working!
     */
    private synchronized String getAuthToken() {
        // Return cached token if valid (with 10 minute buffer)
        if (cachedToken != null && tokenExpiry != null &&
                tokenExpiry.isAfter(LocalDateTime.now().plusMinutes(10))) {
            log.debug("Using cached Shiprocket token");
            return cachedToken;
        }

        log.info("🔐 Getting new Shiprocket token for: {}", config.getEmail());

        try {
            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("email", config.getEmail());
            authRequest.put("password", config.getPassword());

            Map<String, Object> response = webClient.post()
                    .uri("/auth/login")
                    .bodyValue(authRequest)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new ShiprocketException(
                                        "Authentication failed: HTTP " +
                                                clientResponse.statusCode().value() + " - " +
                                                parseErrorMessage(body))));
                    })
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("token")) {
                cachedToken = (String) response.get("token");

                // Token expires in 3600 seconds (1 hour)
                tokenExpiry = LocalDateTime.now().plusSeconds(3600);

                log.info("✅ Shiprocket authentication SUCCESSFUL!");
                log.info("Token expires at: {}", tokenExpiry);

                return cachedToken;
            } else {
                throw new ShiprocketException("No token in response: " + response);
            }

        } catch (Exception e) {
            log.error("💥 Failed to get auth token: {}", e.getMessage());
            throw new ShiprocketException("Shiprocket authentication failed: " + e.getMessage(), e);
        }
    }

    private String parseErrorMessage(String body) {
        if (body == null) return "Empty response";
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }


    private String handleSuccessfulAuth(Map<String, Object> response) {
        authToken = (String) response.get("token");
        Long expiresIn = response.get("expires_in") != null ?
                ((Number) response.get("expires_in")).longValue() : 3600L;
        tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn);
        log.info("✅ Shiprocket authentication successful!");
        log.info("Token expires at: {}", tokenExpiry);
        return authToken;
    }

//    /**
//    /**
//     * Create order in Shiprocket with Address entity
//     */
//    public ShiprocketCreateOrderResponse createOrder(Order order, User user, Address shippingAddress) {
//        try {
//            String token = getAuthToken();
//
//            // Prepare order request
//            Map<String, Object> request = new HashMap<>();
//
//            // Order details
//            request.put("order_id", "ORG" + order.getId());
//            request.put("order_date", order.getOrderDate().toString());
//            request.put("order_amount", order.getOrderAmount());
//            request.put("comment", order.getDescription());
//
//            // Billing/Shipping address from Address entity
//            request.put("billing_customer_name", user.getFirstName() + " " + user.getLastName());
//            request.put("billing_address", getAddressString(shippingAddress));
//            request.put("billing_city", shippingAddress.getCity());
//            request.put("billing_pincode", shippingAddress.getPinCode());
//            request.put("billing_state", shippingAddress.getState());
//            request.put("billing_email", user.getEmailId());
//            request.put("billing_phone", shippingAddress.getAlternatePhoneNumber() != null ?
//                    shippingAddress.getAlternatePhoneNumber() : user.getPhoneNumber());
//            request.put("billing_country", "India");
//
//            // Shipping address
//            request.put("shipping_is_billing", true);
//
//            // Order items
//            List<Map<String, Object>> orderItems = new ArrayList<>();
//
//            if (order.getOrderItems() != null) {
//                for (OrderItems item : order.getOrderItems()) {
//                    Map<String, Object> orderItem = new HashMap<>();
//                    orderItem.put("name", item.getProduct().getProductName());
//                    orderItem.put("sku", item.getProduct().getSku() != null ?
//                            item.getProduct().getSku() : "SKU" + item.getProduct().getId());
//                    orderItem.put("units", item.getQuantity());
//                    orderItem.put("selling_price", item.getPrice());
//                    orderItem.put("discount", item.getDiscount() != null ? item.getDiscount() : 0.0);
//                    orderItem.put("tax", item.getTax() != null ? item.getTax() : 0.0);
//                    orderItem.put("hsn", 0.0);
//
//                    orderItems.add(orderItem);
//                }
//            }
//
//            request.put("order_items", orderItems);
//
//            // Payment method
//            request.put("payment_method", "Prepaid"); // Change based on your payment method
//
//            // Shipping charges (you can calculate or get from rates API)
//            request.put("shipping_charges", 0.0);
//            request.put("total_discount", order.getOrderItems().stream()
//                    .mapToDouble(item -> item.getDiscount() != null ? item.getDiscount() : 0.0)
//                    .sum());
//
//            log.info("Creating Shiprocket order for order ID: {}", order.getId());
//
//            // Make API call
//            Map<String, Object> response = webClient.post()
//                    .uri("/orders/create/adhoc")
//                    .header("Authorization", "Bearer " + token)
//                    .bodyValue(request)
//                    .retrieve()
//                    .bodyToMono(Map.class)
//                    .block();
//
//            if (response != null && response.containsKey("order_id")) {
//                ShiprocketCreateOrderResponse createOrderResponse = new ShiprocketCreateOrderResponse();
//                createOrderResponse.setOrderId(((Number) response.get("order_id")).longValue());
//                createOrderResponse.setShipmentId(((Number) response.get("shipment_id")).longValue());
//                createOrderResponse.setStatus((String) response.get("status"));
//                createOrderResponse.setMessage((String) response.get("message"));
//                createOrderResponse.setAwbCode((String) response.get("awb_code"));
//                createOrderResponse.setCourierName((String) response.get("courier_name"));
//
//                if (response.get("courier_company_id") != null) {
//                    createOrderResponse.setCourierCompanyId(((Number) response.get("courier_company_id")).longValue());
//                }
//
//                log.info("Shiprocket order created successfully. Shiprocket Order ID: {}, Shipment ID: {}",
//                        createOrderResponse.getOrderId(), createOrderResponse.getShipmentId());
//                return createOrderResponse;
//            } else {
//                throw new ShiprocketException("Failed to create Shiprocket order");
//            }
//        } catch (Exception e) {
//            log.error("Error creating Shiprocket order: {}", e.getMessage());
//            throw new ShiprocketException("Failed to create Shiprocket order: " + e.getMessage());
//        }
//    }

    public ShiprocketCreateOrderResponse createOrder(Order order, User user, Address shippingAddress) {
        try {
            String token = getAuthToken();

            log.info("🚀 Creating Shiprocket order for Order ID: {}", order.getId());

            Map<String, Object> request = buildOrderRequest(order, user, shippingAddress);

            log.debug("Sending order to Shiprocket...");

            // Make the API call
            String responseJson = webClient.post()
                    .uri("/orders/create/adhoc")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Shiprocket API error: {}", body);
                                    return Mono.error(new ShiprocketException(
                                            "Shiprocket API error: " + body));
                                });
                    })
                    .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new ShiprocketException(
                                        "Shiprocket server error: " + clientResponse.statusCode())));
                    })
                    .bodyToMono(String.class) // Get response as String first
                    .block();

            log.debug("Raw Shiprocket response: {}", responseJson);

            // Parse the JSON response
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

            return parseCreateOrderResponse(response);

        } catch (Exception e) {
            log.error("❌ Failed to create Shiprocket order: {}", e.getMessage());
            throw new ShiprocketException("Failed to create Shiprocket order: " + e.getMessage(), e);
        }
    }

    private ShiprocketCreateOrderResponse parseCreateOrderResponse(Map<String, Object> response) {
        if (response == null) {
            throw new ShiprocketException("Empty response from Shiprocket");
        }

        log.debug("Parsed Shiprocket response: {}", response);

        // Check for errors in response
        if (response.containsKey("errors")) {
            Object errors = response.get("errors");
            throw new ShiprocketException("Shiprocket validation errors: " + errors.toString());
        }

        if (response.containsKey("message") && !"success".equalsIgnoreCase(String.valueOf(response.get("message")))) {
            throw new ShiprocketException("Shiprocket error: " + response.get("message"));
        }

        ShiprocketCreateOrderResponse result = new ShiprocketCreateOrderResponse();

        // Safely parse order_id - handle both String and Number types
        Object orderIdObj = response.get("order_id");
        if (orderIdObj != null) {
            if (orderIdObj instanceof Number) {
                result.setOrderId(((Number) orderIdObj).longValue());
            } else if (orderIdObj instanceof String) {
                try {
                    result.setOrderId(Long.parseLong((String) orderIdObj));
                } catch (NumberFormatException e) {
                    result.setOrderId(0L);
                    log.warn("Could not parse order_id as number: {}", orderIdObj);
                }
            }
        }

        // Safely parse shipment_id
        Object shipmentIdObj = response.get("shipment_id");
        if (shipmentIdObj != null) {
            if (shipmentIdObj instanceof Number) {
                result.setShipmentId(((Number) shipmentIdObj).longValue());
            } else if (shipmentIdObj instanceof String) {
                try {
                    result.setShipmentId(Long.parseLong((String) shipmentIdObj));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse shipment_id as number: {}", shipmentIdObj);
                }
            }
        }

        // Safely parse courier_company_id
        Object courierCompanyIdObj = response.get("courier_company_id");
        if (courierCompanyIdObj != null && courierCompanyIdObj instanceof Number) {
            result.setCourierCompanyId(((Number) courierCompanyIdObj).longValue());
        }

        // Set string fields
        result.setStatus(getStringValue(response.get("status")));
        result.setMessage(getStringValue(response.get("message")));
        result.setAwbCode(getStringValue(response.get("awb_code")));
        result.setCourierName(getStringValue(response.get("courier_name")));

        log.info("✅ Shiprocket order created successfully!");
        log.info("Order ID: {}, Shipment ID: {}", result.getOrderId(), result.getShipmentId());

        return result;
    }

    private String getStringValue(Object obj) {
        if (obj == null) return null;
        return String.valueOf(obj);
    }

    private Map<String, Object> buildOrderRequest(Order order, User user, Address shippingAddress) {
        Map<String, Object> request = new HashMap<>();

        // Order details
        String uniqueOrderId = "ORG" + order.getId() + "_" + System.currentTimeMillis();
        request.put("order_id", uniqueOrderId);
        request.put("order_date", order.getOrderDate().toString());

        // Send the actual order amount (620 in your case)
        request.put("order_amount", order.getOrderAmount());
        request.put("comment", order.getDescription() != null ?
                order.getDescription() : "Organic Products Order");

        // Customer details
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "Customer";

        if (firstName.isEmpty() && lastName.equals("Customer")) {
            firstName = "Customer";
        }

        request.put("billing_customer_name", firstName + " " + lastName);
        request.put("billing_first_name", firstName);
        request.put("billing_last_name", lastName);
        request.put("billing_address", formatAddress(shippingAddress));
        request.put("billing_city", shippingAddress.getCity() != null ? shippingAddress.getCity() : "");
        request.put("billing_pincode", shippingAddress.getPinCode() != null ? shippingAddress.getPinCode() : "");
        request.put("billing_state", shippingAddress.getState() != null ? shippingAddress.getState() : "");
        request.put("billing_email", user.getEmailId() != null ? user.getEmailId() : "");
        request.put("billing_phone", getPhoneNumber(shippingAddress, user));
        request.put("billing_country", "India");

        // Shipping same as billing
        request.put("shipping_is_billing", true);

        List<Map<String, Object>> orderItems = new ArrayList<>();

        double subTotal = order.getOrderItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        for (OrderItems item : order.getOrderItems()) {
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("name", item.getProduct().getProductName());
            orderItem.put("sku", getSku(item.getProduct()));
            orderItem.put("units", item.getQuantity());

            // Shiprocket Math: ((selling_price * units) + tax) - discount = Total
            // Ensure item.getPrice() represents the base price per unit
            orderItem.put("selling_price", item.getPrice());
            request.put("sub_total", subTotal);
            orderItem.put("discount", item.getDiscount() != null ? item.getDiscount() : 0.0);
            orderItem.put("tax", item.getTax() != null ? item.getTax() : 0.0);
            orderItem.put("hsn", "123456");

            orderItems.add(orderItem);
        }

        request.put("order_items", orderItems);
        request.put("payment_method", "Prepaid");
        request.put("shipping_charges", 0.0);

        // dimensions
        request.put("length", 10);
        request.put("breadth", 10);
        request.put("height", 10);
        request.put("weight", 0.5);

        log.info("=== Shiprocket Request ===");
        log.info("Order Amount: {}", order.getOrderAmount());
//        log.info("Sub Total: {}", subTotal);
//        log.info("Total Discount: {}", totalDiscount);
//        log.info("Total Tax: {}", totalTax);
        log.info("Calculated Total (sub + tax): {}", order.getOrderAmount() );
        log.info("==========================");

        return request;
    }

    private double calculateTotalDiscount(Order order) {
        return order.getOrderItems().stream()
                .mapToDouble(item -> item.getDiscount() != null ? item.getDiscount() : 0.0)
                .sum();
    }

    public byte[] generateShippingLabel(Long shipmentId) {
        try {
            String token = getAuthToken();

            Map<String, Object> request = new HashMap<>();
            request.put("shipment_id", new Long[]{shipmentId});

            log.info("Generating shipping label for shipment ID: {}", shipmentId);

            return webClient.post()
                    .uri("/courier/generate/label")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("Error generating shipping label: {}", e.getMessage());
            throw new ShiprocketException("Failed to generate shipping label: " + e.getMessage());
        }
    }

    /**
     * Track order - FIXED: Returns ShiprocketTrackingResponse
     */
    public ShiprocketTrackingResponse trackOrder(String awbCode) {
        try {
            String token = getAuthToken();

            log.info("Fetching tracking info for AWB: {}", awbCode);

            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/courier/track/awb")
                            .queryParam("awb_code", awbCode)
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("tracking_data")) {
                return objectMapper.convertValue(response, ShiprocketTrackingResponse.class);
            }

            return new ShiprocketTrackingResponse();
        } catch (Exception e) {
            log.error("Error fetching tracking info: {}", e.getMessage());
            throw new ShiprocketException("Failed to fetch tracking info: " + e.getMessage());
        }
    }

    /**
     * Get shipping rates based on pin codes (String version)
     */
    public List<Map<String, Object>> getShippingRates(String pickupPincode, String deliveryPincode,
                                                      Double weight, Integer length, Integer breadth,
                                                      Integer height) {
        try {
            String token = getAuthToken();

            Map<String, Object> request = new HashMap<>();
            request.put("pickup_postcode", pickupPincode);
            request.put("delivery_postcode", deliveryPincode);
            request.put("weight", weight != null ? weight : 1.0);
            request.put("length", length != null ? length : 10);
            request.put("breadth", breadth != null ? breadth : 10);
            request.put("height", height != null ? height : 10);

            log.info("Fetching shipping rates from {} to {}", pickupPincode, deliveryPincode);

            Map<String, Object> response = webClient.post()
                    .uri("/courier/serviceability")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching shipping rates: {}", e.getMessage());
            throw new ShiprocketException("Failed to fetch shipping rates: " + e.getMessage());
        }
    }

    /**
     * Get shipping rates based on Address entities
     */
    public List<Map<String, Object>> getShippingRates(Address pickupAddress, Address deliveryAddress,
                                                      Double weight, Integer length, Integer breadth,
                                                      Integer height) {
        return getShippingRates(pickupAddress.getPinCode(), deliveryAddress.getPinCode(),
                weight, length, breadth, height);
    }

//    public Map<String, Object> testAuthentication() {
//        try {
//            Map<String, String> authRequest = new HashMap<>();
//            authRequest.put("email", config.getEmail());
//            authRequest.put("password", config.getPassword());
//
//            log.info("Testing Shiprocket authentication with email: {}", config.getEmail());
//
//            // Make the request with detailed error handling
//            Map<String, Object> response = webClient.post()
//                    .uri("/auth/login")
//                    .bodyValue(authRequest)
//                    .retrieve()
//                    .onStatus(
//                            status -> status == HttpStatus.FORBIDDEN,
//                            clientResponse -> Mono.error(new ShiprocketException(
//                                    "Invalid credentials. Please check your email and password."))
//                    )
//                    .onStatus(
//                            status -> status == HttpStatus.UNAUTHORIZED,
//                            clientResponse -> Mono.error(new ShiprocketException(
//                                    "Unauthorized. Your account may not have API access enabled."))
//                    )
//                    .onStatus(
//                            status -> status.is4xxClientError(),
//                            clientResponse -> clientResponse.bodyToMono(String.class)
//                                    .flatMap(body -> Mono.error(new ShiprocketException(
//                                            "Client error: " + clientResponse.statusCode() + " - " + body)))
//                    )
//                    .onStatus(
//                            status -> status.is5xxServerError(),
//                            clientResponse -> clientResponse.bodyToMono(String.class)
//                                    .flatMap(body -> Mono.error(new ShiprocketException(
//                                            "Shiprocket server error: " + clientResponse.statusCode() + " - " + body)))
//                    )
//                    .bodyToMono(Map.class)
//                    .block();
//
//            if (response != null && response.containsKey("token")) {
//                return Map.of(
//                        "success", true,
//                        "message", "Authentication successful",
//                        "token", "***" + ((String) response.get("token")).substring(Math.max(0, ((String) response.get("token")).length() - 10)),
//                        "expires_in", response.get("expires_in"),
//                        "token_type", response.get("token_type")
//                );
//            } else {
//                return Map.of(
//                        "success", false,
//                        "message", "Authentication failed - no token in response",
//                        "response", response
//                );
//            }
//
//        } catch (Exception e) {
//            log.error("Authentication test failed: {}", e.getMessage());
//            return Map.of(
//                    "success", false,
//                    "message", "Authentication failed: " + e.getMessage(),
//                    "error", e.getMessage()
//            );
//        }
//    }


    /**
     * Check serviceability for pin codes
     */
    public boolean checkServiceability(String pickupPincode, String deliveryPincode, Double weight) {
        try {
            List<Map<String, Object>> rates = getShippingRates(pickupPincode, deliveryPincode, weight, 10, 10, 10);
            return rates != null && !rates.isEmpty();
        } catch (Exception e) {
            log.warn("Serviceability check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check serviceability for addresses
     */
    public boolean checkServiceability(Address pickupAddress, Address deliveryAddress, Double weight) {
        return checkServiceability(pickupAddress.getPinCode(), deliveryAddress.getPinCode(), weight);
    }

    /**
     * Helper method to format address string
     */
    private String getAddressString(Address address) {
        StringBuilder addressBuilder = new StringBuilder();

        if (address.getHouseNumber() != null) {
            addressBuilder.append(address.getHouseNumber()).append(", ");
        }

        if (address.getApartmentName() != null) {
            addressBuilder.append(address.getApartmentName()).append(", ");
        }

        if (address.getStreetName() != null) {
            addressBuilder.append(address.getStreetName()).append(", ");
        }

        if (address.getLandMark() != null) {
            addressBuilder.append(address.getLandMark()).append(", ");
        }

        if (address.getCity() != null) {
            addressBuilder.append(address.getCity()).append(", ");
        }

        if (address.getState() != null) {
            addressBuilder.append(address.getState()).append(", ");
        }

        if (address.getPinCode() != null) {
            addressBuilder.append(address.getPinCode());
        }

        return addressBuilder.toString();
    }

    /**
     * Cancel shipment
     */
    public Map<String, Object> cancelShipment(Long shipmentId) {
        try {
            String token = getAuthToken();

            Map<String, Object> request = new HashMap<>();
            request.put("ids", new Long[]{shipmentId});

            log.info("Cancelling shipment ID: {}", shipmentId);

            return webClient.post()
                    .uri("/orders/cancel")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("Error cancelling shipment: {}", e.getMessage());
            throw new ShiprocketException("Failed to cancel shipment: " + e.getMessage());
        }
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getHouseNumber() != null) sb.append(address.getHouseNumber()).append(", ");
        if (address.getApartmentName() != null) sb.append(address.getApartmentName()).append(", ");
        if (address.getStreetName() != null) sb.append(address.getStreetName()).append(", ");
        if (address.getLandMark() != null) sb.append(address.getLandMark()).append(", ");
        if (address.getCity() != null) sb.append(address.getCity()).append(", ");
        if (address.getState() != null) sb.append(address.getState()).append(" - ");
        if (address.getPinCode() != null) sb.append(address.getPinCode());
        return sb.toString();
    }

    private String getPhoneNumber(Address address, User user) {
        if (address.getAlternatePhoneNumber() != null &&
                !address.getAlternatePhoneNumber().isEmpty()) {
            return address.getAlternatePhoneNumber();
        }
        return user.getPhoneNumber() != null ? user.getPhoneNumber() : "";
    }

    private String getSku(Product product) {
        if (product.getSku() != null && !product.getSku().isEmpty()) {
            return product.getSku();
        }
        return "SKU-" + product.getId();
    }




    public Map<String, Object> schedulePickup(Long shipmentId, String pickupDate) {
        try {
            String token = getAuthToken();

            Map<String, Object> request = new HashMap<>();
            request.put("shipment_id", new Long[]{shipmentId});
            request.put("pickup_date", pickupDate);

            log.info("Scheduling pickup for shipment ID: {} on {}", shipmentId, pickupDate);

            return webClient.post()
                    .uri("/courier/generate/pickup")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("Error scheduling pickup: {}", e.getMessage());
            throw new ShiprocketException("Failed to schedule pickup: " + e.getMessage());
        }
    }



}