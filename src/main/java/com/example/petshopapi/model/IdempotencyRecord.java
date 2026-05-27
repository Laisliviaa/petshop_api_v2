package com.example.petshopapi.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class IdempotencyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false) private String idempotencyKey;
    @Column(nullable = false, length = 4000) private String responseBody;
    @Column(nullable = false) private int responseStatus;
    private LocalDateTime createdAt = LocalDateTime.now();
}
