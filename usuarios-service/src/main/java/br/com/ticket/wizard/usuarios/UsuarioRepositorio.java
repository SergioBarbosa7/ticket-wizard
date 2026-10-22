package br.com.ticket.wizard.usuarios;

import br.com.ticket.wizard.base.Erros;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UsuarioRepositorio {

    // Código SQLSTATE do PostgreSQL para violação de unicidade.
    private static final String VIOLACAO_DE_UNICIDADE = "23505";

    public Usuario inserir(Connection conexao, Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuarios (id, nome, email, senha_hash, papel) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setObject(1, usuario.id());
            ps.setString(2, usuario.nome());
            ps.setString(3, usuario.email());
            ps.setString(4, usuario.senhaHash());
            ps.setString(5, usuario.papel());
            ps.executeUpdate();
            return usuario;
        } catch (SQLException erro) {
            if (VIOLACAO_DE_UNICIDADE.equals(erro.getSQLState())) {
                throw Erros.jaExiste("Já existe um usuário com o e-mail " + usuario.email() + ".");
            }
            throw erro;
        }
    }

    public Optional<Usuario> buscarPorEmail(Connection conexao, String email) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                "SELECT id, nome, email, senha_hash, papel FROM usuarios WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(rs.getObject("id", UUID.class),
                                               rs.getString("nome"),
                                               rs.getString("email"),
                                               rs.getString("senha_hash"),
                                               rs.getString("papel")));
            }
        }
    }
}
