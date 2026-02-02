package com.organics.products.service;

import com.organics.products.dto.LocationResponse;
import com.organics.products.exception.ExternalServiceException;
import com.organics.products.exception.PincodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PincodeService {

    private final RestClient restClient;

    public PincodeService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable(
            value = "pincodeLocation", key = "#pinCode", unless = "#result == null")
    public LocationResponse getByPincode(String pinCode) {

        log.info("Fetching location for pincode: {}", pinCode);

        try {
            List<Map<String, Object>> response =
                    restClient.get()
                            .uri("https://api.postalpincode.in/pincode/{pin}", pinCode)
                            .retrieve()
                            .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.isEmpty()) {
                log.warn("Empty response received for pincode: {}", pinCode);
                throw new PincodeNotFoundException("Invalid pincode: " + pinCode);
            }

            Map<String, Object> data = response.get(0);

            List<Map<String, String>> postOffices =
                    (List<Map<String, String>>) data.get("PostOffice");

            if (postOffices == null || postOffices.isEmpty()) {
                log.warn("No post offices found for pincode: {}", pinCode);
                throw new PincodeNotFoundException("No address found for pincode: " + pinCode);
            }

            Map<String, String> po = postOffices.get(0);

            LocationResponse res = new LocationResponse();
            res.setCity(po.get("District"));
            res.setState(po.get("State"));
            res.setPinCode(pinCode);

            log.info("Resolved pincode {} → {}, {}", pinCode, res.getCity(), res.getState());

            return res;

        } catch (ResourceAccessException ex) {

            log.error("Pincode API unavailable for pincode: {}", pinCode, ex);

            throw new ExternalServiceException(
                    "Pincode service temporarily unavailable. Please enter address manually."
            );

        } catch (PincodeNotFoundException ex) {

            log.warn("Pincode lookup failed: {}", ex.getMessage());
            throw ex;

        } catch (Exception ex) {

            log.error("Unexpected error while fetching pincode: {}", pinCode, ex);

            throw new ExternalServiceException(
                    "Unexpected error while fetching pincode. Try again later."
            );
        }
    }
}
