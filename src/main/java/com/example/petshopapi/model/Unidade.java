package com.example.petshopapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class Unidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "Unidade Centro", description = "Nome da unidade do petshop")
    @NotBlank(message = "Nome da unidade é obrigatório")
    private String nome;

    @Schema(example = "Rua das Flores, 100", description = "Endereço da unidade")
    private String endereco;
}
