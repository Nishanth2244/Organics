package com.organics.products.dto;

import lombok.Data;

@Data
public class LocationResponse {
    private String city;
    private String state;
    private String pinCode;
    private String street;
    private Double latitude;
    private Double longitude;
}
