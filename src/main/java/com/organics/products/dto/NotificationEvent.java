package com.organics.products.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private String receiver;
    private String message;
    private String type;
    private String sender;
    private String link;
    private String category;
    private String kind;
    private String subject;
    private LocalDateTime createdAt;
}
