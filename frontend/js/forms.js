import { el } from './dom.js';

export function limparErros(form) {
  form.querySelectorAll('[data-erro-campo]').forEach((p) => { p.textContent = ''; });
  form.querySelectorAll('[aria-invalid]').forEach((i) => i.removeAttribute('aria-invalid'));
  const alerta = form.querySelector('[data-alerta]');
  if (alerta) {
    alerta.hidden = true;
    alerta.textContent = '';
  }
}

export function mostrarErro(form, erro, apelidos = {}) {
  const mensagens = [];
  for (const { campo, mensagem } of erro.erros ?? []) {
    const nome = apelidos[campo] ?? campo;
    const alvo = form.querySelector(`[data-erro-campo="${CSS.escape(nome)}"]`);
    if (alvo) {
      alvo.textContent = mensagem;
      form.elements[nome]?.setAttribute('aria-invalid', 'true');
    } else {
      mensagens.push(`${campo}: ${mensagem}`);
    }
  }
  const alerta = form.querySelector('[data-alerta]');
  if (alerta) {
    alerta.textContent = [erro.detalhe || erro.message, ...mensagens].filter(Boolean).join(' ');
    alerta.hidden = false;
  }
}

export function erroDeCampo(form, nome, mensagem) {
  const alvo = form.querySelector(`[data-erro-campo="${CSS.escape(nome)}"]`);
  if (alvo) alvo.textContent = mensagem;
  form.elements[nome]?.setAttribute('aria-invalid', 'true');
}

export async function enviando(form, trabalho) {
  const botao = form.querySelector('button[type="submit"]');
  const textoOriginal = botao?.textContent;
  if (botao) {
    botao.disabled = true;
    botao.textContent = 'Enviando…';
  }
  try {
    return await trabalho();
  } finally {
    if (botao) {
      botao.disabled = false;
      botao.textContent = textoOriginal;
    }
  }
}

export function campo({ id, nome, rotulo, dica, tipo = 'text', ...atributos }) {
  return el('div', { class: 'campo' },
    el('label', { for: id }, rotulo, dica ? el('span', { class: 'campo__dica' }, ` ${dica}`) : null),
    el('input', { id, name: nome, type: tipo, 'aria-describedby': `${id}-erro`, ...atributos }),
    el('p', { class: 'campo__erro', id: `${id}-erro`, 'data-erro-campo': nome }));
}

export function alertaDoForm() {
  return el('p', { class: 'alerta alerta--erro', 'data-alerta': '', role: 'alert', hidden: true });
}
