# Arquitetura — Fase 1

## Visão geral

Dois microsserviços Java conversando por gRPC sobre HTTP/2, com mensagens
serializadas em Protocol Buffers.

```text
pedidos-service (cliente gRPC)
        |
        | IngressoService :50051
        v
ingressos-service (servidor gRPC)
        |
        v
ConcurrentHashMap: categorias e ingressos
```

## Decisões desta fase

- **Três módulos Maven, não dois projetos separados.** O `.proto` vive uma única vez
  em `contratos-grpc`, que gera as classes usadas pelos dois serviços. Isso evita
  manter duas cópias do contrato em sincronia. Cada serviço continua sendo um
  artefato próprio, executável e empacotável de forma independente na Fase 3.
- **Estado em memória.** `ConcurrentHashMap` no `IngressoServiceImpl`. Reiniciar o
  servidor apaga tudo; persistência fica para uma fase posterior.
- **Erros de negócio na resposta, não como exceção.** Categoria inexistente ou
  esgotada devolve `status = "ERRO"` com mensagem explicativa, em vez de
  `StatusRuntimeException`. Mantém a demo simples e legível no terminal.
- **Endereço do servidor por variável de ambiente.** O cliente lê
  `INGRESSOS_SERVICE_HOST` (padrão `localhost:50051`), então mudar de localhost
  para o IP interno da VM no GCP não exige recompilar.
- **Sem Spring, sem framework web.** Só `grpc-java` e Java puro, como nas aulas.

## Contrato

`contratos-grpc/src/main/proto/ingressos.proto`

| RPC                          | Request            | Response            |
|------------------------------|--------------------|---------------------|
| `CadastrarCategoriaIngresso` | `CategoriaRequest` | `CategoriaResponse` |
| `EmitirIngresso`             | `PedidoRequest`    | `IngressoResponse`  |

Ambos são RPCs unários: uma requisição, uma resposta.

## Fluxo da demo

1. `ClientePedidosGrpc` abre o canal e cria o stub bloqueante.
2. Chama `CadastrarCategoriaIngresso` com "Pista", R$ 100,00 e 500 unidades.
   O servidor gera um `categoria_id` (UUID) e guarda a categoria no mapa.
3. Chama `EmitirIngresso` com o `categoria_id` devolvido no passo anterior.
   O servidor valida a categoria, decrementa a quantidade disponível, gera um
   `ingresso_id` e devolve `status = "OK"`.
4. O cliente imprime as duas respostas e fecha o canal.

## Portas

| Serviço             | Porta   | Onde alterar                            |
|---------------------|---------|-----------------------------------------|
| `ingressos-service` | `50051` | constante `PORTA` em `ServidorIngressosGrpc` |
| `pedidos-service`   | —       | é apenas cliente nesta fase             |

A porta `50051/TCP` precisa de uma regra de firewall VPC liberando o tráfego
entre as VMs no GCP.
