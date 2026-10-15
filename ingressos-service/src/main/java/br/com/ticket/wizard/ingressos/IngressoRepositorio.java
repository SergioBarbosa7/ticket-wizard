package br.com.ticket.wizard.ingressos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IngressoRepositorio {

    public List<UUID> inserir(Connection conexao, UUID categoriaId, UUID usuarioId, UUID pedidoId, int quantidade)
            throws SQLException {
        String sql = "INSERT INTO ingressos (id, categoria_id, usuario_id, pedido_id) VALUES (?, ?, ?, ?)";
        List<UUID> ids = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            for (int i = 0; i < quantidade; i++) {
                UUID id = UUID.randomUUID();
                ps.setObject(1, id);
                ps.setObject(2, categoriaId);
                ps.setObject(3, usuarioId);
                ps.setObject(4, pedidoId);
                ps.addBatch();
                ids.add(id);
            }
            ps.executeBatch();
        }
        return ids;
    }

    public List<IngressoEmitido> listarPorUsuario(Connection conexao, UUID usuarioId) throws SQLException {
        String sql = "SELECT i.id, i.categoria_id, c.evento_id, c.nome, i.usuario_id, i.pedido_id, i.emitido_em "
                     + "FROM ingressos i JOIN categorias c ON c.id = i.categoria_id "
                     + "WHERE i.usuario_id = ? ORDER BY i.emitido_em DESC, i.id";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setObject(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<IngressoEmitido> ingressos = new ArrayList<>();
                while (rs.next()) {
                    ingressos.add(new IngressoEmitido(rs.getObject("id", UUID.class),
                                                      rs.getObject("categoria_id", UUID.class),
                                                      rs.getString("evento_id"),
                                                      rs.getString("nome"),
                                                      rs.getObject("usuario_id", UUID.class),
                                                      rs.getObject("pedido_id", UUID.class),
                                                      rs.getObject("emitido_em", OffsetDateTime.class)));
                }
                return ingressos;
            }
        }
    }
}
