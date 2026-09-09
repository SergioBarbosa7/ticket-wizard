# Ticket Wizard — Fase 1 (comunicação gRPC entre microsserviços)

Sistema de venda de ingressos dividido em dois microsserviços Java que se comunicam
por gRPC, sem Spring e sem banco de dados. O estado fica em memória.

| Módulo             | Papel                                                                   |
|--------------------|-------------------------------------------------------------------------|
| `contratos-grpc`   | Contrato `ingressos.proto` e classes Java geradas pelo Protobuf/gRPC     |
| `ingressos-service`| Servidor gRPC (Serviço B), dono do domínio de categorias e ingressos     |
| `pedidos-service`  | Cliente gRPC (Serviço A), dispara a demo de cadastro + emissão           |

## Arquitetura

```text
ClientePedidosGrpc  (pedidos-service)
        |
        | gRPC :50051
        v
ServidorIngressosGrpc  (ingressos-service)
        |
        v
ConcurrentHashMap de categorias e ingressos (memória)
```

RPCs definidos em `contratos-grpc/src/main/proto/ingressos.proto`:

- `CadastrarCategoriaIngresso` — o organizador cadastra uma categoria (nome, preço, quantidade).
- `EmitirIngresso` — o comprador emite um ingresso de uma categoria já cadastrada.

Detalhes e decisões estão em [docs/arquitetura.md](docs/arquitetura.md).

## Requisitos

- JDK 21
- Não é preciso instalar o Maven: use o wrapper `./mvnw` (`mvnw.cmd` no Windows).

## Como compilar

Na raiz do projeto:

```bash
./mvnw clean install
```

O `protobuf-maven-plugin` gera os stubs a partir do `.proto` durante o `mvn compile`.
As classes geradas ficam em `contratos-grpc/target/generated-sources/protobuf/` e
**não** devem ser copiadas para `src/main/java`.

Ordem de build:

```text
ticket-wizard
contratos-grpc
ingressos-service
pedidos-service
```

## Como executar localmente

Abra dois terminais na raiz do projeto e respeite a ordem.

Terminal 1 — servidor de ingressos:

```bash
./mvnw -pl ingressos-service exec:java -Dexec.mainClass=br.com.ticket.wizard.ingressos.ServidorIngressosGrpc
```

Terminal 2 — cliente de pedidos:

```bash
./mvnw -pl pedidos-service exec:java -Dexec.mainClass=br.com.ticket.wizard.pedidos.ClientePedidosGrpc
```

Saída esperada no servidor:

```text
Servidor gRPC iniciado na porta 50051
Categoria recebida: evento=evento-001, nome=Pista, preco=100,00, quantidade=500
Emissão solicitada: evento=evento-001, usuario=user-42, categoria=<uuid>
```

Saída esperada no cliente:

```text
Chamando o ingressos-service em localhost:50051

== Cadastro de categoria ==
Categoria ID: 3f1c...-...
Status: OK
Mensagem: Categoria cadastrada com sucesso.

== Emissão de ingresso ==
Ingresso ID: 9b20...-...
Status: OK
Mensagem: Ingresso emitido: Pista do evento evento-001 por R$ 100,00. Restam 499.
```

Os UUIDs mudam a cada execução.

## Host e porta do servidor

O cliente lê o endereço do servidor da variável de ambiente
`INGRESSOS_SERVICE_HOST`, no formato `host:porta`. Sem a variável, usa
`localhost:50051`.

```bash
export INGRESSOS_SERVICE_HOST=10.128.0.5:50051   # Linux / macOS
```

```powershell
$env:INGRESSOS_SERVICE_HOST = "10.128.0.5:50051"  # Windows PowerShell
```

A porta do servidor é a constante `PORTA` em
`ingressos-service/src/main/java/br/com/ticket/wizard/ingressos/ServidorIngressosGrpc.java`.

## Cenário de erro para a apresentação

Para mostrar o tratamento de erro, altere em `ClientePedidosGrpc` a constante
`QUANTIDADE_DISPONIVEL` para `0` e recompile o módulo:

```bash
./mvnw -pl pedidos-service compile
```

A emissão passa a responder:

```text
Status: ERRO
Mensagem: Categoria esgotada.
```

Depois da demonstração, restaure `QUANTIDADE_DISPONIVEL = 500`.

## Erros comuns

| Sintoma                                       | Causa provável                                    | Ação                                                       |
|-----------------------------------------------|---------------------------------------------------|------------------------------------------------------------|
| `UNAVAILABLE: io exception`                   | Servidor parado, host errado ou porta bloqueada   | Suba o servidor, confira a variável e a regra de firewall  |
| `Connection refused`                          | Ninguém ouvindo na porta informada                | Respeite a ordem: servidor → cliente                       |
| Dependência `contratos-grpc` não encontrada   | Módulos não instalados no repositório Maven local | Rode `./mvnw clean install` na raiz                        |
| Imports `br.com.ticket.wizard.grpc` em vermelho    | IDE não recarregou o modelo Maven                 | Recarregue os projetos Maven na IDE                        |
| Erro de versão do Java                        | `java -version` não mostra 21                     | Instale ou selecione o JDK 21                              |

## Fora de escopo nesta fase

API Gateway REST (Fase 2), autenticação, persistência em banco e containerização (Fase 3).
