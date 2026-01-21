package com.organics.products.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "shiprocket")
public class ShiprocketConfigProperties {
    private String baseUrl = "https://apiv2.shiprocket.in/v1/external";
    private String email;
    private String password;
    private String webhookUrl;
    private boolean enableSandbox = false;
}