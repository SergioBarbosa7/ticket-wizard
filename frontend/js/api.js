import { lerSessao } from './sessao.js';
import { mascarar, mascararToken, registrar } from './inspetor.js';

let aoSessaoInvalida = () => {};

export function definirAoSessaoInvalida(funcao) {
  aoSessaoInvalida = funcao;
}

export class ErroApi extends Error {
  constructor(status, problema) {
    super(problema.detail || problema.title || 'Erro inesperado.');
    this.status = status;
    this.titulo = problema.title || '';
    this.detalhe = problema.detail || problema.title || '';
    this.erros = Array.isArray(problema.erros) ? problema.erros : [];
  }
}

function urlBase() {
  return String(window.API_URL ?? '').replace(/\/+$/, '');
}

const MENSAGENS_PADRAO = {
  401: 'Sessão inválida ou expirada.',
  403: 'Você não tem permissão para essa ação.',
  502: 'O Gateway não respondeu.',
  503: 'Serviço indisponível no momento. Tente novamente.',
  504: 'O serviço demorou demais para responder.',
};

// opcoes:
//   corpo        objeto enviado como JSON
//   autenticado  false nas rotas públicas (login e cadastro): não envia Authorization
//   token        força um token específico (usado nas simulações do inspetor)
//   rotulo       selo exibido no inspetor (ex.: "simulado")
export async function chamar(metodo, caminho, { corpo, autenticado = true, token, rotulo } = {}) {
  const tokenUsado = autenticado ? (token ?? lerSessao()?.token ?? null) : null;
  const cabecalhos = { Accept: 'application/json' };
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (tokenUsado) cabecalhos.Authorization = `Bearer ${tokenUsado}`;

  const entrada = {
    metodo,
    caminho,
    rotulo,
    autorizacao: tokenUsado ? `Bearer ${mascararToken(tokenUsado)}` : null,
    enviado: corpo === undefined ? undefined : mascarar(corpo),
  };
  const inicio = performance.now();

  let resposta;
  let texto;
  try {
    resposta = await fetch(urlBase() + caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
    texto = await resposta.text();
  } catch {
    registrar({ ...entrada, status: 0, ms: Math.round(performance.now() - inicio),
                recebido: { detail: 'Sem conexão com o Gateway.' } });
    throw new ErroApi(0, { title: 'Sem conexão', detail: 'Não foi possível falar com o Gateway. Verifique a conexão.' });
  }

  let dados = null;
  try {
    dados = texto ? JSON.parse(texto) : null;
  } catch {
    dados = null;
  }

  registrar({ ...entrada, status: resposta.status, ms: Math.round(performance.now() - inicio),
              recebido: mascarar(dados) });

  if (!resposta.ok) {
    // 401 numa rota autenticada com o token da sessão = token vencido ou inválido.
    // As simulações do inspetor passam token próprio e não derrubam a sessão.
    if (resposta.status === 401 && autenticado && token === undefined) {
      aoSessaoInvalida();
    }
    const problema = dados && typeof dados === 'object' ? dados : {};
    if (!problema.detail && !problema.title) {
      problema.detail = MENSAGENS_PADRAO[resposta.status] || `Erro ${resposta.status}.`;
    }
    throw new ErroApi(resposta.status, problema);
  }
  return dados;
}
