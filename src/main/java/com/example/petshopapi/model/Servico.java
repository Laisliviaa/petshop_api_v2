package com.example.petshopapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Servico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "Banho e Tosa", description = "Descrição do serviço")
    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;

    @Schema(example = "80.00", description = "Preço do serviço")
    @NotNull
    @Positive(message = "O preço deve ser maior que zero")
    private Double preco;

    @ManyToMany(mappedBy = "servicos")
    @JsonIgnoreProperties("servicos")
    private List<Pet> pets;
}
