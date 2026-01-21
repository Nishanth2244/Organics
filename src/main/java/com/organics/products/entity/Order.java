// Add these fields to your Order.java entity
package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

// Add this to Order.java entity
@Entity
@Data
@Table(name = "order")
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDate orderDate;
	private Double orderAmount;
	private String description;

	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;

	// Shipping address
	@ManyToOne
	@JoinColumn(name = "shipping_address_id")
	private Address shippingAddress;

	// Shiprocket fields
	@Column(name = "shiprocket_order_id")
	private String shiprocketOrderId;

	@Column(name = "shiprocket_shipment_id")
	private Long shiprocketShipmentId;

	@Column(name = "shiprocket_awb_code")
	private String shiprocketAwbCode;

	@Column(name = "shiprocket_courier_name")
	private String shiprocketCourierName;

	@Column(name = "shiprocket_label_url")
	private String shiprocketLabelUrl;

	@Column(name = "shiprocket_tracking_url")
	private String shiprocketTrackingUrl;

	@ManyToOne
	@JoinColumn(name = "cart_id")
	private Cart cart;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private List<OrderItems> orderItems;
}