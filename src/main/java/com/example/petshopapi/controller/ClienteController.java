package com.example.petshopapi.controller;

import com.example.petshopapi.exception.ConflictException;
import com.example.petshopapi.exception.RecursoNaoEncontradoException;
import com.example.petshopapi.model.Cliente;
import com.example.petshopapi.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@Tag(name = "Clientes", description = "Clientes do PetShop. Suporta versionamento via header X-API-Version (1 ou 2).")
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService service;
    private final PagedResourcesAssembler<Cliente> assembler;

    private record ClienteFingerprint(String nome, String cpf) {}
    private record IdempotentCliente(ClienteFingerprint fp, Cliente body) {}
    private final ConcurrentHashMap<String, IdempotentCliente> idempotencyCache = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Autowired
    public ClienteController(ClienteService service, PagedResourcesAssembler<Cliente> assembler) {
        this.service = service; this.assembler = assembler;
    }

    @Operation(
        summary = "Lista todos os clientes",
        description = """
            Retorna lista paginada.

            **Versionamento via header X-API-Version:**
            - `1` (padrão) → resposta com HATEOAS
            - `2` → resposta enriquecida com `totalPets` e metadados extras
            """,
        parameters = @Parameter(name = "X-API-Version", in = ParameterIn.HEADER,
                description = "Versão da resposta: 1 (padrão) ou 2", example = "1")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Versão inválida no header X-API-Version",
                content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido",
                content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String version,
            Pageable pageable) {

        Page<Cliente> page = service.listarTodos(pageable);

        if ("2".equals(version)) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("apiVersion", "2");
            response.put("totalElements", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("page", page.getNumber());
            response.put("content", page.getContent().stream().map(c -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", c.getId());
                item.put("nome", c.getNome());
                item.put("cpf", c.getCpf());
                item.put("totalPets", c.getPets() != null ? c.getPets().size() : 0);
                item.put("_links", Map.of(
                    "self", "/api/v1/clientes/" + c.getId(),
                    "pets", "/api/v1/pets?clienteId=" + c.getId()
                ));
                return item;
            }).toList());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(assembler.toModel(page,
            c -> EntityModel.of(c,
                linkTo(methodOn(ClienteController.class).buscar(c.getId(), "1")).withSelfRel(),
                linkTo(methodOn(ClienteController.class).listar("1", null)).withRel("lista"))));
    }

    @Operation(summary = "Busca cliente por ID",
        parameters = @Parameter(name = "X-API-Version", in = ParameterIn.HEADER, example = "1"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "400", description = "Versão inválida"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(
            @PathVariable Long id,
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String version) {

        Cliente c = service.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(id));

        if ("2".equals(version)) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("apiVersion", "2");
            response.put("id", c.getId());
            response.put("nome", c.getNome());
            response.put("cpf", c.getCpf());
            response.put("totalPets", c.getPets() != null ? c.getPets().size() : 0);
            response.put("pets", c.getPets());
            response.put("_links", Map.of("self", "/api/v1/clientes/" + c.getId()));
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(EntityModel.of(c,
            linkTo(methodOn(ClienteController.class).buscar(id, "1")).withSelfRel(),
            linkTo(methodOn(ClienteController.class).listar("1", null)).withRel("lista")));
    }

    @Operation(summary = "Busca cliente por CPF")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<EntityModel<Cliente>> buscarPorCpf(@PathVariable String cpf) {
        Cliente c = service.buscarPorCpf(cpf)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado com CPF: " + cpf));
        return ResponseEntity.ok(EntityModel.of(c,
            linkTo(methodOn(ClienteController.class).buscar(c.getId(), "1")).withSelfRel()));
    }

    @Operation(
        summary = "Cadastra novo cliente",
        description = "Suporta idempotência via header **X-Idempotency-Key**. " +
            "Mesma chave + JSON diferente → 409 Conflict. " +
            "Requer X-API-Key no header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com payload diferente"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Cliente>> criar(
            @Parameter(description = "X-API-Key de autenticação", required = true)
            @RequestHeader("X-API-Key") String apiKey,
            @Parameter(description = "UUID único por operação para idempotência")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Cliente cliente) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ClienteFingerprint fp = new ClienteFingerprint(cliente.getNome(), cliente.getCpf());
            synchronized (lock) {
                IdempotentCliente cached = idempotencyCache.get(idempotencyKey);
                if (cached != null) {
                    if (!cached.fp().equals(fp))
                        throw new ConflictException("Idempotency-Key já utilizada com payload diferente.");
                    return ResponseEntity.ok(EntityModel.of(cached.body(),
                        linkTo(methodOn(ClienteController.class).buscar(cached.body().getId(), "1")).withSelfRel()));
                }
                Cliente novo = service.salvar(cliente);
                idempotencyCache.put(idempotencyKey, new IdempotentCliente(fp, novo));
                return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
                    linkTo(methodOn(ClienteController.class).buscar(novo.getId(), "1")).withSelfRel(),
                    linkTo(methodOn(ClienteController.class).listar("1", null)).withRel("lista")));
            }
        }

        Cliente novo = service.salvar(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
            linkTo(methodOn(ClienteController.class).buscar(novo.getId(), "1")).withSelfRel()));
    }

    @Operation(summary = "Atualiza dados do cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @PutMapping("/{id}")
    public EntityModel<Cliente> atualizar(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long id,
            @Valid @RequestBody Cliente novo) {
        return service.buscarPorId(id).map(c -> {
            c.setNome(novo.getNome()); c.setCpf(novo.getCpf());
            return EntityModel.of(service.salvar(c),
                linkTo(methodOn(ClienteController.class).buscar(id, "1")).withSelfRel());
        }).orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    @Operation(summary = "Deleta um cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removido com sucesso"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long id) {
        if (!service.buscarPorId(id).isPresent()) return ResponseEntity.notFound().build();
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
