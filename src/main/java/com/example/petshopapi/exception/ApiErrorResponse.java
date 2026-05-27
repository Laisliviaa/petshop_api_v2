package com.example.petshopapi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private final LocalDateTime timestamp;
    private final int           status;
    private final String        erro;
    private final String        mensagem;
    private final String        caminho;
    private final String        metodo;
    private final List<String>  detalhes;
}
