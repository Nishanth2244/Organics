package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "cartItems")
public class CartItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;
}
