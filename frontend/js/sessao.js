// Sessão em sessionStorage (some ao fechar a aba). O storage pode lançar exceção
// (modo privado, bloqueio de dados do site), então há um espelho em memória.
const CHAVE = 'ticketwizard.sessao';
let emMemoria = null;

export function lerSessao() {
  if (emMemoria) return emMemoria;
  try {
    const bruto = sessionStorage.getItem(CHAVE);
    emMemoria = bruto ? JSON.parse(bruto) : null;
  } catch {
    emMemoria = null;
  }
  if (emMemoria?.expiraEm && Date.now() >= emMemoria.expiraEm) {
    limparSessao();
  }
  return emMemoria;
}

export function salvarSessao(sessao) {
  emMemoria = sessao;
  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao));
  } catch {
    // Segue só em memória.
  }
}

export function limparSessao() {
  emMemoria = null;
  try {
    sessionStorage.removeItem(CHAVE);
  } catch {
    // Nada a fazer.
  }
}
