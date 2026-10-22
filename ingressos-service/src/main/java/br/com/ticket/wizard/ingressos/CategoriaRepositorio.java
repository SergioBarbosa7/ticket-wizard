package br.com.ticket.wizard.ingressos;

import br.com.ticket.wizard.base.Erros;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CategoriaRepositorio {

    // Código SQLSTATE do PostgreSQL para violação de unicidade.
    private static final String VIOLACAO_DE_UNICIDADE = "23505";

    private static final String COLUNAS =
            "id, evento_id, nome, preco_centavos, quantidade_total, quantidade_disponivel";

    public Categoria inserir(Connection conexao, Categoria categoria, UUID criadoPor) throws SQLException {
        String sql = "INSERT INTO categorias (id, evento_id, nome, preco_centavos, quantidade_total, "
                     + "quantidade_disponivel, criado_por) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setObject(1, categoria.id());
            ps.setString(2, categoria.eventoId());
            ps.setString(3, categoria.nome());
            ps.setLong(4, categoria.precoCentavos());
            ps.setInt(5, categoria.quantidadeTotal());
            ps.setInt(6, categoria.quantidadeDisponivel());
            ps.setObject(7, criadoPor);
            ps.executeUpdate();
            return categoria;
        } catch (SQLException erro) {
            if (VIOLACAO_DE_UNICIDADE.equals(erro.getSQLState())) {
                throw Erros.jaExiste("Já existe a categoria '" + categoria.nome()
                                     + "' para o evento '" + categoria.eventoId() + "'.");
            }
            throw erro;
        }
    }

    public List<Categoria> listar(Connection conexao, String eventoId) throws SQLException {
        boolean filtrar = eventoId != null && !eventoId.isBlank();
        String sql = "SELECT " + COLUNAS + " FROM categorias"
                     + (filtrar ? " WHERE evento_id = ?" : "")
                     + " ORDER BY evento_id, nome";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            if (filtrar) {
                ps.setString(1, eventoId.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Categoria> categorias = new ArrayList<>();
                while (rs.next()) {
                    categorias.add(mapear(rs));
                }
                return categorias;
            }
        }
    }

    public Optional<Categoria> buscar(Connection conexao, UUID id) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM categorias WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    // Baixa o estoque em um único UPDATE condicional. A condição "quantidade_disponivel >= ?"
    // e a atualização acontecem de forma atômica, então duas compras simultâneas nunca
    // vendem o mesmo ingresso. Sem linha afetada = estoque insuficiente (ou categoria inexistente).
    public Optional<Categoria> baixarEstoque(Connection conexao, UUID id, int quantidade) throws SQLException {
        String sql = "UPDATE categorias SET quantidade_disponivel = quantidade_disponivel - ? "
                     + "WHERE id = ? AND quantidade_disponivel >= ? RETURNING " + COLUNAS;
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setObject(2, id);
            ps.setInt(3, quantidade);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        return new Categoria(rs.getObject("id", UUID.class),
                             rs.getString("evento_id"),
                             rs.getString("nome"),
                             rs.getLong("preco_centavos"),
                             rs.getInt("quantidade_total"),
                             rs.getInt("quantidade_disponivel"));
    }
}
