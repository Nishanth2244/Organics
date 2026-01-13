package com.organics.products.dto;

import lombok.Data;


@Data
public class AddressDTO {
    private Long id;
    private Long phoneNumber;
    private String houseNumber;
    private String streetName;
    private String city;
    private String state;
    private String apartmentName;
    private Integer pinCode;
    private String landMark;

    private Long customerId;
}