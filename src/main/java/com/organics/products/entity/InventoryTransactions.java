package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    private String transactionType;
    private Integer quantity;
    private String referenceType;
    private Long referenceId;
    private LocalDate transactionDate;

}
