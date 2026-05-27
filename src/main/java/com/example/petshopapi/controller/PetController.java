package com.example.petshopapi.controller;

import com.example.petshopapi.exception.ConflictException;
import com.example.petshopapi.exception.RecursoNaoEncontradoException;
import com.example.petshopapi.model.Pet;
import com.example.petshopapi.repository.PetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@Tag(name = "Pets", description = "Pets dos clientes. Demonstra Many-to-One com Cliente e Many-to-Many com Serviços.")
@RequestMapping("/api/v1/pets")
public class PetController {

    private final PetRepository repository;
    private final PagedResourcesAssembler<Pet> assembler;

    private record PetFingerprint(String nome, String especie) {}
    private record IdempotentPet(PetFingerprint fp, Pet body) {}
    private final ConcurrentHashMap<String, IdempotentPet> idempotencyCache = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Autowired
    public PetController(PetRepository repository, PagedResourcesAssembler<Pet> assembler) {
        this.repository = repository; this.assembler = assembler;
    }

    @Operation(summary = "Lista todos os pets", description = "Retorna lista paginada com links HATEOAS.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping
    public PagedModel<EntityModel<Pet>> listar(Pageable pageable) {
        Page<Pet> pets = repository.findAll(pageable);
        return assembler.toModel(pets,
            pet -> EntityModel.of(pet, linkTo(methodOn(PetController.class).buscar(pet.getId())).withSelfRel()));
    }

    @Operation(summary = "Busca pet por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet encontrado"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping("/{id}")
    public EntityModel<Pet> buscar(@PathVariable Long id) {
        Pet pet = repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(id));
        return EntityModel.of(pet,
            linkTo(methodOn(PetController.class).buscar(id)).withSelfRel(),
            linkTo(methodOn(PetController.class).listar(null)).withRel("lista"));
    }

    @Operation(summary = "Busca pets por nome", description = "Consulta personalizada por nome.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultados encontrados"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping("/busca")
    public CollectionModel<EntityModel<Pet>> buscarPorNome(@RequestParam String nome) {
        List<EntityModel<Pet>> pets = repository.findByNomeContainingIgnoreCase(nome).stream()
            .map(p -> EntityModel.of(p, linkTo(methodOn(PetController.class).buscar(p.getId())).withSelfRel()))
            .collect(Collectors.toList());
        return CollectionModel.of(pets);
    }

    @Operation(summary = "Cadastra novo pet",
        description = "Suporta idempotência via X-Idempotency-Key. Mesma chave + JSON diferente → 409.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pet criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com payload diferente"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Pet>> criar(
            @RequestHeader("X-API-Key") String apiKey,
            @Parameter(description = "UUID único por operação")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Pet pet) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            PetFingerprint fp = new PetFingerprint(pet.getNome(), pet.getEspecie());
            synchronized (lock) {
                IdempotentPet cached = idempotencyCache.get(idempotencyKey);
                if (cached != null) {
                    if (!cached.fp().equals(fp))
                        throw new ConflictException("Idempotency-Key já utilizada com payload diferente.");
                    return ResponseEntity.ok(EntityModel.of(cached.body(),
                        linkTo(methodOn(PetController.class).buscar(cached.body().getId())).withSelfRel()));
                }
                Pet novo = repository.save(pet);
                idempotencyCache.put(idempotencyKey, new IdempotentPet(fp, novo));
                return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
                    linkTo(methodOn(PetController.class).buscar(novo.getId())).withSelfRel()));
            }
        }

        Pet novo = repository.save(pet);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
            linkTo(methodOn(PetController.class).buscar(novo.getId())).withSelfRel()));
    }

    @Operation(summary = "Atualiza dados do pet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @PutMapping("/{id}")
    public EntityModel<Pet> atualizar(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody Pet novo) {
        return repository.findById(id).map(pet -> {
            pet.setNome(novo.getNome()); pet.setEspecie(novo.getEspecie()); pet.setCliente(novo.getCliente());
            return EntityModel.of(repository.save(pet),
                linkTo(methodOn(PetController.class).buscar(id)).withSelfRel());
        }).orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    @Operation(summary = "Deleta um pet")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removido com sucesso"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "404", description = "Pet não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@RequestHeader("X-API-Key") String apiKey, @PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
