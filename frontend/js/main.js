import { chamar, definirAoSessaoInvalida } from './api.js';
import { iniciarAuth, mostrarAba, mostrarAviso } from './auth.js';
import { renderCatalogo } from './catalogo.js';
import { renderCategoria } from './categoria.js';
import { el, esvaziar } from './dom.js';
import { renderIngressos } from './ingressos.js';
import { iniciarInspetor } from './inspetor.js';
import { renderPedidos } from './pedidos.js';
import { limparSessao, lerSessao } from './sessao.js';

const ROTAS = {
  catalogo: { titulo: 'Catálogo', render: renderCatalogo },
  pedidos: { titulo: 'Meus pedidos', render: renderPedidos },
  ingressos: { titulo: 'Meus ingressos', render: renderIngressos },
  categoria: { titulo: 'Cadastrar categoria', render: renderCategoria, soOrganizador: true },
};
const ROTULO_PAPEL = { COMPRADOR: 'Comprador', ORGANIZADOR: 'Organizador' };

const telaAuth = document.getElementById('tela-auth');
const telaApp = document.getElementById('tela-app');
const painel = document.getElementById('painel');
const navegacao = document.getElementById('navegacao');
const areaUsuario = document.getElementById('usuario');

function rotaAtual() {
  const nome = location.hash.startsWith('#/') ? location.hash.slice(2) : '';
  return ROTAS[nome] ? nome : null;
}

function rotasPermitidas(sessao) {
  return Object.entries(ROTAS).filter(([, rota]) => !rota.soOrganizador || sessao.usuario?.papel === 'ORGANIZADOR');
}

function atualizarTela() {
  const sessao = lerSessao();
  telaAuth.hidden = Boolean(sessao);
  telaApp.hidden = !sessao;
  areaUsuario.hidden = !sessao;
  if (!sessao) {
    esvaziar(painel);
    return;
  }

  document.getElementById('usuario-nome').textContent = sessao.usuario?.nome ?? '';
  document.getElementById('usuario-papel').textContent = ROTULO_PAPEL[sessao.usuario?.papel] ?? '';

  const permitidas = rotasPermitidas(sessao);
  let atual = rotaAtual();
  if (!atual || !permitidas.some(([nome]) => nome === atual)) {
    atual = 'catalogo';
    history.replaceState(null, '', '#/catalogo');
  }

  esvaziar(navegacao);
  for (const [nome, rota] of permitidas) {
    navegacao.append(el('a', { href: `#/${nome}`, class: 'aba', 'aria-current': nome === atual ? 'page' : null }, rota.titulo));
  }

  // Cada navegação ganha uma raiz nova: resposta atrasada de uma tela anterior escreve
  // numa raiz já descartada e não aparece por cima da tela atual.
  const raiz = el('div', { class: 'secao' });
  esvaziar(painel);
  painel.append(raiz);
  ROTAS[atual].render(raiz, sessao);
}

function sair(aviso) {
  limparSessao();
  location.hash = '';
  mostrarAba('login');
  mostrarAviso(aviso);
  atualizarTela();
}

definirAoSessaoInvalida(() => sair('Sua sessão expirou ou o token é inválido. Entre novamente.'));

iniciarAuth({
  aoEntrar: () => {
    // Mudar o hash já dispara hashchange (e a renderização); só chama direto se não mudou.
    const jaEstavaNoCatalogo = location.hash === '#/catalogo';
    location.hash = '#/catalogo';
    if (jaEstavaNoCatalogo) atualizarTela();
  },
});

iniciarInspetor({
  aoSimularSemToken: () => chamar('GET', '/api/categorias', { autenticado: false, rotulo: 'simulado' }).catch(() => {}),
  aoSimularTokenInvalido: () => chamar('GET', '/api/categorias', { token: 'token.invalido.simulado', rotulo: 'simulado' }).catch(() => {}),
});

document.getElementById('btn-sair').addEventListener('click', () => sair(null));
window.addEventListener('hashchange', () => {
  if (location.hash.startsWith('#/') || location.hash === '') atualizarTela();
});

atualizarTela();
