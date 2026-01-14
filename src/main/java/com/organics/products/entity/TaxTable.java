package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "tax")
public class TaxTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String taxName;
    private Double taxPercentage;
    private boolean isActive;

    @OneToMany
    @JoinColumn(name = "product_id")
    private List<Product> product;
}
