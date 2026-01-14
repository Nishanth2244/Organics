package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryName;
    @Column(length = 10000)
    private String description;
    private String categoryImage;
    private Boolean status;

}
