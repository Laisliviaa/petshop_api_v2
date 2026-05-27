package com.example.petshopapi.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(Long id) {
        super("Não foi possível encontrar o registro com ID: " + id);
    }
}
