package com.organics.products.dto;

import lombok.Data;

@Data
public class TaxRequestDTO {
    private Long categoryId;
    private double taxPercent;
}
