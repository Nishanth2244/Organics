package com.organics.products.dto;

import lombok.Data;

@Data
public class ShippingAddressDTO {
    private Long id;
    private String houseNumber;
    private String apartmentName;
    private String streetName;
    private String landMark;
    private String city;
    private String state;
    private String pinCode;
    private String alternatePhoneNumber;
    private String addressType; // HOME, OFFICE, etc.
    private Boolean isPrimary;
    
    // Helper method to format full address
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (houseNumber != null) sb.append(houseNumber).append(", ");
        if (apartmentName != null) sb.append(apartmentName).append(", ");
        if (streetName != null) sb.append(streetName).append(", ");
        if (landMark != null) sb.append(landMark).append(", ");
        if (city != null) sb.append(city).append(", ");
        if (state != null) sb.append(state).append(" - ");
        if (pinCode != null) sb.append(pinCode);
        return sb.toString();
    }
}