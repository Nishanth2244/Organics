package com.organics.products.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private String brand;
    private String description;
    private Integer returnDays;
    private Double MRP;
    private Boolean status;
    
    
    @Enumerated(EnumType.STRING)
    private UnitType unit;
    private Double netWeight;


    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
   
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ProductImage> images;
    
    @OneToMany(mappedBy = "product")
    private List<Inventory> inventories;

    private String sku;
}