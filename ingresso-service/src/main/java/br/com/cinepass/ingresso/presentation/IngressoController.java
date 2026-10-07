package br.com.cinepass.ingresso.presentation;

import br.com.cinepass.ingresso.application.EmitirIngressoCommand;
import br.com.cinepass.ingresso.application.IngressoApplicationService;
import br.com.cinepass.ingresso.domain.model.Ingresso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingressos")
public class IngressoController {
    private final IngressoApplicationService service;
    public IngressoController(IngressoApplicationService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngressoResponse emitir(@Valid @RequestBody EmitirIngressoRequest request) {
        return resposta(service.emitir(new EmitirIngressoCommand(request.reservaId(), request.sessaoId(), request.assentos(), request.simularFalha())));
    }

    @PostMapping("/{id}/cancelamento")
    public IngressoResponse cancelar(@PathVariable UUID id) { return resposta(service.cancelar(id)); }

    @GetMapping("/reserva/{reservaId}")
    public IngressoResponse porReserva(@PathVariable UUID reservaId) { return resposta(service.buscarPorReserva(reservaId)); }

    private IngressoResponse resposta(Ingresso i) {
        return new IngressoResponse(i.getId().valor(), i.getReservaId().valor(), i.getSessaoId().valor(), i.getAssentos(),
                i.getCodigo().valor(), i.getStatus().name(), i.getEmitidoEm());
    }

    public record EmitirIngressoRequest(@NotNull UUID reservaId, @NotNull UUID sessaoId, @NotEmpty List<String> assentos, boolean simularFalha) {}
    public record IngressoResponse(UUID ingressoId, UUID reservaId, UUID sessaoId, List<String> assentos, String codigo, String status, Instant emitidoEm) {}
}
