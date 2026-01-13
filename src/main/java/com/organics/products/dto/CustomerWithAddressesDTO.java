package com.organics.products.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CustomerWithAddressesDTO {
    private Long id;
    private String displayName;
    private Long phoneNumber;
    private String emailId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private List<AddressDTO> addresses;
}