package com.example.petshopapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Entity
@Data
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "Rex", description = "Nome do pet")
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @Schema(example = "Cachorro", description = "Espécie do pet")
    private String especie;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties("pets")
    private Cliente cliente;

    @ManyToMany
    @JoinTable(
            name = "pet_servicos",
            joinColumns = @JoinColumn(name = "pet_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    @JsonIgnoreProperties("pets")
    private List<Servico> servicos;
}
