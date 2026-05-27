package com.example.petshopapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "João Silva", description = "Nome completo do cliente")
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @Schema(example = "123.456.789-00", description = "CPF do cliente")
    @NotBlank(message = "O CPF é obrigatório")
    private String cpf;

    @JsonIgnoreProperties("cliente")
    @OneToMany(mappedBy = "cliente")
    private List<Pet> pets;
}
