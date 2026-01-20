package com.organics.products.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Double rating;
    private String imageUrl;
    private String userName;
    private LocalDateTime createdAt;
}
