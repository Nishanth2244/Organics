package com.organics.products.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.organics.products.entity.AddressType;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaveAddressRequest {

    private AddressType addressType;
    private String alternatePhoneNumber;

    private String houseNumber;
    private String apartmentName;
    private String streetName;
    private String landMark;

    private String pinCode;

    private Double latitude;
    private Double longitude;

    private Boolean isPrimary;
}
