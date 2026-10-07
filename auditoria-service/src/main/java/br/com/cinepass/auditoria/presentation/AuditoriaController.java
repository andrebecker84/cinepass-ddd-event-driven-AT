package br.com.cinepass.auditoria.presentation;

import br.com.cinepass.auditoria.application.AuditoriaApplicationService;
import br.com.cinepass.auditoria.domain.model.RegistroAuditoria;
import br.com.cinepass.auditoria.domain.repository.AuditoriaRepository;
import br.com.cinepass.mensageria.inbox.ReprocessadorDeDlt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consulta da trilha de auditoria. Exemplos:
 * <pre>
 *   GET /api/auditoria/eventos?reservaId=...       ciclo de vida de uma reserva, em ordem
 *   GET /api/auditoria/eventos?correlationId=...   tudo o que uma operação externa gerou
 *   GET /api/auditoria/eventos?eventType=ReservaCancelada
 *   GET /api/auditoria/eventos/{eventId}
 * </pre>
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaApplicationService service;
    private final ReprocessadorDeDlt reprocessador;
    private final JsonMapper jsonMapper;

    public AuditoriaController(AuditoriaApplicationService service, ReprocessadorDeDlt reprocessador, JsonMapper jsonMapper) {
        this.service = service;
        this.reprocessador = reprocessador;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/eventos")
    public List<RegistroResponse> consultar(@RequestParam(required = false) UUID reservaId,
                                            @RequestParam(required = false) String correlationId,
                                            @RequestParam(required = false) String eventType,
                                            @RequestParam(required = false) UUID eventId) {
        return service.consultar(new AuditoriaRepository.Filtro(reservaId, correlationId, eventType, eventId)).stream()
                .map(this::resposta).toList();
    }

    @GetMapping("/eventos/{eventId}")
    public ResponseEntity<RegistroResponse> porEvento(@PathVariable UUID eventId) {
        return service.porEvento(eventId).map(r -> ResponseEntity.ok(resposta(r))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/dlt/reprocessar")
    public ReprocessadorDeDlt.Resultado reprocessarDlt() {
        // Na DLT não há metadado de origem confiável para a auditoria; registra a posição da própria DLT.
        return reprocessador.reprocessar(evento -> service.registrar(evento,
                new AuditoriaApplicationService.Origem("dlt", -1, -1, null)));
    }

    private RegistroResponse resposta(RegistroAuditoria r) {
        return new RegistroResponse(r.eventId(), r.eventType(), r.eventVersion(), r.reservaId(), r.sequence(), r.occurredAt(),
                r.correlationId(), r.producer(), r.traceId(), r.topico(), r.particao(), r.offset(), r.registradoEm(),
                jsonMapper.readTree(r.payload()));
    }

    public record RegistroResponse(UUID eventId, String eventType, int eventVersion, UUID reservaId, long sequence,
                                   Instant occurredAt, String correlationId, String producer, String traceId, String topico,
                                   int particao, long offset, Instant registradoEm, JsonNode payload) {
    }
}
