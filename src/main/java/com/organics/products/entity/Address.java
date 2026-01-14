package com.organics.products.entity;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType addressType; // HOME, WORK

    @Pattern(regexp = "^[6-9]\\d{9}$")
    @Column(name = "alternate_phone", length = 15)
    private String alternatePhoneNumber;

    @NotBlank
    private String houseNumber;

    private String apartmentName;
    private String streetName;
    private String landMark;

    @Column(updatable = false)
    private String city;

    @Column(updatable = false)
    private String state;

    @Pattern(regexp = "\\d{6}")
    private String pinCode;

    private Double latitude;
    private Double longitude;

    private Boolean isPrimary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
