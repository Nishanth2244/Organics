package com.organics.products.controller;

import com.organics.products.dto.LocationResponse;
import com.organics.products.entity.Address;
import com.organics.products.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/current")
    public LocationResponse current(
            @RequestParam double lat,
            @RequestParam double lon) {

        return locationService.reverseGeocode(lat, lon);
    }
}

