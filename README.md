# comment-service-java

Serviço de **Comment Management** da plataforma JComments, responsável pela capacidade de negócio de **Gerenciamento de Comentários**.

## Descrição

Recebe comentários enviados pelos usuários, solicita a moderação ao `ModerationService` e armazena somente os comentários aprovados. Também disponibiliza operações para consultar um comentário aprovado e listar comentários com paginação.

## Funcionalidades Principais

- Receber comentários e enviá-los para moderação
- Persistir somente comentários aprovados
- Consultar um comentário aprovado por ID
- Listar comentários aprovados com paginação
- Tratar falhas e timeouts na integração com o `ModerationService`

## Endpoints

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/comments` | Cria um comentário e solicita sua moderação |
| `GET` | `/api/comments/{id}` | Consulta um comentário aprovado pelo ID |
| `GET` | `/api/comments` | Lista comentários aprovados com paginação (`page` e `size`) |

## Objetos de Negócio

### CommentInput

| Propriedade | Tipo |
| --- | --- |
| text | String |
| author | String |

### CommentOutput

| Propriedade | Tipo |
| --- | --- |
| id | UUID |
| text | String |
| author | String |
| createdAt | Data e hora (ISO 8601) |

### Resposta Paginada

| Propriedade | Tipo |
| --- | --- |
| page | Integer |
| size | Integer |
| totalElements | Long |
| totalPages | Integer |
| content | Lista de `CommentOutput` |

## Regras e Tratamento de Respostas

- Comentários aprovados são persistidos no banco H2.
- Comentários rejeitados não são armazenados e resultam em `422 Unprocessable Entity`, com o motivo da rejeição.
- A criação de um comentário aprovado retorna `201 Created`.
- A consulta de um comentário inexistente retorna `404 Not Found`.
- Erros gerais na integração com o `ModerationService` resultam em `502 Bad Gateway`.
- Timeout na chamada ao `ModerationService` resulta em `504 Gateway Timeout`.
- O ID do comentário deve ser um UUID.

## Tecnologias Utilizadas

- Linguagem de Programação: Java
- Framework: Spring
- Comunicação HTTP síncrona: `RestClient`
- Banco de Dados: H2

## Comunicação com Outros Serviços

- **Síncrona (HTTP/REST)**: Envia uma requisição `POST` para `/api/moderate` no `ModerationService`, usando `RestClient`. A chamada deve ter read timeout de 5 segundos. O serviço aguarda a decisão da moderação antes de persistir o comentário ou retornar uma rejeição.

## Papel na Solução

Este serviço coordena o ciclo de vida dos comentários aprovados, garantindo que somente conteúdo aprovado seja armazenado e disponibilizado para consulta. Também concentra as operações de criação, consulta individual e listagem paginada, tratando de forma explícita rejeições e falhas na comunicação com o serviço de moderação.
