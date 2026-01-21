package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_table", schema = "organic")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiver;

    @Lob
    @Column(columnDefinition = "text")
    private String message;

    private String type;
    private boolean read = false;
    private String sender;
    private String link;
    private String category;
    private String kind;

    private boolean stared = false;

    @Lob
    @Column(columnDefinition = "text")
    private String subject;

    private Boolean deleted = false;

    private LocalDateTime createdAt;
}
