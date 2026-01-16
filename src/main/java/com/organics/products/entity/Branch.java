package com.organics.products.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

import java.util.List;

@Entity
@Table(
        name = "branch",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "branch_code")
        }
)
@Data
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String branchName;
    private String branchCode;

    private String location;
    private String pincode;

    private Double latitude;
    private Double longitude;
    private Double chargePerKm;

    private Boolean active = true;

    @OneToMany(
            mappedBy = "branch",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Inventory> inventories;
}
