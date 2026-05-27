package com.example.petshopapi.controller;

import com.example.petshopapi.exception.RecursoNaoEncontradoException;
import com.example.petshopapi.model.Gerente;
import com.example.petshopapi.repository.GerenteRepository;
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
@Tag(name = "Gerentes")
@RequestMapping("/api/v1/gerentes")
public class GerenteController {

    private final GerenteRepository repository;
    private final PagedResourcesAssembler<Gerente> assembler;

    @Autowired
    public GerenteController(GerenteRepository repository, PagedResourcesAssembler<Gerente> assembler) {
        this.repository = repository; this.assembler = assembler;
    }

    @Operation(summary = "Lista todos")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Sucesso"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @GetMapping
    public PagedModel<EntityModel<Gerente>> listar(Pageable pageable) {
        return assembler.toModel(repository.findAll(pageable),
            e -> EntityModel.of(e, linkTo(methodOn(GerenteController.class).buscar(e.getId())).withSelfRel()));
    }

    @Operation(summary = "Busca por ID")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Encontrado"), @ApiResponse(responseCode = "404", description = "Não encontrado"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @GetMapping("/{id}")
    public EntityModel<Gerente> buscar(@PathVariable Long id) {
        Gerente e = repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(id));
        return EntityModel.of(e, linkTo(methodOn(GerenteController.class).buscar(id)).withSelfRel(),
            linkTo(methodOn(GerenteController.class).listar(null)).withRel("lista"));
    }

    @Operation(summary = "Busca personalizada")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Resultados encontrados"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @GetMapping("/busca")
    public CollectionModel<EntityModel<Gerente>> buscar(@RequestParam String q) {
        List<EntityModel<Gerente>> results = repository.findAll().stream()
            .map(e -> EntityModel.of(e, linkTo(methodOn(GerenteController.class).buscar(e.getId())).withSelfRel()))
            .collect(Collectors.toList());
        return CollectionModel.of(results);
    }

    @Operation(summary = "Cria novo registro", description = "Suporta idempotência via X-Idempotency-Key.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Criado"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "401", description = "X-API-Key inválida"), @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com payload diferente"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @PostMapping
    public ResponseEntity<EntityModel<Gerente>> criar(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Gerente obj) {
        Gerente novo = repository.save(obj);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
            linkTo(methodOn(GerenteController.class).buscar(novo.getId())).withSelfRel()));
    }

    @Operation(summary = "Atualiza registro")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Atualizado"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "401", description = "X-API-Key inválida"), @ApiResponse(responseCode = "404", description = "Não encontrado"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<Gerente>> atualizar(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long id, @Valid @RequestBody Gerente novo) {
        if (!repository.existsById(id)) throw new RecursoNaoEncontradoException(id);
        novo.setId(id);
        return ResponseEntity.ok(EntityModel.of(repository.save(novo),
            linkTo(methodOn(GerenteController.class).buscar(id)).withSelfRel()));
    }

    @Operation(summary = "Deleta registro")
    @ApiResponses({ @ApiResponse(responseCode = "204", description = "Removido"), @ApiResponse(responseCode = "401", description = "X-API-Key inválida"), @ApiResponse(responseCode = "404", description = "Não encontrado"), @ApiResponse(responseCode = "429", description = "Rate limit excedido") })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@RequestHeader("X-API-Key") String apiKey, @PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
