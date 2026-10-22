package br.com.ticket.wizard.gateway.web;

import br.com.ticket.wizard.gateway.clientes.IngressosClient;
import br.com.ticket.wizard.gateway.seguranca.FiltroJwt;
import br.com.ticket.wizard.gateway.seguranca.UsuarioAutenticado;
import br.com.ticket.wizard.gateway.web.dto.CategoriaDto;
import br.com.ticket.wizard.gateway.web.dto.CategoriaRequest;
import br.com.ticket.wizard.gateway.web.dto.PapelUsuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final IngressosClient ingressos;

    public CategoriaController(IngressosClient ingressos) {
        this.ingressos = ingressos;
    }

    @GetMapping
    public List<CategoriaDto> listar(@RequestParam(required = false) String eventoId) {
        return ingressos.listarCategorias(eventoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDto cadastrar(@Valid @RequestBody CategoriaRequest dados,
                                  @RequestAttribute(FiltroJwt.ATRIBUTO_USUARIO) UsuarioAutenticado usuario) {
        if (usuario.papel() != PapelUsuario.ORGANIZADOR) {
            throw new AcessoNegadoException("Apenas organizadores podem cadastrar categorias de ingresso.");
        }
        return ingressos.cadastrarCategoria(dados, usuario.id());
    }
}
