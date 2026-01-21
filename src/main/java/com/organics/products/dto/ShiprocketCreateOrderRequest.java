// ShiprocketCreateOrderRequest.java
package com.organics.products.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ShiprocketCreateOrderRequest {
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("order_date")
    private String orderDate;
    
    @JsonProperty("pickup_location")
    private String pickupLocation = "Primary";
    
    @JsonProperty("billing_customer_name")
    private String billingCustomerName;
    
    @JsonProperty("billing_last_name")
    private String billingLastName;
    
    @JsonProperty("billing_address")
    private String billingAddress;
    
    @JsonProperty("billing_address_2")
    private String billingAddress2;
    
    @JsonProperty("billing_city")
    private String billingCity;
    
    @JsonProperty("billing_pincode")
    private String billingPincode;
    
    @JsonProperty("billing_state")
    private String billingState;
    
    @JsonProperty("billing_country")
    private String billingCountry = "India";
    
    @JsonProperty("billing_email")
    private String billingEmail;
    
    @JsonProperty("billing_phone")
    private String billingPhone;
    
    @JsonProperty("shipping_is_billing")
    private boolean shippingIsBilling = true;
    
    @JsonProperty("shipping_customer_name")
    private String shippingCustomerName;
    
    @JsonProperty("shipping_last_name")
    private String shippingLastName;
    
    @JsonProperty("shipping_address")
    private String shippingAddress;
    
    @JsonProperty("shipping_address_2")
    private String shippingAddress2;
    
    @JsonProperty("shipping_city")
    private String shippingCity;
    
    @JsonProperty("shipping_pincode")
    private String shippingPincode;
    
    @JsonProperty("shipping_country")
    private String shippingCountry = "India";
    
    @JsonProperty("shipping_state")
    private String shippingState;
    
    @JsonProperty("shipping_email")
    private String shippingEmail;
    
    @JsonProperty("shipping_phone")
    private String shippingPhone;
    
    private Double orderAmount;
    
    @JsonProperty("payment_method")
    private String paymentMethod = "Prepaid";
    
    @JsonProperty("shipping_charges")
    private Double shippingCharges = 0.0;
    
    @JsonProperty("giftwrap_charges")
    private Double giftwrapCharges = 0.0;
    
    @JsonProperty("transaction_charges")
    private Double transactionCharges = 0.0;
    
    @JsonProperty("total_discount")
    private Double totalDiscount = 0.0;
    
    private String comment;
    
    private List<ShiprocketOrderItem> orderItems;
}

@Data
class ShiprocketOrderItem {
    private String name;
    private String sku;
    private Integer units;
    private Double sellingPrice;
    private Double discount;
    private Double tax;
    private Double hsn;
}