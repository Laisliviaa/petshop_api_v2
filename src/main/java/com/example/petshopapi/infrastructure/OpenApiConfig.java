package com.example.petshopapi.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PetShop API",
                version = "2.0.0",
                description = """
                        Sistema completo para gestão de um PetShop.

                        ## Parte I — Fundamentos REST
                        - Projeto Maven com Spring Boot, JPA/Hibernate e H2
                        - CRUD REST para Clientes, Pets, Agendamentos, Serviços, Unidades e Gerentes
                        - Respostas paginadas com Spring Data Pageable e links HATEOAS
                        - Relacionamentos JPA: One-to-One (Gerente↔Unidade), One-to-Many (Cliente→Pets), Many-to-Many (Pet↔Serviços)
                        - Enum StatusAgendamento: PENDENTE, CONCLUIDO, CANCELADO
                        - Validação com Bean Validation nos modelos
                        - Consultas personalizadas e tratamento global de erros

                        ---

                        ## Parte II — Autenticação com X-API-Key (HTTP 401)

                        Operações de escrita (POST, PUT, DELETE) exigem o header **X-API-Key**.
                        GET é público. POST em `/api/v1/apikeys` é público para geração inicial.

                        **Chaves padrão:**
                        - `petshop-admin-key-2026` → role ADMIN (revogar chaves de outros)
                        - `petshop-user-key-2026` → role USER

                        **Fluxo:**
                        1. Gere sua chave em `POST /api/v1/apikeys`
                        2. Use a chave no header `X-API-Key` em todas as operações de escrita
                        3. Clique em **Authorize** (🔒) e cole a chave

                        ---

                        ## Parte II — Rate Limiting (HTTP 429)

                        | Operação | Limite |
                        |---|---|
                        | GET | 10 requisições por minuto por IP |
                        | POST / PUT / DELETE | 5 requisições por minuto por IP |

                        Ao exceder: **429 Too Many Requests** com header `Retry-After`.

                        ---

                        ## Parte II — Idempotência (HTTP 409)

                        Envie **X-Idempotency-Key** no header em operações POST.
                        - Mesma chave + mesmo JSON → retorna resposta original sem reprocessar
                        - Mesma chave + JSON diferente → **409 Conflict**

                        ---

                        ## Parte II — CORS

                        A API aceita requisições cross-origin e libera os headers `X-API-Key` e `X-Idempotency-Key`.
                        Expõe os headers de rate limit e autenticação para clientes web.

                        ---

                        ## Parte II — Versionamento por Header (HTTP 400 para versão inválida)

                        O versionamento é feito via header **X-API-Version** (não por URL).

                        | Versão | Comportamento |
                        |---|---|
                        | `1` | Resposta padrão com HATEOAS *(padrão quando ausente)* |
                        | `2` | Resposta enriquecida com `totalPets`, metadados extras |

                        Versão inválida → **400 Bad Request**. O header `X-API-Version` é ecoado na resposta.

                        ---

                        ## Tratamento de Erros

                        Erros seguem o contrato `ApiErrorResponse` com `timestamp`, `status`, `erro`, `mensagem`, `caminho`, `metodo` e `detalhes`.

                        | Código | Significado |
                        |---|---|
                        | 400 | Dados inválidos ou versão inválida |
                        | 401 | X-API-Key ausente ou inválida |
                        | 403 | Permissão insuficiente (USER tentando revogar chaves) |
                        | 404 | Recurso não encontrado |
                        | 409 | Idempotency-Key reutilizada com payload diferente |
                        | 429 | Limite de requisições excedido |
                        | 500 | Erro interno do servidor |

                        ---

                        ## ⚡ Dados pré-carregados

                        - **3 unidades** com gerentes vinculados
                        - **5 serviços**: Banho e Tosa, Consulta Veterinária, Vacinação, Adestramento, Hospedagem
                        - **4 clientes** com CPF
                        - **6 pets** com serviços vinculados
                        - **5 agendamentos** com status variados
                        """,
                contact = @Contact(name = "PetShop API Team", email = "contato@petshop.com")
        ),
        security = @SecurityRequirement(name = "ApiKeyAuth")
)
@SecurityScheme(
        name = "ApiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "Cole sua X-API-Key. Padrão: petshop-admin-key-2026"
)
public class OpenApiConfig {}
