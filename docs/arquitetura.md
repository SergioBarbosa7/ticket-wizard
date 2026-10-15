# Arquitetura — Fase 2

## Visão geral

Quatro processos Java e um frontend estático, ligados assim:

```text
Navegador ──HTTP/JSON──> frontend (nginx) ──/api──> api-gateway (Spring Boot)
                                                         │ gRPC (Protobuf sobre HTTP/2)
                              ┌──────────────────────────┼───────────────────────┐
                              v                          v                       v
                      usuarios-service           pedidos-service ──gRPC──> ingressos-service
                              │                          │                       │
                         usuarios_db                pedidos_db              ingressos_db
                                         (PostgreSQL 16, um banco por serviço)
```

## Responsabilidades

| Componente          | Faz                                                                                   | Não faz                          |
|---------------------|---------------------------------------------------------------------------------------|----------------------------------|
| `frontend`          | Telas, guarda o token em `sessionStorage`, chama `/api/*`                             | Falar com gRPC ou com o banco    |
| `api-gateway`       | JWT, validação do payload, status HTTP, JSON ⇄ Protobuf, escolhe o serviço de destino | Regra de negócio, banco          |
| `usuarios-service`  | Cadastro, hash de senha, autenticação de credenciais, papel                           | Emitir o token                   |
| `ingressos-service` | Categorias, estoque, emissão de ingressos                                             | Saber de pedidos                 |
| `pedidos-service`   | Registrar e fechar pedidos; chama o `ingressos-service` para emitir                   | Ser dono do estoque              |

O `usuarios-service` existe porque a autenticação precisa de usuários guardados em banco real;
um usuário fixo no Gateway seria dado estático. Assim o Gateway fica sem estado e sem banco.

## Decisões

- **Banco por serviço, mesma instância.** Cada serviço só conhece o seu banco. Migrações Flyway
  (`src/main/resources/db/migration` de cada serviço) rodam na subida.
- **JDBC puro + HikariCP.** Um repositório por agregado (`CategoriaRepositorio`, `PedidoRepositorio`...),
  recebendo a `Connection` do chamador; a transação é aberta na camada de serviço (`Transacao.executar`).
  Mantém o estilo da Fase 1 (sem framework nos serviços) e deixa a camada de dados explícita.
- **Erro como status gRPC.** Na Fase 1 o erro voltava dentro da mensagem (`status = "ERRO"`). Agora o serviço
  lança `INVALID_ARGUMENT`, `NOT_FOUND`, `FAILED_PRECONDITION`..., e o Gateway traduz para HTTP. Um interceptor
  (`TratadorDeExcecoes`) converte as exceções em respostas gRPC; qualquer exceção inesperada vira `INTERNAL`
  sem vazar detalhe ao cliente.
- **Dinheiro em centavos (`int64`)**, nunca `double`; combina com `BIGINT` no banco.
- **Estoque sem corrida.** A baixa é um único `UPDATE ... SET quantidade_disponivel = quantidade_disponivel - n
  WHERE id = ? AND quantidade_disponivel >= n RETURNING ...`, na mesma transação que insere os ingressos. Além
  disso, um `CHECK` no banco impede estoque negativo.
- **JWT no Gateway.** HS256, claims `sub` (id), `papel`, `iss`, `exp`. Filtro de servlet (`FiltroJwt`) antes dos
  controllers: sem token, malformado, expirado, chave errada ou `alg: none` → 401, sem tocar nos serviços.
  Só `/api/auth/**` é público. A chave vem de `JWT_SECRET` e o Gateway não sobe com menos de 32 caracteres.
- **Autorização por papel.** `POST /api/categorias` exige `ORGANIZADOR` (403 caso contrário).
- **Validação em duas camadas.** Bean Validation nos DTOs do Gateway (400 com a lista de campos) e de novo nos
  serviços, que são donos das invariantes.
- **Senha.** PBKDF2-HMAC-SHA256 (600 000 iterações) do JDK, sal aleatório, comparação em tempo constante. Quando o
  e-mail não existe, a senha é conferida contra um hash falso, para login inexistente e senha errada terem a mesma
  resposta e o mesmo tempo.
