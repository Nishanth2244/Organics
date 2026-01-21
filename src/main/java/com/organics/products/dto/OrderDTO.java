// OrderDTO.java
package com.organics.products.dto;

import com.organics.products.entity.OrderStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private LocalDate orderDate;
    private Double orderAmount;
    private String description;
    private OrderStatus orderStatus;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;

    private ShippingAddressDTO shippingAddress;

    // Shiprocket fields
    private String shiprocketOrderId;
    private Long shiprocketShipmentId;
    private String shiprocketAwbCode;
    private String shiprocketCourierName;
    private String shiprocketLabelUrl;
    private String shiprocketTrackingUrl;

    // Shipping address
    //private String shippingAddress;

    private List<OrderItemDTO> orderItems;
}