package br.com.ticket.wizard.gateway.seguranca;

import br.com.ticket.wizard.gateway.web.Problemas;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class FiltroJwt extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO = "usuarioAutenticado";

    private static final Logger LOG = LoggerFactory.getLogger(FiltroJwt.class);
    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwt;
    private final ObjectMapper json;

    public FiltroJwt(JwtService jwt, ObjectMapper json) {
        this.jwt = jwt;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return !caminho.startsWith("/api/")
               || caminho.startsWith("/api/auth/")
               || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadeia)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            recusar(request, response, "Token de autenticação ausente. Envie o cabeçalho 'Authorization: Bearer <token>'.");
            return;
        }

        Optional<UsuarioAutenticado> usuario = jwt.validar(cabecalho.substring(PREFIXO_BEARER.length()).trim());
        if (usuario.isEmpty()) {
            recusar(request, response, "Token inválido ou expirado. Faça login novamente.");
            return;
        }

        request.setAttribute(ATRIBUTO_USUARIO, usuario.get());
        cadeia.doFilter(request, response);
    }

    private void recusar(HttpServletRequest request, HttpServletResponse response, String detalhe) throws IOException {
        LOG.info("401 {} {}: {}", request.getMethod(), request.getRequestURI(), detalhe);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), Problemas.criar(HttpStatus.UNAUTHORIZED, detalhe));
    }
}