- **Prazo em toda chamada gRPC** (15 s do Gateway, 10 s do pedidos ao ingressos), calculado por chamada.
- **Pedido entre dois bancos, sem transação distribuída.** `PENDENTE` → chamada ao `ingressos-service` →
  `CONFIRMADO` ou `FALHOU`. Recusa de negócio (estoque, categoria inexistente) fecha como `FALHOU` com o motivo;
  falha de infraestrutura (timeout, serviço fora do ar) deixa `PENDENTE`, porque não dá para saber se emitiu.
- **Segredos fora do repositório.** `.env` está no `.gitignore`; só o `.env.example` é versionado. A senha do banco
  não tem valor padrão no código.

## Contratos

`contratos-grpc/src/main/proto/`

| Arquivo           | Serviço          | RPCs                                                                         |
|-------------------|------------------|------------------------------------------------------------------------------|
| `usuarios.proto`  | `UsuarioService` | `Cadastrar`, `Autenticar`                                                    |
| `ingressos.proto` | `IngressoService`| `CadastrarCategoriaIngresso`, `ListarCategorias`, `EmitirIngressos`, `ListarIngressos` |
| `pedidos.proto`   | `PedidoService`  | `RealizarPedido`, `ListarPedidos`                                            |

Todos os RPCs são unários. As classes Java são geradas em `contratos-grpc/target/generated-sources/protobuf/`
durante o build e **não** são copiadas para `src/`.

## Fluxo: comprar ingressos

1. O navegador envia `POST /api/pedidos` com `{categoriaId, quantidade}` e o Bearer token.
2. O `FiltroJwt` valida o token e guarda o usuário no request; os DTOs são validados (400 se inválidos).
3. `PedidosClient` converte o DTO em `RealizarPedidoRequest` (Protobuf) e chama `pedidos-service` por gRPC.
4. O `pedidos-service` grava o pedido `PENDENTE` em `pedidos_db` e chama `ingressos-service.EmitirIngressos`.
5. O `ingressos-service`, numa transação em `ingressos_db`, baixa o estoque e insere os ingressos.
6. O `pedidos-service` grava `CONFIRMADO` com os ids dos ingressos e responde.
7. O Gateway converte a resposta Protobuf em JSON e devolve **201**. Se algum serviço respondeu erro, o status
   gRPC vira o HTTP equivalente (409 estoque, 404 categoria, 503 serviço fora do ar...).

## Esquema dos bancos

```sql
-- usuarios_db
usuarios(id uuid PK, nome, email UNIQUE, senha_hash, papel CHECK IN ('COMPRADOR','ORGANIZADOR'), criado_em)

-- ingressos_db
categorias(id uuid PK, evento_id, nome, preco_centavos CHECK > 0, quantidade_total,
           quantidade_disponivel CHECK entre 0 e total, criado_por, criado_em, UNIQUE (evento_id, nome))
ingressos(id uuid PK, categoria_id FK, usuario_id, pedido_id, emitido_em)

-- pedidos_db
pedidos(id uuid PK, usuario_id, categoria_id, evento_id, nome_categoria, quantidade,
        valor_total_centavos, status CHECK IN ('PENDENTE','CONFIRMADO','FALHOU'), motivo_falha, criado_em)
pedido_ingressos(pedido_id FK, ingresso_id, PK (pedido_id, ingresso_id))
```

`pedidos` guarda `evento_id` e `nome_categoria` copiados na compra, porque o dono desses dados é o
`ingressos-service`.

## Portas

| Serviço             | Porta   | Exposição                                    |
|---------------------|---------|----------------------------------------------|
| `frontend` (nginx)  | `80`    | host / internet                              |
| `api-gateway`       | `8080`  | host / internet                              |
| `ingressos-service` | `50051` | rede interna do Compose                      |
| `pedidos-service`   | `50052` | rede interna do Compose                      |
| `usuarios-service`  | `50053` | rede interna do Compose                      |
| PostgreSQL          | `5432`  | rede interna; `127.0.0.1:5433` no host       |
