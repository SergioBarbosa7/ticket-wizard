import { el } from './dom.js';

export function carregando(texto = 'Carregando…') {
  return el('p', { class: 'estado', role: 'status' }, el('span', { class: 'giro', 'aria-hidden': 'true' }), texto);
}

export function vazio(texto) {
  return el('p', { class: 'estado estado--vazio' }, texto);
}

export function falha(erro, aoTentarNovamente) {
  return el('div', { class: 'alerta alerta--erro', role: 'alert' },
    el('span', {}, erro.detalhe || erro.message || 'Algo deu errado.'),
    aoTentarNovamente
      ? el('button', { type: 'button', class: 'botao botao--mini', onclick: aoTentarNovamente }, 'Tentar novamente')
      : null);
}

export function cabecalhoDaSecao(titulo, aoAtualizar) {
  return el('div', { class: 'secao__cabecalho' },
    el('h1', { class: 'titulo' }, titulo),
    el('button', { type: 'button', class: 'botao botao--secundario', onclick: aoAtualizar }, 'Atualizar'));
}
