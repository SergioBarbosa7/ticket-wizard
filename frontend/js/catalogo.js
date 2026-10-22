import { chamar } from './api.js';
import { el, esvaziar } from './dom.js';
import { cabecalhoDaSecao, carregando, falha, vazio } from './estados.js';
import { formatarCentavos, textoParaInteiro } from './formato.js';

export function renderCatalogo(raiz) {
  const aviso = el('div', { 'aria-live': 'polite' });
  const grade = el('div', { class: 'grade' });
  const filtro = el('input', { id: 'filtro-evento', type: 'text', placeholder: 'Filtrar por evento (ex.: show-rock-2026)' });
  const formFiltro = el('form', { class: 'filtro', novalidate: true, role: 'search',
    onsubmit: (evento) => { evento.preventDefault(); carregar(); } },
  el('label', { for: 'filtro-evento', class: 'sr-only' }, 'Filtrar por evento'),
  filtro,
  el('button', { type: 'submit', class: 'botao botao--secundario' }, 'Buscar'));

  raiz.append(cabecalhoDaSecao('Catálogo de ingressos', () => carregar()), formFiltro, aviso, grade);

  async function carregar() {
    esvaziar(grade);
    grade.append(carregando('Carregando categorias…'));
    const evento = filtro.value.trim();
    try {
      const categorias = await chamar('GET', '/api/categorias' + (evento ? `?eventoId=${encodeURIComponent(evento)}` : ''));
      esvaziar(grade);
      if (categorias.length === 0) {
        grade.append(vazio(evento ? 'Nenhuma categoria para esse evento.' : 'Ainda não há categorias cadastradas.'));
        return;
      }
      categorias.forEach((categoria) => grade.append(cartaoDaCategoria(categoria)));
    } catch (erro) {
      esvaziar(grade);
      grade.append(falha(erro, carregar));
    }
  }

  function cartaoDaCategoria(categoria) {
    const esgotada = categoria.quantidadeDisponivel <= 0;
    const idQuantidade = `qtd-${categoria.id}`;
    const alerta = el('p', { class: 'alerta alerta--erro', role: 'alert', hidden: true });
    const erroQuantidade = el('p', { class: 'campo__erro', id: `${idQuantidade}-erro` });
    const entrada = el('input', { id: idQuantidade, type: 'number', inputmode: 'numeric', value: '1',
      'aria-describedby': `${idQuantidade}-erro`, disabled: esgotada });
    const botao = el('button', { type: 'submit', class: 'botao botao--primario', disabled: esgotada },
      esgotada ? 'Esgotado' : 'Comprar');

    const form = el('form', { class: 'compra', novalidate: true,
      onsubmit: async (evento) => {
        evento.preventDefault();
        alerta.hidden = true;
        erroQuantidade.textContent = '';
        entrada.removeAttribute('aria-invalid');
        botao.disabled = true;
        botao.textContent = 'Comprando…';
        try {
          const pedido = await chamar('POST', '/api/pedidos', {
            corpo: { categoriaId: categoria.id, quantidade: textoParaInteiro(entrada.value) },
          });
          esvaziar(aviso);
          aviso.append(el('p', { class: 'alerta alerta--sucesso', role: 'status' },
            `Pedido ${pedido.status.toLowerCase()}: ${pedido.quantidade} ingresso(s) de ${categoria.nome} `
            + `(${categoria.eventoId}) por ${formatarCentavos(pedido.valorTotalCentavos)}.`));
          await carregar();
        } catch (erro) {
          const daQuantidade = erro.erros.find((e) => e.campo === 'quantidade');
          if (daQuantidade) {
            erroQuantidade.textContent = daQuantidade.mensagem;
            entrada.setAttribute('aria-invalid', 'true');
          }
          alerta.textContent = erro.detalhe;
          alerta.hidden = false;
          botao.disabled = esgotada;
          botao.textContent = esgotada ? 'Esgotado' : 'Comprar';
          // Estoque ou categoria podem ter mudado desde que a lista foi carregada: recarrega
          // e deixa a mensagem no aviso do topo, que sobrevive ao redesenho dos cartões.
          if (erro.status === 404 || erro.status === 409) {
            esvaziar(aviso);
            aviso.append(el('p', { class: 'alerta alerta--erro', role: 'alert' }, erro.detalhe));
            carregar();
          }
        }
      } },
    el('div', { class: 'campo campo--linha' },
      el('label', { for: idQuantidade }, 'Quantidade'), entrada, botao),
    erroQuantidade, alerta);

    const percentual = categoria.quantidadeTotal > 0 ? categoria.quantidadeDisponivel / categoria.quantidadeTotal : 0;
    return el('article', { class: 'cartao categoria' },
      el('p', { class: 'categoria__evento' }, categoria.eventoId),
      el('h2', { class: 'categoria__nome' }, categoria.nome),
      el('p', { class: 'categoria__preco' }, formatarCentavos(categoria.precoCentavos)),
      el('meter', { min: 0, max: categoria.quantidadeTotal, value: categoria.quantidadeDisponivel,
        low: categoria.quantidadeTotal * 0.2, optimum: categoria.quantidadeTotal,
        'aria-label': 'Ingressos restantes' }),
      el('p', { class: percentual <= 0.2 ? 'categoria__vagas categoria__vagas--baixas' : 'categoria__vagas' },
        esgotada ? 'Esgotado' : `${categoria.quantidadeDisponivel} de ${categoria.quantidadeTotal} restantes`),
      form);
  }

  carregar();
}
