package br.com.cinepass.reserva.presentation;

import br.com.cinepass.mensageria.outbox.AdministracaoOutbox;
import br.com.cinepass.mensageria.outbox.OutboxMensagem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Visão do outbox, para diagnóstico e demonstração.
 *
 * <ul>
 *   <li>{@code GET /api/reservas/{id}/eventos}: os eventos que a reserva gerou, em ordem, e
 *       se cada um já foi publicado no Kafka.</li>
 *   <li>{@code POST /api/reservas/eventos/{eventId}/republicar}: devolve um evento já
 *       publicado à fila do relay. Ele sai de novo, idêntico, com o mesmo eventId: é a
 *       reentrega que os consumidores precisam absorver sem efeito duplicado.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reservas")
public class EventosDaReservaController {

    private final AdministracaoOutbox outbox;

    public EventosDaReservaController(AdministracaoOutbox outbox) {
        this.outbox = outbox;
    }

    @GetMapping("/{reservaId}/eventos")
    public List<EventoNoOutbox> eventos(@PathVariable UUID reservaId) {
        return outbox.daReserva(reservaId).stream().map(EventoNoOutbox::de).toList();
    }

    @PostMapping("/eventos/{eventId}/republicar")
    public ResponseEntity<Map<String, Object>> republicar(@PathVariable UUID eventId) {
        if (!outbox.republicar(eventId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.accepted().body(Map.of("eventId", eventId,
                "situacao", "devolvido ao outbox; será publicado de novo com o mesmo eventId"));
    }

    public record EventoNoOutbox(Long ordem, UUID eventId, String eventType, String topico, String chave,
                                 String correlationId, Instant criadoEm, Instant publicadoEm, int tentativas) {
        static EventoNoOutbox de(OutboxMensagem m) {
            return new EventoNoOutbox(m.getId(), m.getEventId(), m.getEventType(), m.getTopico(), m.getChave(),
                    m.getCorrelationId(), m.getCriadoEm(), m.getPublicadoEm(), m.getTentativas());
        }
    }
}
