#!/usr/bin/env bash
#   ./infra/demo.sh                      # Gateway em http://localhost:8080
#   BASE_URL=http://IP-DA-VM:8080 ./infra/demo.sh
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
SUFIXO=$(date +%s)
SENHA='senha-segura-123'

chamar() {
  local rotulo=$1 metodo=$2 caminho=$3 token=${4:-} corpo=${5:-}
  local args=(-s -o /tmp/demo-corpo.$$ -w '%{http_code}' -X "$metodo" "$BASE_URL$caminho")
  [[ -n $token ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n $corpo ]] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  STATUS=$(curl "${args[@]}")
  CORPO=$(cat /tmp/demo-corpo.$$)
  printf '\n\033[1m%s\033[0m\n  %s %s  ->  HTTP %s\n' "$rotulo" "$metodo" "$caminho" "$STATUS"
  local cor=()
  [[ -t 1 ]] && cor=(-C)
  { jq "${cor[@]}" . <<<"$CORPO" 2>/dev/null || echo "$CORPO"; } | sed 's/^/  /'
}

esperar() {
  if [[ $STATUS != "$1" ]]; then
    printf '\033[31m  FALHOU: esperava HTTP %s e veio %s\033[0m\n' "$1" "$STATUS" >&2
    exit 1
  fi
}
trap 'rm -f /tmp/demo-corpo.$$' EXIT

chamar '1. Sem token: barrado na borda' GET /api/categorias
esperar 401

chamar '2. Cadastro com payload inválido' POST /api/auth/register '' '{"nome":"","email":"nao-e-email","senha":"123"}'
esperar 400

chamar '3. Cadastra organizador' POST /api/auth/register '' \
  "{\"nome\":\"Olivia Organizadora\",\"email\":\"olivia.$SUFIXO@exemplo.com\",\"senha\":\"$SENHA\",\"papel\":\"ORGANIZADOR\"}"
esperar 201
chamar '4. Cadastra comprador' POST /api/auth/register '' \
  "{\"nome\":\"Caio Comprador\",\"email\":\"caio.$SUFIXO@exemplo.com\",\"senha\":\"$SENHA\",\"papel\":\"COMPRADOR\"}"
esperar 201
chamar '5. E-mail repetido' POST /api/auth/register '' \
  "{\"nome\":\"Outro\",\"email\":\"caio.$SUFIXO@exemplo.com\",\"senha\":\"$SENHA\",\"papel\":\"COMPRADOR\"}"
esperar 409

chamar '6. Login com senha errada' POST /api/auth/login '' "{\"email\":\"caio.$SUFIXO@exemplo.com\",\"senha\":\"errada-errada\"}"
esperar 401
chamar '7. Login do organizador' POST /api/auth/login '' "{\"email\":\"olivia.$SUFIXO@exemplo.com\",\"senha\":\"$SENHA\"}"
esperar 200
TOKEN_ORG=$(jq -r .token <<<"$CORPO")
chamar '8. Login do comprador' POST /api/auth/login '' "{\"email\":\"caio.$SUFIXO@exemplo.com\",\"senha\":\"$SENHA\"}"
esperar 200
TOKEN_COMP=$(jq -r .token <<<"$CORPO")

chamar '9. Token adulterado' GET /api/categorias "${TOKEN_COMP}x"
esperar 401

chamar '10. Comprador tenta cadastrar categoria' POST /api/categorias "$TOKEN_COMP" \
  '{"eventoId":"show","nome":"Pista","precoCentavos":10000,"quantidade":3}'
esperar 403
chamar '11. Categoria sem campos obrigatórios' POST /api/categorias "$TOKEN_ORG" '{"nome":"Pista"}'
esperar 400
chamar '12. Organizador cadastra categoria (3 vagas)' POST /api/categorias "$TOKEN_ORG" \
  "{\"eventoId\":\"show-$SUFIXO\",\"nome\":\"Pista\",\"precoCentavos\":10000,\"quantidade\":3}"
esperar 201
CATEGORIA=$(jq -r .id <<<"$CORPO")

chamar '13. Lista categorias' GET /api/categorias "$TOKEN_COMP"
esperar 200

chamar '14. Pedido com quantidade fora do limite' POST /api/pedidos "$TOKEN_COMP" "{\"categoriaId\":\"$CATEGORIA\",\"quantidade\":11}"
esperar 400
chamar '15. Comprador compra 2 ingressos' POST /api/pedidos "$TOKEN_COMP" "{\"categoriaId\":\"$CATEGORIA\",\"quantidade\":2}"
esperar 201
chamar '16. Pede 2 com 1 restante: estoque insuficiente' POST /api/pedidos "$TOKEN_COMP" "{\"categoriaId\":\"$CATEGORIA\",\"quantidade\":2}"
esperar 409
chamar '17. Categoria inexistente' POST /api/pedidos "$TOKEN_COMP" '{"categoriaId":"00000000-0000-0000-0000-000000000000","quantidade":1}'
esperar 404

chamar '18. Meus pedidos (o confirmado e os que falharam, todos no banco)' GET /api/pedidos "$TOKEN_COMP"
esperar 200
chamar '19. Meus ingressos (vindos do banco)' GET /api/ingressos "$TOKEN_COMP"
esperar 200

printf '\n\033[32mOK: todos os status esperados.\033[0m\n'
