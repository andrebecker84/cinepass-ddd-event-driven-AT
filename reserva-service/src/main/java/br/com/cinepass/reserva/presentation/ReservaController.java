package br.com.cinepass.reserva.presentation;

import br.com.cinepass.reserva.application.RealizarReservaCommand;
import br.com.cinepass.reserva.application.ReservaApplicationService;
import br.com.cinepass.reserva.application.ReservaDetalhe;
import br.com.cinepass.reserva.application.port.ReservaOrquestrador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
    private static final Logger log = LoggerFactory.getLogger(ReservaController.class);

    private final ReservaApplicationService service;
    private final ReservaOrquestrador reservaOrquestrador;
    public ReservaController(ReservaApplicationService service, ReservaOrquestrador reservaOrquestrador){
        this.service=service;
        this.reservaOrquestrador = reservaOrquestrador;
    }

    /** Executa a Saga da reserva. Os eventos de cada passo saem pelo outbox, sem chamada aos serviços auxiliares. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDetalhe realizar(@Valid @RequestBody CriarReservaRequest request){
        log.info("Requisição de reserva recebida: sessao={} assentos={}", request.sessaoId(), request.assentos());
        return reservaOrquestrador.realizar(new RealizarReservaCommand(request.clienteId(), request.sessaoId(), request.assentos(),
                request.simularRecusaPagamento(), request.simularFalhaIngresso()));
    }

    @GetMapping("/{id}")
    public ReservaDetalhe buscar(@PathVariable UUID id){return service.buscar(id);}

    @GetMapping
    public List<ReservaDetalhe> listar(){return service.listar();}

    public record CriarReservaRequest(@NotNull UUID clienteId, @NotNull UUID sessaoId, @NotEmpty List<String> assentos,
                                      boolean simularRecusaPagamento, boolean simularFalhaIngresso) {}
}
