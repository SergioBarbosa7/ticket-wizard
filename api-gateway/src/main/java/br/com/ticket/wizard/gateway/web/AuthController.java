package br.com.ticket.wizard.gateway.web;

import br.com.ticket.wizard.gateway.clientes.UsuariosClient;
import br.com.ticket.wizard.gateway.seguranca.JwtService;
import br.com.ticket.wizard.gateway.web.dto.LoginRequest;
import br.com.ticket.wizard.gateway.web.dto.RegistroRequest;
import br.com.ticket.wizard.gateway.web.dto.TokenResponse;
import br.com.ticket.wizard.gateway.web.dto.UsuarioDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuariosClient usuarios;
    private final JwtService jwt;

    public AuthController(UsuariosClient usuarios, JwtService jwt) {
        this.usuarios = usuarios;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto cadastrar(@Valid @RequestBody RegistroRequest dados) {
        return usuarios.cadastrar(dados);
    }

    @PostMapping("/login")
    public TokenResponse entrar(@Valid @RequestBody LoginRequest dados) {
        UsuarioDto usuario = usuarios.autenticar(dados);
        return TokenResponse.bearer(jwt.gerar(usuario), jwt.expiracaoEmSegundos(), usuario);
    }
}
