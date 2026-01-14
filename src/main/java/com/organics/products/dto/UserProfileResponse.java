package com.organics.products.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserProfileResponse {

    private Long id;
    private String phoneNumber;
    private String emailId;

    private String displayName;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;

    private List<AddressResponse> addresses;
}
