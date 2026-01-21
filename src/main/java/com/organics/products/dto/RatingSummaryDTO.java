package com.organics.products.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RatingSummaryDTO {
    private Long ratingCount;
    private Double averageRating;
}
