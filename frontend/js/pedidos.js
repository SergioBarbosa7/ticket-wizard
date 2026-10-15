import { chamar } from './api.js';
import { el, esvaziar } from './dom.js';
import { cabecalhoDaSecao, carregando, falha, vazio } from './estados.js';
import { formatarCentavos, formatarDataHora, idCurto } from './formato.js';

const ROTULO_STATUS = { CONFIRMADO: 'Confirmado', PENDENTE: 'Pendente', FALHOU: 'Falhou' };

export function renderPedidos(raiz) {
  const lista = el('div', { class: 'lista' });
  raiz.append(cabecalhoDaSecao('Meus pedidos', carregar), lista);

  async function carregar() {
    esvaziar(lista);
    lista.append(carregando('Carregando pedidos…'));
    try {
      const pedidos = await chamar('GET', '/api/pedidos');
      esvaziar(lista);
      if (pedidos.length === 0) {
        lista.append(vazio('Você ainda não fez nenhum pedido. Escolha um ingresso no catálogo.'));
        return;
      }
      pedidos.forEach((pedido) => lista.append(cartaoDoPedido(pedido)));
    } catch (erro) {
      esvaziar(lista);
      lista.append(falha(erro, carregar));
    }
  }

  function cartaoDoPedido(pedido) {
    const ids = pedido.ingressoIds ?? [];
    return el('article', { class: 'cartao pedido' },
      el('div', { class: 'pedido__topo' },
        el('h2', { class: 'pedido__titulo' }, pedido.nomeCategoria || 'Pedido sem categoria confirmada',
          pedido.eventoId ? el('span', { class: 'pedido__evento' }, ` · ${pedido.eventoId}`) : null),
        el('span', { class: `selo selo--${String(pedido.status).toLowerCase()}` }, ROTULO_STATUS[pedido.status] ?? pedido.status)),
      el('dl', { class: 'dados' },
        el('div', {}, el('dt', {}, 'Pedido'), el('dd', { title: pedido.id, class: 'mono' }, idCurto(pedido.id))),
        el('div', {}, el('dt', {}, 'Quantidade'), el('dd', {}, String(pedido.quantidade))),
        el('div', {}, el('dt', {}, 'Total'), el('dd', {}, formatarCentavos(pedido.valorTotalCentavos))),
        el('div', {}, el('dt', {}, 'Feito em'), el('dd', {}, formatarDataHora(pedido.criadoEm)))),
      pedido.motivoFalha ? el('p', { class: 'alerta alerta--erro' }, `Motivo: ${pedido.motivoFalha}`) : null,
      ids.length > 0
        ? el('details', {}, el('summary', {}, `${ids.length} ingresso(s) emitido(s)`),
          el('ul', { class: 'ids' }, ids.map((id) => el('li', { class: 'mono' }, id))))
        : null);
  }

  carregar();
}
