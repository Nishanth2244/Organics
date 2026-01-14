package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

//    @ManyToOne
//    @JoinColumn(name = "orderitem_id")
//    private OrderItems orderItems;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private LocalDate updatedDate;

    private Integer stock;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InventoryTransactions> inventoryTransactions;





}
