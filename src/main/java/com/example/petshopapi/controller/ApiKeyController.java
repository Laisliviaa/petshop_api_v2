package com.example.petshopapi.controller;

import com.example.petshopapi.apikey.ApiKey;
import com.example.petshopapi.apikey.ApiKeyRepository;
import com.example.petshopapi.apikey.ApiKeyRequest;
import com.example.petshopapi.apikey.ApiKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/apikeys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Gestão de chaves de autenticação. Geração é pública — use a chave gerada nos demais endpoints.")
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;

    @Operation(summary = "Gerar nova API Key", description = "Cria uma chave de acesso. Role: USER ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chave gerada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<ApiKeyResponse> gerar(@Valid @RequestBody ApiKeyRequest req) {
        ApiKey entity = new ApiKey();
        entity.setClientName(req.getClientName());
        if (req.getRole() != null) entity.setRole(req.getRole());
        ApiKey saved = apiKeyRepository.save(entity);
        return ResponseEntity.created(URI.create("/api/v1/apikeys/" + saved.getId()))
                .body(toResponse(saved));
    }

    @Operation(summary = "Listar todas as API Keys")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listar() {
        return ResponseEntity.ok(apiKeyRepository.findAll().stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Buscar API Key por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave encontrada"),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> buscar(@PathVariable Long id) {
        return apiKeyRepository.findById(id)
                .map(k -> ResponseEntity.ok(toResponse(k)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Revogar API Key", description = "Desativa a chave. Requer role ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave revogada"),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> revogar(@PathVariable Long id) {
        return apiKeyRepository.findById(id).map(k -> {
            k.setActive(false);
            return ResponseEntity.ok(toResponse(apiKeyRepository.save(k)));
        }).orElse(ResponseEntity.notFound().build());
    }

    private ApiKeyResponse toResponse(ApiKey k) {
        return new ApiKeyResponse(k.getId(), k.getKeyValue(), k.getClientName(),
                k.getRole(), k.isActive(), k.getCreatedAt());
    }
}
