import { el, esvaziar } from './dom.js';

const MAXIMO = 10;
const itens = [];
let contador = 0;
let abertoId = null;
let lista, vazio, contagem, corpo, alternar;

// Segredos nunca chegam ao painel: a máscara é aplicada ANTES de guardar a entrada.
export function mascarar(valor) {
  if (Array.isArray(valor)) return valor.map(mascarar);
  if (valor && typeof valor === 'object') {
    return Object.fromEntries(Object.entries(valor).map(([chave, v]) => [chave, mascararCampo(chave, v)]));
  }
  return valor;
}

function mascararCampo(chave, valor) {
  if (chave === 'senha') return '••••';
  if (chave === 'token' && typeof valor === 'string') return mascararToken(valor);
  return mascarar(valor);
}

export function mascararToken(token) {
  return token.slice(0, 12) + '…';
}

export function iniciarInspetor({ aoSimularSemToken, aoSimularTokenInvalido }) {
  lista = document.getElementById('inspetor-lista');
  vazio = document.getElementById('inspetor-vazio');
  contagem = document.getElementById('inspetor-contagem');
  corpo = document.getElementById('inspetor-corpo');
  alternar = document.getElementById('inspetor-alternar');

  alternar.addEventListener('click', () => definirAberto(alternar.getAttribute('aria-expanded') !== 'true'));
  document.getElementById('inspetor-limpar').addEventListener('click', () => {
    itens.length = 0;
    abertoId = null;
    desenhar();
  });
  document.getElementById('inspetor-sem-token').addEventListener('click', aoSimularSemToken);
  document.getElementById('inspetor-token-invalido').addEventListener('click', aoSimularTokenInvalido);

  definirAberto(window.matchMedia('(min-width: 900px)').matches);
  desenhar();
}

function definirAberto(aberto) {
  alternar.setAttribute('aria-expanded', String(aberto));
  corpo.hidden = !aberto;
  document.body.classList.toggle('inspetor-aberto', aberto);
}

export function registrar(entrada) {
  itens.unshift({ ...entrada, id: ++contador, hora: new Date() });
  itens.length = Math.min(itens.length, MAXIMO);
  desenhar();
}

function classeDoStatus(status) {
  if (status >= 200 && status < 300) return 'status--2xx';
  if (status >= 400 && status < 500) return 'status--4xx';
  return 'status--5xx';
}

function json(valor) {
  return valor === undefined || valor === null ? '(sem corpo)' : JSON.stringify(valor, null, 2);
}

function desenhar() {
  contagem.textContent = String(itens.length);
  vazio.hidden = itens.length > 0;
  esvaziar(lista);

  for (const item of itens) {
    const aberto = item.id === abertoId;
    const idDetalhe = `inspetor-detalhe-${item.id}`;

    const cabecalho = el('button', {
      type: 'button',
      class: 'req__linha',
      'aria-expanded': String(aberto),
      'aria-controls': idDetalhe,
      onclick: () => {
        abertoId = aberto ? null : item.id;
        desenhar();
      },
    },
    el('span', { class: `status ${classeDoStatus(item.status)}` }, item.status === 0 ? 'ERRO' : item.status),
    el('span', { class: 'req__metodo' }, item.metodo),
    el('span', { class: 'req__caminho' }, item.caminho),
    item.rotulo ? el('span', { class: 'selo selo--aviso' }, item.rotulo) : null,
    el('span', { class: 'req__tempo' }, `${item.ms} ms`));

    const detalhe = el('div', { class: 'req__detalhe', id: idDetalhe, hidden: !aberto },
      el('p', { class: 'req__meta' }, `${item.hora.toLocaleTimeString('pt-BR')} · ${item.metodo} ${item.caminho}`
        + (item.autorizacao ? ` · Authorization: ${item.autorizacao}` : ' · sem Authorization')),
      el('h3', {}, 'Corpo enviado'),
      el('pre', {}, json(item.enviado)),
      el('h3', {}, `Resposta (${item.status === 0 ? 'sem resposta' : item.status})`),
      el('pre', {}, json(item.recebido)));

    lista.append(el('li', { class: 'req' }, cabecalho, detalhe));
  }
}
