package com.example.petshopapi.controller;

import com.example.petshopapi.exception.ConflictException;
import com.example.petshopapi.exception.RecursoNaoEncontradoException;
import com.example.petshopapi.model.Agendamento;
import com.example.petshopapi.model.StatusAgendamento;
import com.example.petshopapi.repository.AgendamentoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@Tag(name = "Agendamentos", description = "Agendamentos de serviços para pets. Demonstra relação com Pet.")
@RequestMapping("/api/v1/agendamentos")
public class AgendamentoController {

    private final AgendamentoRepository repository;
    private final PagedResourcesAssembler<Agendamento> assembler;

    private record AgendamentoFp(String dataHora, String status) {}
    private record IdempotentAgendamento(AgendamentoFp fp, Agendamento body) {}
    private final ConcurrentHashMap<String, IdempotentAgendamento> cache = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Autowired
    public AgendamentoController(AgendamentoRepository repository, PagedResourcesAssembler<Agendamento> assembler) {
        this.repository = repository; this.assembler = assembler;
    }

    @Operation(summary = "Lista todos os agendamentos", description = "Retorna lista paginada com HATEOAS.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping
    public PagedModel<EntityModel<Agendamento>> listar(Pageable pageable) {
        return assembler.toModel(repository.findAll(pageable),
            a -> EntityModel.of(a, linkTo(methodOn(AgendamentoController.class).buscar(a.getId())).withSelfRel()));
    }

    @Operation(summary = "Busca agendamento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Encontrado"),
        @ApiResponse(responseCode = "404", description = "Não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping("/{id}")
    public EntityModel<Agendamento> buscar(@PathVariable Long id) {
        Agendamento a = repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(id));
        return EntityModel.of(a,
            linkTo(methodOn(AgendamentoController.class).buscar(id)).withSelfRel(),
            linkTo(methodOn(AgendamentoController.class).listar(null)).withRel("lista"));
    }

    @Operation(summary = "Busca agendamentos por status", description = "Consulta personalizada: PENDENTE, CONCLUIDO ou CANCELADO.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultados encontrados"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<?> buscarPorStatus(@PathVariable StatusAgendamento status) {
        return ResponseEntity.ok(repository.findByStatus(status));
    }

    @Operation(summary = "Cria novo agendamento",
        description = "Suporta idempotência via X-Idempotency-Key.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agendamento criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada com payload diferente"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @PostMapping
    public ResponseEntity<EntityModel<Agendamento>> criar(
            @RequestHeader("X-API-Key") String apiKey,
            @Parameter(description = "UUID único por operação")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Agendamento agendamento) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            AgendamentoFp fp = new AgendamentoFp(
                agendamento.getDataHora() != null ? agendamento.getDataHora().toString() : null,
                agendamento.getStatus() != null ? agendamento.getStatus().name() : null);
            synchronized (lock) {
                IdempotentAgendamento cached = cache.get(idempotencyKey);
                if (cached != null) {
                    if (!cached.fp().equals(fp))
                        throw new ConflictException("Idempotency-Key já utilizada com payload diferente.");
                    return ResponseEntity.ok(EntityModel.of(cached.body(),
                        linkTo(methodOn(AgendamentoController.class).buscar(cached.body().getId())).withSelfRel()));
                }
                Agendamento novo = repository.save(agendamento);
                cache.put(idempotencyKey, new IdempotentAgendamento(fp, novo));
                return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
                    linkTo(methodOn(AgendamentoController.class).buscar(novo.getId())).withSelfRel()));
            }
        }

        Agendamento novo = repository.save(agendamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityModel.of(novo,
            linkTo(methodOn(AgendamentoController.class).buscar(novo.getId())).withSelfRel()));
    }

    @Operation(summary = "Atualiza agendamento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "404", description = "Não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @PutMapping("/{id}")
    public EntityModel<Agendamento> atualizar(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long id, @Valid @RequestBody Agendamento novo) {
        return repository.findById(id).map(a -> {
            a.setDataHora(novo.getDataHora()); a.setStatus(novo.getStatus()); a.setPet(novo.getPet());
            return EntityModel.of(repository.save(a),
                linkTo(methodOn(AgendamentoController.class).buscar(id)).withSelfRel());
        }).orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    @Operation(summary = "Deleta agendamento")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removido"),
        @ApiResponse(responseCode = "401", description = "X-API-Key ausente ou inválida"),
        @ApiResponse(responseCode = "404", description = "Não encontrado"),
        @ApiResponse(responseCode = "429", description = "Limite de requisições excedido")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@RequestHeader("X-API-Key") String apiKey, @PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
