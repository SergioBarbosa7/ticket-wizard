const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' });

export function formatarCentavos(centavos) {
  return moeda.format(Number(centavos) / 100);
}

export function formatarDataHora(iso) {
  const data = new Date(iso);
  return Number.isNaN(data.getTime()) ? '—' : dataHora.format(data);
}

export function idCurto(id) {
  return typeof id === 'string' ? id.slice(0, 8) : '—';
}

// Converte "100,00", "1.250,5", "R$ 99" ou "100.50" em centavos inteiros sem passar por
// ponto flutuante (100.10 * 100 daria 10010.000000000002). Devolve:
//   null  -> campo vazio (o servidor responde 400 "obrigatório")
//   NaN   -> texto que não é um valor em reais
export function reaisParaCentavos(texto) {
  let t = String(texto ?? '').trim().replace(/^R\$\s*/i, '').replace(/\s/g, '');
  if (t === '') return null;
  // Com vírgula, o ponto é separador de milhar ("1.250,50"); sem vírgula, é decimal.
  if (t.includes(',')) t = t.replace(/\./g, '').replace(',', '.');
  const partes = /^(\d+)(?:\.(\d{1,2}))?$/.exec(t);
  if (!partes) return NaN;
  const reais = Number(partes[1]);
  const centavos = Number((partes[2] ?? '').padEnd(2, '0'));
  return reais * 100 + centavos;
}

export function textoParaInteiro(texto) {
  const t = String(texto ?? '').trim();
  if (t === '') return null;
  return /^-?\d+$/.test(t) ? Number(t) : t;
}
