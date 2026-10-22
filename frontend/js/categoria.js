import { chamar } from './api.js';
import { el } from './dom.js';
import { alertaDoForm, campo, enviando, erroDeCampo, limparErros, mostrarErro } from './forms.js';
import { reaisParaCentavos, textoParaInteiro } from './formato.js';

const APELIDOS = { precoCentavos: 'preco' };

export function renderCategoria(raiz) {
  const sucesso = el('p', { class: 'alerta alerta--sucesso', role: 'status', hidden: true });
  const form = el('form', { class: 'cartao formulario', novalidate: true },
    alertaDoForm(),
    campo({ id: 'cat-evento', nome: 'eventoId', rotulo: 'Evento', dica: '(identificador, ex.: show-rock-2026)' }),
    campo({ id: 'cat-nome', nome: 'nome', rotulo: 'Nome da categoria', dica: '(ex.: Pista, Camarote)' }),
    campo({ id: 'cat-preco', nome: 'preco', rotulo: 'Preço (R$)', dica: '(ex.: 100,00)', inputmode: 'decimal' }),
    campo({ id: 'cat-quantidade', nome: 'quantidade', rotulo: 'Quantidade de ingressos', tipo: 'number', inputmode: 'numeric' }),
    el('button', { type: 'submit', class: 'botao botao--primario' }, 'Cadastrar categoria'));

  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limparErros(form);
    sucesso.hidden = true;

    const centavos = reaisParaCentavos(form.preco.value);
    if (Number.isNaN(centavos)) {
      erroDeCampo(form, 'preco', 'Informe o valor em reais, por exemplo 100,00.');
      return;
    }

    const corpo = {
      eventoId: form.eventoId.value,
      nome: form.nome.value,
      precoCentavos: centavos,
      quantidade: textoParaInteiro(form.quantidade.value),
    };
    await enviando(form, async () => {
      try {
        const criada = await chamar('POST', '/api/categorias', { corpo });
        sucesso.textContent = `Categoria "${criada.nome}" cadastrada para ${criada.eventoId} com ${criada.quantidadeTotal} ingressos.`;
        sucesso.hidden = false;
        form.reset();
      } catch (erro) {
        mostrarErro(form, erro, APELIDOS);
      }
    });
  });

  raiz.append(el('h1', { class: 'titulo' }, 'Cadastrar categoria'),
    el('p', { class: 'subtitulo' }, 'Defina uma categoria de ingresso para um evento. Ela aparece no catálogo assim que for salva.'),
    sucesso, form);
}
