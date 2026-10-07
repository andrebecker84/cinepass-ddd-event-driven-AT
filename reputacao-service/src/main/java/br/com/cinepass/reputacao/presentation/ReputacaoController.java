package br.com.cinepass.reputacao.presentation;

import br.com.cinepass.mensageria.inbox.ReprocessadorDeDlt;
import br.com.cinepass.reputacao.application.ReputacaoApplicationService;
import br.com.cinepass.reputacao.domain.model.RegistroHistorico;
import br.com.cinepass.reputacao.domain.model.ReputacaoCliente;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reputacao")
public class ReputacaoController {

    private final ReputacaoApplicationService service;
    private final ReprocessadorDeDlt reprocessador;

    public ReputacaoController(ReputacaoApplicationService service, ReprocessadorDeDlt reprocessador) {
        this.service = service;
        this.reprocessador = reprocessador;
    }

    @GetMapping("/clientes")
    public List<ReputacaoResponse> todas() {
        return service.todas().stream().map(r -> ReputacaoResponse.de(r, null)).toList();
    }

    @GetMapping("/clientes/{clienteId}")
    public ResponseEntity<ReputacaoResponse> doCliente(@PathVariable UUID clienteId) {
        return service.reputacao(clienteId)
                .map(r -> ResponseEntity.ok(ReputacaoResponse.de(r, service.historico(clienteId))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/dlt/reprocessar")
    public ReprocessadorDeDlt.Resultado reprocessarDlt() {
        return reprocessador.reprocessar(service::aplicar);
    }

    public record ReputacaoResponse(UUID clienteId, String nivel, long pontos, int reservasConfirmadas, int reservasCanceladas,
                                    int pagamentosRecusados, BigDecimal valorTotalGasto, Instant atualizadaEm,
                                    List<RegistroHistorico> historico) {
        static ReputacaoResponse de(ReputacaoCliente r, List<RegistroHistorico> historico) {
            return new ReputacaoResponse(r.getClienteId(), r.nivel().name(), r.getPontos(), r.getReservasConfirmadas(),
                    r.getReservasCanceladas(), r.getPagamentosRecusados(), r.getValorTotalGasto(), r.getAtualizadaEm(), historico);
        }
    }
}
