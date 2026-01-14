package com.organics.products.dto;

import com.organics.products.entity.AddressType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressResponse {

    private Long id;

    private AddressType addressType;

    private String houseNumber;
    private String apartmentName;
    private String streetName;
    private String landMark;

    private String city;
    private String state;
    private String pinCode;

    private String alternatePhoneNumber;

    private Double latitude;
    private Double longitude;

    private Boolean isPrimary;
}
