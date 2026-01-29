package com.organics.products.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyNowRequestDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Address ID cannot be null")
    private Long addressId;

    @Schema(example = "SAVE10", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String couponCode; 

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

}