package br.com.ticket.wizard.gateway.web;

import br.com.ticket.wizard.gateway.clientes.IngressosClient;
import br.com.ticket.wizard.gateway.seguranca.FiltroJwt;
import br.com.ticket.wizard.gateway.seguranca.UsuarioAutenticado;
import br.com.ticket.wizard.gateway.web.dto.IngressoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingressos")
public class IngressoController {

    private final IngressosClient ingressos;

    public IngressoController(IngressosClient ingressos) {
        this.ingressos = ingressos;
    }

    @GetMapping
    public List<IngressoDto> listar(@RequestAttribute(FiltroJwt.ATRIBUTO_USUARIO) UsuarioAutenticado usuario) {
        return ingressos.listarIngressos(usuario.id());
    }
}
