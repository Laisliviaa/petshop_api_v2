package com.example.petshopapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class Gerente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "Maria Souza", description = "Nome do gerente")
    @NotBlank(message = "Nome do gerente é obrigatório")
    private String nome;

    @Schema(description = "Unidade gerenciada (relacionamento 1:1)")
    @OneToOne
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;
}
