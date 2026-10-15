package br.com.ticket.wizard.usuarios;

import br.com.ticket.wizard.base.Erros;
import br.com.ticket.wizard.base.Transacao;
import br.com.ticket.wizard.base.Validacao;
import br.com.ticket.wizard.grpc.usuarios.AutenticarRequest;
import br.com.ticket.wizard.grpc.usuarios.CadastrarUsuarioRequest;
import br.com.ticket.wizard.grpc.usuarios.Papel;
import br.com.ticket.wizard.grpc.usuarios.UsuarioServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class UsuarioServiceImpl extends UsuarioServiceGrpc.UsuarioServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int SENHA_MINIMA = 8;
    // O PBKDF2 aceita qualquer tamanho; o teto só evita abuso de CPU com senhas gigantes.
    private static final int SENHA_MAXIMA = 128;

    // Hash de uma senha que ninguém tem. Quando o e-mail não existe, a senha informada é
    // conferida contra ele, para a resposta demorar o mesmo e não revelar quais e-mails existem.
    private static final String HASH_FALSO = SenhaHasher.gerar("senha-que-ninguem-possui");

    private final DataSource dataSource;
    private final UsuarioRepositorio usuarios;

    public UsuarioServiceImpl(DataSource dataSource, UsuarioRepositorio usuarios) {
        this.dataSource = dataSource;
        this.usuarios = usuarios;
    }

    @Override
    public void cadastrar(CadastrarUsuarioRequest request,
                          StreamObserver<br.com.ticket.wizard.grpc.usuarios.Usuario> responseObserver) {
        String nome = Validacao.texto("nome", request.getNome(), 120);
        String email = normalizarEmail(request.getEmail());
        if (!EMAIL.matcher(email).matches()) {
            throw Erros.argumentoInvalido("O e-mail informado não é válido.");
        }
        String senha = request.getSenha();
        if (senha.length() < SENHA_MINIMA || senha.length() > SENHA_MAXIMA) {
            throw Erros.argumentoInvalido(
                    "A senha deve ter entre " + SENHA_MINIMA + " e " + SENHA_MAXIMA + " caracteres.");
        }
        String papel = switch (request.getPapel()) {
            case PAPEL_NAO_ESPECIFICADO, COMPRADOR -> "COMPRADOR";
            case ORGANIZADOR -> "ORGANIZADOR";
            case UNRECOGNIZED -> throw Erros.argumentoInvalido("Papel inválido.");
        };

        Usuario novo = new Usuario(UUID.randomUUID(), nome, email, SenhaHasher.gerar(senha), papel);
        Transacao.executar(dataSource, conexao -> usuarios.inserir(conexao, novo));
        LOG.info("Usuário cadastrado: id={}, papel={}", novo.id(), novo.papel());

        responseObserver.onNext(paraProto(novo));
        responseObserver.onCompleted();
    }

    @Override
    public void autenticar(AutenticarRequest request,
                           StreamObserver<br.com.ticket.wizard.grpc.usuarios.Usuario> responseObserver) {
        String email = normalizarEmail(request.getEmail());
        if (email.isEmpty() || request.getSenha().isEmpty()) {
            throw Erros.argumentoInvalido("E-mail e senha são obrigatórios.");
        }

        var encontrado = Transacao.executar(dataSource, conexao -> usuarios.buscarPorEmail(conexao, email));
        boolean senhaConfere = SenhaHasher.confere(request.getSenha(),
                                                   encontrado.map(Usuario::senhaHash).orElse(HASH_FALSO));

        if (encontrado.isEmpty() || !senhaConfere) {
            LOG.info("Falha de autenticação");
            throw Erros.naoAutenticado("E-mail ou senha inválidos.");
        }

        responseObserver.onNext(paraProto(encontrado.get()));
        responseObserver.onCompleted();
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static br.com.ticket.wizard.grpc.usuarios.Usuario paraProto(Usuario usuario) {
        return br.com.ticket.wizard.grpc.usuarios.Usuario.newBuilder()
                                                         .setId(usuario.id().toString())
                                                         .setNome(usuario.nome())
                                                         .setEmail(usuario.email())
                                                         .setPapel(Papel.valueOf(usuario.papel()))
                                                         .build();
    }
}
