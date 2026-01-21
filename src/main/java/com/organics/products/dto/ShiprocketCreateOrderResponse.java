// ShiprocketCreateOrderResponse.java
package com.organics.products.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShiprocketCreateOrderResponse {
    @JsonProperty("order_id")
    private Long orderId;
    
    @JsonProperty("shipment_id")
    private Long shipmentId;
    
    private String status;
    private String message;
    
    @JsonProperty("awb_code")
    private String awbCode;
    
    @JsonProperty("courier_name")
    private String courierName;
    
    @JsonProperty("courier_company_id")
    private Long courierCompanyId;

    public void setUniqueOrderId(String uniqueOrderId) {
    }
}