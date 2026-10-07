package br.com.cinepass.notificacao.presentation;

import br.com.cinepass.mensageria.inbox.ReprocessadorDeDlt;
import br.com.cinepass.notificacao.application.NotificacaoApplicationService;
import br.com.cinepass.notificacao.domain.model.Destinatario;
import br.com.cinepass.notificacao.domain.model.Notificacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoApplicationService service;
    private final ReprocessadorDeDlt reprocessador;

    public NotificacaoController(NotificacaoApplicationService service, ReprocessadorDeDlt reprocessador) {
        this.service = service;
        this.reprocessador = reprocessador;
    }

    /** Notificações de uma reserva, ou as 50 mais recentes. */
    @GetMapping
    public List<Notificacao> listar(@RequestParam(required = false) UUID reservaId) {
        return reservaId == null ? service.ultimas() : service.daReserva(reservaId);
    }

    /** Os contatos aparecem mascarados: a API também não expõe o e-mail completo. */
    @GetMapping("/destinatarios")
    public List<DestinatarioResponse> destinatarios() {
        return service.destinatarios().stream().map(DestinatarioResponse::de).toList();
    }

    @PutMapping("/destinatarios/{clienteId}")
    public DestinatarioResponse cadastrar(@PathVariable UUID clienteId, @Valid @RequestBody DestinatarioRequest request) {
        return DestinatarioResponse.de(service.cadastrarDestinatario(clienteId, request.nome(), request.email()));
    }

    /** Reprocessa as mensagens da DLT deste consumidor, na ordem em que falharam. */
    @PostMapping("/dlt/reprocessar")
    public ReprocessadorDeDlt.Resultado reprocessarDlt() {
        return reprocessador.reprocessar(service::registrar);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalido(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    public record DestinatarioRequest(@NotBlank String nome, @NotBlank @Email String email) {}

    public record DestinatarioResponse(UUID clienteId, String nome, String email) {
        static DestinatarioResponse de(Destinatario d) {
            return new DestinatarioResponse(d.clienteId(), d.nome(), d.email().mascarado());
        }
    }
}
