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
- **Quantidade de ingressos por argumento, não por constante.** O cliente lê
  `-Dexec.args` para saber quantos ingressos emitir e com que quantidade
  cadastrar a categoria, então o cenário de erro na apresentação não exige
  editar código nem recompilar.
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

1. `ClientePedidosGrpc` lê os argumentos da execução (quantos ingressos emitir e
   qual a quantidade da categoria), abre o canal e cria o stub bloqueante.
2. Chama `CadastrarCategoriaIngresso` com "Pista", R$ 100,00 e a quantidade
   informada. O servidor gera um `categoria_id` (UUID) e guarda a categoria no mapa.
3. Chama `EmitirIngresso` uma vez por ingresso pedido, sempre com o `categoria_id`
   devolvido no passo anterior. A cada chamada o servidor valida a categoria,
   decrementa a quantidade disponível, gera um `ingresso_id` e devolve
   `status = "OK"`. Quando a quantidade zera, as chamadas seguintes voltam com
   `status = "ERRO"` e a mensagem "Categoria esgotada.".
4. O cliente imprime cada resposta e fecha o canal ao final.

A emissão em lote é um laço no cliente, não um RPC de lote: são N chamadas
unárias independentes. Isso mantém o contrato `.proto` do plano intacto e
deixa visível, na demonstração, o estado do servidor mudando entre as chamadas.

## Portas

| Serviço             | Porta   | Onde alterar                            |
|---------------------|---------|-----------------------------------------|
| `ingressos-service` | `50051` | constante `PORTA` em `ServidorIngressosGrpc` |
| `pedidos-service`   | —       | é apenas cliente nesta fase             |

A porta `50051/TCP` precisa de uma regra de firewall VPC liberando o tráfego
entre as VMs no GCP.
