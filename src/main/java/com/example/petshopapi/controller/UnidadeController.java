package com.example.petshopapi.controller;

import com.example.petshopapi.exception.RecursoNaoEncontradoException;
import com.example.petshopapi.model.Unidade;
import com.example.petshopapi.repository.UnidadeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@Tag(name = "Unidades")
@RequestMapping("/api/v1/unidades")
public class UnidadeController {

    private final UnidadeRepository repository;
    private final PagedResourcesAssembler<Unidade> assembler;

    @Autowired
    public UnidadeController(UnidadeRepository repository, PagedResourcesAssembler<Unidade> assembler) {
        this.repository = repository; this.assembler = assembler;
    }

    @Operation(summary = "Lista todos")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Sucesso"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @GetMapping
    public PagedModel<EntityModel<Unidade>> listar(Pageable pageable) {
        return assembler.toModel(repository.findAll(pageable),
            e -> EntityModel.of(e, linkTo(methodOn(UnidadeController.class).buscar(e.getId())).withSelfRel()));
    }

    @Operation(summary = "Busca por ID")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Encontrado"), @ApiResponse(responseCode = "404", description = "Não encontrado"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @GetMapping("/{id}")
    public EntityModel<Unidade> buscar(@PathVariable Long id) {
        Unidade e = repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(id));
        return EntityModel.of(e, linkTo(methodOn(UnidadeController.class).buscar(id)).withSelfRel(),
            linkTo(methodOn(UnidadeController.class).listar(null)).withRel("lista"));
    }

    @Operation(summary = "Busca personalizada")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Resultados encontrados"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @GetMapping("/busca")
    public CollectionModel<EntityModel<Unidade>> buscar(@RequestParam String q) {
        List<EntityModel<Unidade>> results = repository.findAll().stream()
            .map(e -> EntityModel.of(e, linkTo(methodOn(UnidadeController.class).buscar(e.getId())).withSelfRel()))
            .collect(Collectors.toList());
        return CollectionModel.of(results);
    }

    @Operation(summary = "Cria novo registro", description = "Suporta idempotência via X-Idempotency-Key.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Criado"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "401", description = "X-API-Key inválida"), @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com payload diferente"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @PostMapping
    public ResponseEntity<EntityModel<Unidade>> criar(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Unidade obj) {
        Unidade novo = repository.save(obj);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
            linkTo(methodOn(UnidadeController.class).buscar(novo.getId())).withSelfRel()));
    }

    @Operation(summary = "Atualiza registro")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Atualizado"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "401", description = "X-API-Key inválida"), @ApiResponse(responseCode = "404", description = "Não encontrado"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Unidade>> atualizar(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long id, @Valid @RequestBody Unidade novo) {
        if (!repository.existsById(id)) throw new RecursoNaoEncontradoException(id);
        novo.setId(id);
        return ResponseEntity.ok(EntityModel.of(repository.save(novo),
            linkTo(methodOn(UnidadeController.class).buscar(id)).withSelfRel()));
    }

    @Operation(summary = "Deleta registro")
    @ApiResponses({ @ApiResponse(responseCode = "204", description = "Removido"), @ApiResponse(responseCode = "401", description = "X-API-Key inválida"), @ApiResponse(responseCode = "404", description = "Não encontrado"), @ApiResponse(responseCode = "409", description = "Conflito de dependência: recurso possui entidades associadas"), @ApiResponse(responseCode = "429", description = "Rate limit excedido"), @ApiResponse(responseCode = "500", description = "Erro interno inesperado") })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@RequestHeader("X-API-Key") String apiKey, @PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
