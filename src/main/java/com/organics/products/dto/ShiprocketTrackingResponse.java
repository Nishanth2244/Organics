// ShiprocketTrackingResponse.java
package com.organics.products.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ShiprocketTrackingResponse {
    @JsonProperty("tracking_data")
    private TrackingData trackingData;
    
    @Data
    public static class TrackingData {
        @JsonProperty("track_url")
        private String trackUrl;
        
        private List<TrackingEvent> etd;
        
        @JsonProperty("awb_code")
        private String awbCode;
        
        @JsonProperty("courier_name")
        private String courierName;
    }
    
    @Data
    public static class TrackingEvent {
        private String date;
        private String status;
        private String activity;
    }
}