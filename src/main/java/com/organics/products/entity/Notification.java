package com.organics.products.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_table", schema = "organic")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L; // <--- Best practice
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiver;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "admin_id")
    private Long adminId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;
}
