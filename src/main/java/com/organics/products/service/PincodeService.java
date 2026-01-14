package com.organics.products.service;

import com.organics.products.dto.LocationResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class PincodeService {

    private final RestClient restClient;

    public PincodeService(RestClient restClient) {
        this.restClient = restClient;
    }

    public LocationResponse getByPincode(String pinCode) {

        try {
            List<Map<String, Object>> response =
                    restClient.get()
                            .uri("https://api.postalpincode.in/pincode/{pin}", pinCode)
                            .retrieve()
                            .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Invalid pincode");
            }

            Map<String, Object> data = response.get(0);
            List<Map<String, String>> postOffices =
                    (List<Map<String, String>>) data.get("PostOffice");

            if (postOffices == null || postOffices.isEmpty()) {
                throw new RuntimeException("No address found for pincode");
            }

            Map<String, String> po = postOffices.get(0);

            LocationResponse res = new LocationResponse();
            res.setCity(po.get("District"));
            res.setState(po.get("State"));
            res.setPinCode(pinCode);

            return res;

        } catch (ResourceAccessException ex) {
            throw new RuntimeException(
                    "Pincode service temporarily unavailable. Please enter address manually."
            );
        }
    }
}
