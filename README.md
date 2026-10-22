# Ticket Wizard — Fase 2 (Frontend, API Gateway, gRPC e PostgreSQL)

Sistema de venda de ingressos. O navegador fala HTTP/JSON com um API Gateway (Spring Boot),
que autentica por JWT, valida o payload e despacha para microsserviços Java por gRPC. Cada
microsserviço persiste em seu próprio banco PostgreSQL.

| Módulo              | Papel                                                                          |
|---------------------|--------------------------------------------------------------------------------|
| `frontend`          | Interface web (HTML/CSS/JS puros) servida por nginx; só chama `/api/*`          |
| `api-gateway`       | Spring Boot: filtro JWT, validação, status HTTP, tradução JSON ⇄ Protobuf       |
| `usuarios-service`  | gRPC: cadastro e autenticação (hash PBKDF2), papéis                            |
| `ingressos-service` | gRPC: categorias, estoque e emissão de ingressos                               |
| `pedidos-service`   | gRPC: pedidos; é cliente gRPC do `ingressos-service`                           |
| `contratos-grpc`    | Contratos `.proto` e classes geradas (`usuarios`, `ingressos`, `pedidos`)      |
| `base-servicos`     | Infra comum dos serviços: servidor gRPC, pool + Flyway, transação, erros       |

## Integrantes

| Nome                          | Matrícula   |
|-------------------------------|-------------|
| Sérgio Natan Costa Barbosa    | 202006577   |
| Guilherme dos Santos Silva    | 202011563   |

## Arquitetura

```text
Navegador
   |  HTTP/JSON  (Authorization: Bearer <jwt>)
   v
frontend (nginx :80)           arquivos estáticos + proxy de /api/ para o Gateway
   |
   v
api-gateway (Spring Boot :8080)   filtro JWT · validação · JSON ⇄ Protobuf
   |                 |                       |
   | gRPC :50053     | gRPC :50052           | gRPC :50051
   v                 v                       v
usuarios-service   pedidos-service ──gRPC──> ingressos-service
   |                 |                       |
   v                 v                       v
usuarios_db        pedidos_db              ingressos_db      (PostgreSQL 16, um banco por serviço)
```

- O frontend nunca fala com os serviços gRPC nem com o banco.
- Nenhum serviço lê o banco de outro: quem precisa do dado alheio chama o dono por gRPC.
- O Gateway **emite** o JWT depois que o `usuarios-service` confirma as credenciais.
- Mais detalhes e decisões em [docs/arquitetura.md](docs/arquitetura.md).

## API REST (Gateway)

Todas as rotas, exceto `/api/auth/*`, exigem `Authorization: Bearer <token>`.

| Método e rota          | Auth                | Sucesso | Erros                      |
|------------------------|---------------------|---------|----------------------------|
| `POST /api/auth/register` | pública          | 201     | 400, 409 (e-mail repetido) |
| `POST /api/auth/login`    | pública          | 200     | 400, 401                   |
| `GET  /api/categorias`    | JWT              | 200     | 401                        |
| `POST /api/categorias`    | JWT, `ORGANIZADOR` | 201   | 400, 401, 403, 409         |
| `POST /api/pedidos`       | JWT              | 201     | 400, 401, 404, 409, 503    |
| `GET  /api/pedidos`       | JWT (só os do próprio usuário) | 200 | 401         |
| `GET  /api/ingressos`     | JWT (só os do próprio usuário) | 200 | 401         |

O usuário de um pedido vem sempre do token (`sub`), nunca do corpo da requisição.
Erros saem como `application/problem+json`, com a lista `erros: [{campo, mensagem}]` nos 400.

Status gRPC dos serviços traduzidos pelo Gateway:

| Status gRPC           | HTTP |
|-----------------------|------|
| `INVALID_ARGUMENT`    | 400  |
| `UNAUTHENTICATED`     | 401  |
| `PERMISSION_DENIED`   | 403  |
| `NOT_FOUND`           | 404  |
| `ALREADY_EXISTS`, `FAILED_PRECONDITION` (estoque) | 409 |
| `UNAVAILABLE`         | 503  |
| `DEADLINE_EXCEEDED`   | 504  |

## Como executar (Docker Compose)

Requisitos: Docker com Compose v2.

```bash
cp .env.example .env        # troque POSTGRES_PASSWORD e JWT_SECRET (mín. 32 caracteres)
docker compose up --build
```

| O quê                | Onde                                   |
|----------------------|----------------------------------------|
| Interface            | http://localhost (`FRONTEND_PORT`)     |
| API Gateway          | http://localhost:8080 (`GATEWAY_PORT`) |
| PostgreSQL (loopback)| `127.0.0.1:5433` (`POSTGRES_HOST_PORT`), usuário `ticket` |

Os serviços gRPC (50051–50053) ficam só na rede interna do Compose.

### Demonstração e prova de persistência

```bash
./infra/demo.sh                              # percorre 400, 401, 403, 404, 409, 201 e 200
BASE_URL=http://IP-DA-VM:8080 ./infra/demo.sh
```

