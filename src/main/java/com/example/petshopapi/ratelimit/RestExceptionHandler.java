package com.example.petshopapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(RecursoNaoEncontradoException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .erro("Bad Request")
                .mensagem("Validação falhou. Verifique os campos obrigatórios.")
                .caminho(req.getRequestURI())
                .metodo(req.getMethod())
                .detalhes(detalhes)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                                 HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Header obrigatório ausente: " + ex.getHeaderName(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido. Verifique o JSON.", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                    HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não permitido.", req);
    }

    /**
     * Disparado quando um DELETE tenta remover um recurso que possui dependências
     * (ex.: FK constraint). Retorna 409 Conflict em vez de 500.
     *
     * Códigos de erro possíveis:
     *   409 Conflict – entidade possui relacionamentos que impedem a exclusão.
     *                   Ex.: tentar deletar um Cliente que ainda possui Pets cadastrados.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                 HttpServletRequest req) {
        log.warn("DataIntegrityViolation em {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT,
                "Não é possível remover este recurso pois ele possui dependências associadas. " +
                "Remova os registros relacionados antes de tentar novamente.", req);
    }

    /**
     * Disparado quando deleteById é chamado com um ID inexistente.
     * Retorna 404 em vez de 500.
     *
     * Códigos de erro possíveis:
     *   404 Not Found – ID informado não existe na base de dados.
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleEmptyResult(EmptyResultDataAccessException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado para a operação solicitada.", req);
    }

    /**
     * Disparado quando um IllegalArgumentException é lançado (ex.: enum inválido na URL).
     *
     * Códigos de erro possíveis:
     *   400 Bad Request – valor de parâmetro ou enum inválido.
     */
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                   HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    /**
     * Captura qualquer exceção não tratada pelas regras acima.
     *
     * Códigos de erro possíveis:
     *   500 Internal Server Error – erro inesperado no servidor.
     *   A mensagem de detalhes é registrada no log; o cliente recebe apenas uma
     *   descrição genérica para não expor internals.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado em {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Por favor, tente novamente ou contate o suporte.", req);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String mensagem,
                                                    HttpServletRequest req) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .erro(status.getReasonPhrase())
                .mensagem(mensagem)
                .caminho(req.getRequestURI())
                .metodo(req.getMethod())
                .build());
    }
}
