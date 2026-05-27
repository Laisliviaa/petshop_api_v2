package com.example.petshopapi.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApiKeyRequest {
    @NotBlank(message = "O nome do responsável é obrigatório")
    @Size(max = 100)
    private String clientName;

    private ApiKey.AccessLevel role;
}
