package com.organics.products.dto;

import com.organics.products.entity.EntityType;
import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String receiver,
        String message,
        String type,
        boolean read,
        String sender,
        String link,
        String category,
        String kind,
        boolean stared,
        String subject,
        Boolean deleted,
        LocalDateTime createdAt,
        EntityType entityType,
        Long entityId
) {}
