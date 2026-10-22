package br.com.ticket.wizard.gateway.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErros.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro,
                                                                  HttpHeaders cabecalhos,
                                                                  HttpStatusCode status,
                                                                  WebRequest requisicao) {
        List<Map<String, String>> campos = erro.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(), "mensagem", String.valueOf(e.getDefaultMessage())))
                .sorted((a, b) -> a.get("campo").compareTo(b.get("campo")))
                .collect(Collectors.toList());
        ProblemDetail problema = Problemas.criar(HttpStatus.BAD_REQUEST, "Há campos inválidos ou ausentes na requisição.");
        problema.setProperty("erros", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException erro,
                                                                  HttpHeaders cabecalhos,
                                                                  HttpStatusCode status,
                                                                  WebRequest requisicao) {
        ProblemDetail problema = Problemas.criar(HttpStatus.BAD_REQUEST,
                                                 "O corpo da requisição está ausente ou não é um JSON válido.");
        if (erro.getCause() instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
            String campo = mapeamento.getPath().stream()
                                     .map(JsonMappingException.Reference::getFieldName)
                                     .filter(nome -> nome != null)
                                     .collect(Collectors.joining("."));
            problema.setDetail("O valor informado para '" + campo + "' é inválido.");
            problema.setProperty("erros", List.of(Map.of("campo", campo, "mensagem", "Valor com formato inválido.")));
        }
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ProblemDetail acessoNegado(AcessoNegadoException erro) {
        return Problemas.criar(HttpStatus.FORBIDDEN, erro.getMessage());
    }

    @ExceptionHandler(StatusRuntimeException.class)
    ProblemDetail erroGrpc(StatusRuntimeException erro) {
        Status status = erro.getStatus();
        HttpStatus http = traduzir(status.getCode());
        LOG.info("gRPC {} -> HTTP {}: {}", status.getCode(), http.value(), status.getDescription());

        // Mensagens de erro do cliente (4xx) vêm dos serviços e são seguras de exibir.
        // Falhas de infraestrutura (5xx) não vazam detalhe interno.
        String detalhe = http.is4xxClientError() && status.getDescription() != null
                ? status.getDescription()
                : mensagemGenerica(http);
        return Problemas.criar(http, detalhe);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception erro) {
        LOG.error("Erro inesperado no Gateway", erro);
        return Problemas.criar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");
    }

    static HttpStatus traduzir(Status.Code codigo) {
        return switch (codigo) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS, FAILED_PRECONDITION -> HttpStatus.CONFLICT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private static String mensagemGenerica(HttpStatus http) {
        return switch (http) {
            case SERVICE_UNAVAILABLE -> "Serviço temporariamente indisponível. Tente novamente em instantes.";
            case GATEWAY_TIMEOUT -> "O serviço demorou demais para responder.";
            default -> "Falha ao se comunicar com um serviço interno.";
        };
    }
}