Para mostrar os registros no banco:

```bash
docker compose exec postgres psql -U ticket -d ingressos_db -c "select nome, quantidade_total, quantidade_disponivel from categorias"
docker compose exec postgres psql -U ticket -d pedidos_db   -c "select status, quantidade, valor_total_centavos, motivo_falha from pedidos"
docker compose exec postgres psql -U ticket -d usuarios_db  -c "select nome, email, papel from usuarios"
```

Os dados sobrevivem a `docker compose restart`. Para zerar tudo: `docker compose down -v`.

## Como executar sem Docker (desenvolvimento)

Requisitos: JDK 21 e um PostgreSQL com os bancos `usuarios_db`, `ingressos_db` e `pedidos_db`
(o script `infra/postgres/init/01-criar-bancos.sql` os cria). As tabelas vêm das migrações Flyway,
aplicadas na subida de cada serviço.

```bash
./mvnw clean install

export DB_PASSWORD=...                       # obrigatória; DB_URL e DB_USER têm padrão (localhost, "ticket")
./mvnw -pl usuarios-service  exec:java -Dexec.mainClass=br.com.ticket.wizard.usuarios.ServidorUsuariosGrpc
./mvnw -pl ingressos-service exec:java -Dexec.mainClass=br.com.ticket.wizard.ingressos.ServidorIngressosGrpc
./mvnw -pl pedidos-service   exec:java -Dexec.mainClass=br.com.ticket.wizard.pedidos.ServidorPedidosGrpc

export JWT_SECRET=...                        # mínimo 32 caracteres
./mvnw -pl api-gateway spring-boot:run
```

Variáveis de ambiente:

| Variável                 | Quem usa     | Padrão                          |
|--------------------------|--------------|---------------------------------|
| `DB_PASSWORD`            | serviços     | — (obrigatória)                 |
| `DB_URL`, `DB_USER`      | serviços     | `jdbc:postgresql://localhost:5432/<banco>`, `ticket` |
| `GRPC_PORT`              | serviços     | 50053 / 50051 / 50052           |
| `INGRESSOS_SERVICE_HOST` | pedidos, gateway | `localhost:50051`           |
| `USUARIOS_SERVICE_HOST`, `PEDIDOS_SERVICE_HOST` | gateway | `localhost:50053`, `localhost:50052` |
| `JWT_SECRET`             | gateway      | — (obrigatória, ≥ 32 caracteres) |
| `JWT_EXPIRACAO_SEGUNDOS` | gateway      | 3600                            |
| `CORS_ORIGINS`           | gateway      | `http://localhost:*`            |

Para o frontend fora do nginx, sirva `frontend/` por um servidor estático e defina
`window.API_URL` em `frontend/js/config.js` (o Gateway libera CORS para localhost).

## Nuvem (GCP): duas VMs

```text
                 internet
                    │  tcp:80 (e 8080, opcional)
        ┌───────────▼────────────┐              ┌─────────────────────────────┐
        │ VM-A  "borda"          │  gRPC        │ VM-B  "interna"             │
        │ frontend (nginx)       │  :50051-3    │ usuarios-service  :50053    │
        │ api-gateway            ├─────────────▶│ ingressos-service :50051    │
        │ (tem JWT_SECRET)       │  IP interno  │ pedidos-service   :50052    │
        └────────────────────────┘  da VPC      │ postgres          :5432     │
                                                │ (tem POSTGRES_PASSWORD)     │
                                                └─────────────────────────────┘
```

| Arquivo               | VM       | Sobe                                         | Variáveis (`.env`)                    |
|-----------------------|----------|----------------------------------------------|---------------------------------------|
| `vm-interna.yml`      | VM-B     | PostgreSQL + `usuarios`, `ingressos`, `pedidos` | `.env.interna` (`POSTGRES_PASSWORD`) |
| `vm-borda.yml`        | VM-A     | `api-gateway` + `frontend`                   | `.env.borda` (`JWT_SECRET`, `VM_INTERNA_IP`) |

Cada VM só recebe o segredo de que precisa: o `JWT_SECRET` nunca está na máquina do banco.
O `docker-compose.yml` da raiz continua sendo o modo "tudo numa máquina".

### 1. Rede e firewall

Crie uma VPC própria. Na VPC `default` existe a regra `default-allow-internal`, que libera
**todas** as portas entre as VMs (inclusive a 5432) e anularia a separação. Numa VPC nova, todo
tráfego de entrada é negado até haver uma regra que o permita.

