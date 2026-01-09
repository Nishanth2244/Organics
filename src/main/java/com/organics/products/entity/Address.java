package com.organics.products.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

@Entity
@Data
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long phoneNumber;
    private String houseNumber;
    private String apartmentName;
    private String streetName;
    private String state;
    private String city;
    private Integer pinCode;
    private String landMark;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
