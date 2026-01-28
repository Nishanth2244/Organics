package com.organics.products.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyOrderStatsDTO {
    private LocalDate date;
    private Long totalOrders;
    private Double totalRevenue;
    private Long pendingOrders;
    private Long deliveredOrders;
    private Long confirmedOrders;
    private Long shippedOrders;
    private Double averageOrderValue;

}