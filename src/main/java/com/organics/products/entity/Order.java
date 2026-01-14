package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

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

     @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

     @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
