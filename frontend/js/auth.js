import { chamar } from './api.js';
import { enviando, limparErros, mostrarErro } from './forms.js';
import { salvarSessao } from './sessao.js';

const abas = { login: ['aba-login', 'form-login'], cadastro: ['aba-cadastro', 'form-cadastro'] };

export function mostrarAba(nome) {
  for (const [chave, [idAba, idForm]] of Object.entries(abas)) {
    const ativa = chave === nome;
    const aba = document.getElementById(idAba);
    aba.setAttribute('aria-selected', String(ativa));
    aba.tabIndex = ativa ? 0 : -1;
    document.getElementById(idForm).hidden = !ativa;
  }
}

export function mostrarAviso(texto, tipo = 'info') {
  const aviso = document.getElementById('auth-aviso');
  aviso.textContent = texto ?? '';
  aviso.hidden = !texto;
  aviso.className = `alerta${tipo === 'sucesso' ? ' alerta--sucesso' : ''}`;
}

export function iniciarAuth({ aoEntrar }) {
  document.getElementById('aba-login').addEventListener('click', () => mostrarAba('login'));
  document.getElementById('aba-cadastro').addEventListener('click', () => mostrarAba('cadastro'));

  document.querySelector('.abas').addEventListener('keydown', (evento) => {
    if (evento.key !== 'ArrowLeft' && evento.key !== 'ArrowRight') return;
    const proxima = document.getElementById('aba-login').getAttribute('aria-selected') === 'true' ? 'cadastro' : 'login';
    mostrarAba(proxima);
    document.getElementById(abas[proxima][0]).focus();
  });

  const formLogin = document.getElementById('form-login');
  formLogin.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limparErros(formLogin);
    mostrarAviso(null);
    const corpo = { email: formLogin.email.value, senha: formLogin.senha.value };
    await enviando(formLogin, async () => {
      try {
        const resposta = await chamar('POST', '/api/auth/login', { corpo, autenticado: false });
        const sessao = {
          token: resposta.token,
          usuario: resposta.usuario,
          expiraEm: Date.now() + Number(resposta.expiraEmSegundos ?? 0) * 1000,
        };
        salvarSessao(sessao);
        formLogin.reset();
        aoEntrar(sessao);
      } catch (erro) {
        mostrarErro(formLogin, erro);
      }
    });
  });

  const formCadastro = document.getElementById('form-cadastro');
  formCadastro.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limparErros(formCadastro);
    mostrarAviso(null);
    const corpo = {
      nome: formCadastro.nome.value,
      email: formCadastro.email.value,
      senha: formCadastro.senha.value,
      papel: formCadastro.papel.value,
    };
    await enviando(formCadastro, async () => {
      try {
        const criado = await chamar('POST', '/api/auth/register', { corpo, autenticado: false });
        formCadastro.reset();
        formLogin.email.value = criado.email ?? corpo.email;
        mostrarAba('login');
        mostrarAviso('Conta criada. Entre com seu e-mail e senha.', 'sucesso');
        formLogin.senha.focus();
      } catch (erro) {
        mostrarErro(formCadastro, erro);
      }
    });
  });
}
