package br.com.ticket.wizard.base;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public final class BancoDeDados {

    private static final Logger LOG = LoggerFactory.getLogger(BancoDeDados.class);

    private BancoDeDados() {
    }

    // A senha não tem padrão: segredo não fica no código.
    public static DataSource conectar(String nomeDoBanco) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Ambiente.ou("DB_URL", "jdbc:postgresql://localhost:5432/" + nomeDoBanco));
        config.setUsername(Ambiente.ou("DB_USER", "ticket"));
        config.setPassword(Ambiente.obrigatoria("DB_PASSWORD"));
        config.setMaximumPoolSize(Ambiente.inteiro("DB_POOL_SIZE", 10));
        // O banco pode demorar a subir junto com o compose: espera antes de desistir.
        config.setInitializationFailTimeout(60_000);

        HikariDataSource dataSource = new HikariDataSource(config);
        LOG.info("Conectado ao banco {}", config.getJdbcUrl());

        var resultado = Flyway.configure()
                              .dataSource(dataSource)
                              .locations("classpath:db/migration")
                              .load()
                              .migrate();
        LOG.info("Migrações aplicadas: {}", resultado.migrationsExecuted);
        return dataSource;
    }
}
