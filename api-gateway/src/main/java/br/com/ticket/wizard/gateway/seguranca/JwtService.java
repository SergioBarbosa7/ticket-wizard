package br.com.ticket.wizard.gateway.seguranca;

import br.com.ticket.wizard.gateway.config.GatewayProperties;
import br.com.ticket.wizard.gateway.web.dto.PapelUsuario;
import br.com.ticket.wizard.gateway.web.dto.UsuarioDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtService {

    private static final String EMISSOR = "ticket-wizard-gateway";
    private static final String CLAIM_PAPEL = "papel";
    // HS256 exige chave de pelo menos 256 bits.
    private static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    private final SecretKey chave;
    private final Duration validade;

    public JwtService(GatewayProperties propriedades) {
        byte[] segredo = propriedades.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (segredo.length < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ter pelo menos " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo);
        this.validade = Duration.ofSeconds(propriedades.jwt().expiracaoSegundos());
    }

    public long expiracaoEmSegundos() {
        return validade.toSeconds();
    }

    public String gerar(UsuarioDto usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                   .issuer(EMISSOR)
                   .subject(usuario.id())
                   .claim(CLAIM_PAPEL, usuario.papel().name())
                   .issuedAt(Date.from(agora))
                   .expiration(Date.from(agora.plus(validade)))
                   .signWith(chave)
                   .compact();
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                                .verifyWith(chave)
                                .requireIssuer(EMISSOR)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
            return Optional.of(new UsuarioAutenticado(UUID.fromString(claims.getSubject()),
                                                      PapelUsuario.valueOf(claims.get(CLAIM_PAPEL, String.class))));
        } catch (JwtException | IllegalArgumentException erro) {
            return Optional.empty();
        }
    }
}
