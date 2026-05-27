package com.example.petshopapi.apikey;

import java.time.LocalDateTime;

public record ApiKeyResponse(
        Long id,
        String apiKey,
        String clientName,
        ApiKey.AccessLevel role,
        boolean active,
        LocalDateTime createdAt
) {}
