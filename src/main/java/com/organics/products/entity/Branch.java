package com.organics.products.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "branch")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String branchName;
    private String branchCode;
    private Double latitude;
    private Double lognitude;
    private String location;
    private Double chargePerKm;

    @OneToMany

    private List<Inventory> inventory;



}
