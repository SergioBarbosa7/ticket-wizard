package br.com.ticket.wizard.pedidos;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PedidoRepositorio {

    private static final String SELECT = "SELECT p.id, p.usuario_id, p.categoria_id, p.evento_id, p.nome_categoria, "
            + "p.quantidade, p.valor_total_centavos, p.status, p.motivo_falha, p.criado_em, "
            + "(SELECT COALESCE(array_agg(pi.ingresso_id::text ORDER BY pi.ingresso_id), '{}') "
            + " FROM pedido_ingressos pi WHERE pi.pedido_id = p.id) AS ingresso_ids "
            + "FROM pedidos p ";

    public void inserirPendente(Connection conexao, UUID id, UUID usuarioId, UUID categoriaId, int quantidade)
            throws SQLException {
        String sql = "INSERT INTO pedidos (id, usuario_id, categoria_id, quantidade, status) "
                     + "VALUES (?, ?, ?, ?, 'PENDENTE')";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, usuarioId);
            ps.setObject(3, categoriaId);
            ps.setInt(4, quantidade);
            ps.executeUpdate();
        }
    }

    public void confirmar(Connection conexao, UUID id, String eventoId, String nomeCategoria,
                          long valorTotalCentavos, List<UUID> ingressoIds) throws SQLException {
        String sql = "UPDATE pedidos SET status = 'CONFIRMADO', evento_id = ?, nome_categoria = ?, "
                     + "valor_total_centavos = ? WHERE id = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, eventoId);
            ps.setString(2, nomeCategoria);
            ps.setLong(3, valorTotalCentavos);
            ps.setObject(4, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO pedido_ingressos (pedido_id, ingresso_id) VALUES (?, ?)")) {
            for (UUID ingressoId : ingressoIds) {
                ps.setObject(1, id);
                ps.setObject(2, ingressoId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void falhar(Connection conexao, UUID id, String motivo) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                "UPDATE pedidos SET status = 'FALHOU', motivo_falha = ? WHERE id = ?")) {
            ps.setString(1, motivo.length() > 255 ? motivo.substring(0, 255) : motivo);
            ps.setObject(2, id);
            ps.executeUpdate();
        }
    }

    public Optional<Pedido> buscar(Connection conexao, UUID id) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SELECT + "WHERE p.id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Pedido> listarPorUsuario(Connection conexao, UUID usuarioId) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                SELECT + "WHERE p.usuario_id = ? ORDER BY p.criado_em DESC, p.id")) {
            ps.setObject(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Pedido> pedidos = new ArrayList<>();
                while (rs.next()) {
                    pedidos.add(mapear(rs));
                }
                return pedidos;
            }
        }
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        Array array = rs.getArray("ingresso_ids");
        List<UUID> ingressoIds = new ArrayList<>();
        for (Object ingressoId : (Object[]) array.getArray()) {
            ingressoIds.add(UUID.fromString((String) ingressoId));
        }
        return new Pedido(rs.getObject("id", UUID.class),
                          rs.getObject("usuario_id", UUID.class),
                          rs.getObject("categoria_id", UUID.class),
                          rs.getString("evento_id"),
                          rs.getString("nome_categoria"),
                          rs.getInt("quantidade"),
                          rs.getLong("valor_total_centavos"),
                          rs.getString("status"),
                          rs.getString("motivo_falha"),
                          ingressoIds,
                          rs.getObject("criado_em", OffsetDateTime.class));
    }
}
