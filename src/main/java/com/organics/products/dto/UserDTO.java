package com.organics.products.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {

    private Long id;

    private String displayName;
    private String firstName;
    private String lastName;

    private String phoneNumber;
    private String emailId;

    private String status;
}