```bash
REGIAO=southamerica-east1
ZONA=southamerica-east1-b

gcloud compute networks create ticket-vpc --subnet-mode=custom
gcloud compute networks subnets create ticket-subnet \
  --network=ticket-vpc --region=$REGIAO --range=10.10.0.0/24

# Internet -> VM-A (só a interface web e, opcionalmente, o Gateway direto)
gcloud compute firewall-rules create ticket-allow-web \
  --network=ticket-vpc --direction=INGRESS --action=ALLOW \
  --rules=tcp:80,tcp:8080 --source-ranges=0.0.0.0/0 --target-tags=ticket-edge

# VM-A -> VM-B (somente gRPC; a 5432 NÃO é aberta)
gcloud compute firewall-rules create ticket-allow-grpc-interno \
  --network=ticket-vpc --direction=INGRESS --action=ALLOW \
  --rules=tcp:50051-50053 --source-tags=ticket-edge --target-tags=ticket-internal

# SSH só pelo IAP do Google
gcloud compute firewall-rules create ticket-allow-ssh-iap \
  --network=ticket-vpc --direction=INGRESS --action=ALLOW \
  --rules=tcp:22 --source-ranges=35.235.240.0/20 --target-tags=ticket-edge,ticket-internal
```

### 2. Criar as VMs

O IP interno da VM-B é fixado em `10.10.0.10`. Cada VM leva a tag que as regras acima usam.

```bash
gcloud compute instances create vm-interna --zone=$ZONA --machine-type=e2-standard-2 \
  --subnet=ticket-subnet --private-network-ip=10.10.0.10 --tags=ticket-internal \
  --image-family=debian-12 --image-project=debian-cloud --boot-disk-size=30GB

gcloud compute instances create vm-borda --zone=$ZONA --machine-type=e2-medium \
  --subnet=ticket-subnet --tags=ticket-edge \
  --image-family=debian-12 --image-project=debian-cloud --boot-disk-size=20GB
```

As duas ficam com IP externo efêmero, para baixar imagens e dependências no build; o firewall
é o que impede o acesso da internet à VM-B. Para endurecer depois, remova o IP externo da
`vm-interna` e use Cloud NAT.

### 3. Instalar Docker e baixar o código (nas duas VMs)

```bash
gcloud compute ssh vm-interna --zone=$ZONA --tunnel-through-iap   # depois repita com vm-borda

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && newgrp docker

sudo apt-get install -y git
git clone <URL-DO-REPOSITORIO> ticket-wizard && cd ticket-wizard
git checkout fase-2-gateway-web
```

### 4. Subir a VM-B (interna), primeiro

```bash
cp .env.interna.example .env.interna
sed -i "s/^POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$(openssl rand -hex 16)/" .env.interna

docker compose -f vm-interna.yml --env-file .env.interna up -d --build
docker compose -f vm-interna.yml --env-file .env.interna ps
```

### 5. Subir a VM-A (borda)

```bash
cp .env.borda.example .env.borda
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')|" .env.borda
# VM_INTERNA_IP já vem como 10.10.0.10; confira com: gcloud compute instances list

docker compose -f vm-borda.yml --env-file .env.borda up -d --build
```

A interface fica em `http://<IP-EXTERNO-DA-VM-A>` (`gcloud compute instances list`).

### 6. Verificar o firewall (bom para mostrar na apresentação)

```bash
# Da VM-A: deve conectar (regra ticket-allow-grpc-interno)
nc -zv 10.10.0.10 50053

# Da VM-A: deve FALHAR (a 5432 não tem regra)
nc -zv -w 3 10.10.0.10 5432

# Do seu computador, contra o IP externo da VM-B: deve FALHAR (timeout)
nc -zv -w 3 <IP-EXTERNO-DA-VM-B> 50053

# Fluxo completo pela borda
BASE_URL=http://<IP-EXTERNO-DA-VM-A>:8080 ./infra/demo.sh
```

Os registros no banco, para a demonstração, são consultados **na VM-B**:

```bash
docker compose -f vm-interna.yml --env-file .env.interna exec postgres \
  psql -U ticket -d pedidos_db -c "select status, quantidade, valor_total_centavos from pedidos"
```

### 7. Encerrar (evita cobrança)

```bash
gcloud compute instances delete vm-interna vm-borda --zone=$ZONA
gcloud compute firewall-rules delete ticket-allow-web ticket-allow-grpc-interno ticket-allow-ssh-iap
gcloud compute networks subnets delete ticket-subnet --region=$REGIAO
gcloud compute networks delete ticket-vpc
```

> Os dois arquivos Compose foram validados simulando as duas VMs como dois projetos na mesma máquina
> (o Gateway falando com os serviços pelo IP do host). Os comandos `gcloud` não foram executados.

## Limitações conhecidas

- Não há transação distribuída entre `pedidos_db` e `ingressos_db`. O pedido é gravado como `PENDENTE`,
  o `ingressos-service` emite, e então o pedido vira `CONFIRMADO` (ou `FALHOU` se o estoque recusar).
  Se o `pedidos-service` cair entre a emissão e a confirmação, o pedido fica `PENDENTE` com ingressos
  emitidos; não há reconciliação automática.
- Sem limite de tentativas no login e sem renovação de token (expira em 1 hora).
- O papel (`COMPRADOR`/`ORGANIZADOR`) é escolhido no cadastro; qualquer pessoa pode se declarar organizadora.
- Os três bancos usam o mesmo usuário PostgreSQL (`ticket`); a separação é por banco, não por credencial.
- O Gateway e os serviços falam gRPC sem TLS: dentro da rede do Compose, ou em texto puro pela VPC entre as duas VMs.
