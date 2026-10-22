import { chamar } from './api.js';
import { el, esvaziar } from './dom.js';
import { cabecalhoDaSecao, carregando, falha, vazio } from './estados.js';
import { formatarDataHora, idCurto } from './formato.js';

export function renderIngressos(raiz) {
  const lista = el('div', { class: 'grade' });
  raiz.append(cabecalhoDaSecao('Meus ingressos', carregar), lista);

  async function carregar() {
    esvaziar(lista);
    lista.append(carregando('Carregando ingressos…'));
    try {
      const ingressos = await chamar('GET', '/api/ingressos');
      esvaziar(lista);
      if (ingressos.length === 0) {
        lista.append(vazio('Você ainda não tem ingressos.'));
        return;
      }
      ingressos.forEach((ingresso) => lista.append(cartaoDoIngresso(ingresso)));
    } catch (erro) {
      esvaziar(lista);
      lista.append(falha(erro, carregar));
    }
  }

  function cartaoDoIngresso(ingresso) {
    return el('article', { class: 'cartao ingresso' },
      el('p', { class: 'categoria__evento' }, ingresso.eventoId),
      el('h2', { class: 'categoria__nome' }, ingresso.nomeCategoria),
      el('dl', { class: 'dados dados--coluna' },
        el('div', {}, el('dt', {}, 'Ingresso'), el('dd', { class: 'mono', title: ingresso.id }, idCurto(ingresso.id))),
        el('div', {}, el('dt', {}, 'Pedido'), el('dd', { class: 'mono', title: ingresso.pedidoId }, idCurto(ingresso.pedidoId))),
        el('div', {}, el('dt', {}, 'Emitido em'), el('dd', {}, formatarDataHora(ingresso.emitidoEm)))));
  }

  carregar();
}
