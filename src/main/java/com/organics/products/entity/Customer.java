//package com.organics.products.entity;
//
//import jakarta.persistence.*;
//import lombok.Data;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Data
//@Table(name = "customers")
//public class Customer {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    private String displayName;
//    private Long phoneNumber;
//    private String emailId;
//    private String firstName;
//    private String middleName;
//    private String lastName;
//    private String gender;
//    private LocalDate dateOfBirth;
//    private Integer age;
//
//    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Address> addresses = new ArrayList<>();
//
//
//}
