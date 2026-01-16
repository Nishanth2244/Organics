package com.organics.products.dto;

import lombok.Data;

@Data
public class BranchResponse {

    private Long id;
    private String branchName;
    private String branchCode;

    private String location;
    private Double latitude;
    private Double longitude;

    private Double chargePerKm;
    private Boolean active;
}
