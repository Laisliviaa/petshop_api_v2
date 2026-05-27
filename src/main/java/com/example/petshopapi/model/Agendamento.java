package com.example.petshopapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Agendamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "2026-05-10T14:00:00", description = "Data e hora do agendamento")
    @NotNull(message = "A data é obrigatória")
    private LocalDateTime dataHora;

    @Schema(example = "PENDENTE", description = "Status do agendamento: PENDENTE, CONCLUIDO ou CANCELADO")
    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    @JsonIgnoreProperties("servicos")
    private Pet pet;
}
