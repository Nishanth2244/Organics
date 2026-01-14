package com.organics.products.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDisplayNameRequest {


    private Long id;
    @NotBlank
    private String displayName;
}
