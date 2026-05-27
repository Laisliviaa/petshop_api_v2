package com.example.petshopapi.apikey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_api_keys")
@Getter @Setter @NoArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String keyValue;

    @Column(nullable = false, length = 100)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccessLevel role = AccessLevel.USER;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (this.keyValue == null || this.keyValue.isBlank())
            this.keyValue = UUID.randomUUID().toString();
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();
    }

    public enum AccessLevel { USER, ADMIN }
}
