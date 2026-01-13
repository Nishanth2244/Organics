package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "phone_number")
)
@Getter 
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
  
        ACTIVE,
        BLOCKED
    }
}
