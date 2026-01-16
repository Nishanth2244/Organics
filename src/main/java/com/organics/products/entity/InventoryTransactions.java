package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
public class InventoryTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    private InventoryTransactionType transactionType;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private InventoryReferenceType referenceType;

    private Long referenceId;

    private LocalDateTime transactionDate;

    @PrePersist
    void onCreate() {
        this.transactionDate = LocalDateTime.now();
    }
}
