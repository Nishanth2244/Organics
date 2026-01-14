package com.organics.products.service;

import com.organics.products.dto.LocationResponse;
import com.organics.products.entity.Address;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
@Service
public class LocationService {

    private final RestClient restClient;

    public LocationService(RestClient restClient) {
        this.restClient = restClient;
    }

    public LocationResponse reverseGeocode(double lat, double lon) {

        Map response =
                restClient.get()
                        .uri(
                                "https://nominatim.openstreetmap.org/reverse?format=json&lat={lat}&lon={lon}",
                                lat, lon
                        )
                        .retrieve()
                        .body(Map.class);

        Map addr = (Map) response.get("address");

        LocationResponse res = new LocationResponse();
        res.setStreet((String) addr.get("road"));
        res.setCity((String) addr.getOrDefault("city", addr.get("town")));
        res.setState((String) addr.get("state"));
        res.setPinCode((String) addr.get("postcode"));
        res.setLatitude(lat);
        res.setLongitude(lon);

        return res;
    }
}
