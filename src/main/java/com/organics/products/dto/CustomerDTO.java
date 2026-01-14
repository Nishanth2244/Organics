package com.organics.products.dto;

import lombok.Data;



import java.time.LocalDate;

@Data
public class CustomerDTO {

    private String displayName;
    private Long phoneNumber;
    private String emailId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer age;
}