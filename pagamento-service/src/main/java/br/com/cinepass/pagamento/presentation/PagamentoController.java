package br.com.cinepass.pagamento.presentation;

import br.com.cinepass.pagamento.application.CriarPagamentoCommand;
import br.com.cinepass.pagamento.application.PagamentoApplicationService;
import br.com.cinepass.pagamento.domain.model.Pagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {
    private final PagamentoApplicationService service;

    public PagamentoController(PagamentoApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse criar(@Valid @RequestBody CriarPagamentoRequest request) {
        return resposta(service.processar(new CriarPagamentoCommand(
                request.reservaId(), request.valor(), request.simularRecusa())));
    }

    @PostMapping("/{id}/estorno")
    public PagamentoResponse estornar(@PathVariable UUID id) {
        return resposta(service.estornar(id));
    }

    @GetMapping("/{id}")
    public PagamentoResponse buscar(@PathVariable UUID id) {
        return resposta(service.buscar(id));
    }

    @GetMapping("/reserva/{reservaId}")
    public PagamentoResponse porReserva(@PathVariable UUID reservaId) {
        return resposta(service.buscarPorReserva(reservaId));
    }

    private PagamentoResponse resposta(Pagamento p) {
        return new PagamentoResponse(p.getId().valor(), p.getReservaId().valor(), p.getValor().valor(),
                p.getStatus().name(), p.getCriadoEm());
    }

    public record CriarPagamentoRequest(
            @NotNull UUID reservaId,
            @NotNull @DecimalMin("0.01") BigDecimal valor,
            boolean simularRecusa) {}

    public record PagamentoResponse(UUID pagamentoId, UUID reservaId, BigDecimal valor, String status, Instant criadoEm) {}
}
