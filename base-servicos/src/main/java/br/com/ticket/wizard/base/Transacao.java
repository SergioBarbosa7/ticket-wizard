package br.com.ticket.wizard.base;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class Transacao {

    @FunctionalInterface
    public interface Operacao<T> {
        T executar(Connection conexao) throws SQLException;
    }

    private Transacao() {
    }

    public static <T> T executar(DataSource dataSource, Operacao<T> operacao) {
        try (Connection conexao = dataSource.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                T resultado = operacao.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                throw erro;
            }
        } catch (SQLException erro) {
            throw new ErroDeAcessoADados("Falha na transação: " + erro.getMessage(), erro);
        }
    }
}
