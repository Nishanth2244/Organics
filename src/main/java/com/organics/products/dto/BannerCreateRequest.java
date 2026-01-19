package com.organics.products.dto;

import lombok.Data;

import java.util.List;

@Data
public class BannerCreateRequest {
    private String title;
    private String description;
    private List<String> imageUrls;
    private String redirectUrl;
}
