package com.organics.products.dto;

import lombok.Data;

@Data
public class CreateAdminRequest {
    private String email;
    private String password;
    private String phoneNumber;
}
