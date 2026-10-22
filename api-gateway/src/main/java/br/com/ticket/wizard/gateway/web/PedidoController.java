package br.com.ticket.wizard.gateway.web;

import br.com.ticket.wizard.gateway.clientes.PedidosClient;
import br.com.ticket.wizard.gateway.seguranca.FiltroJwt;
import br.com.ticket.wizard.gateway.seguranca.UsuarioAutenticado;
import br.com.ticket.wizard.gateway.web.dto.PedidoDto;
import br.com.ticket.wizard.gateway.web.dto.PedidoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidosClient pedidos;

    public PedidoController(PedidosClient pedidos) {
        this.pedidos = pedidos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto realizar(@Valid @RequestBody PedidoRequest dados,
                              @RequestAttribute(FiltroJwt.ATRIBUTO_USUARIO) UsuarioAutenticado usuario) {
        return pedidos.realizar(dados, usuario.id());
    }

    @GetMapping
    public List<PedidoDto> listar(@RequestAttribute(FiltroJwt.ATRIBUTO_USUARIO) UsuarioAutenticado usuario) {
        return pedidos.listar(usuario.id());
    }
}
