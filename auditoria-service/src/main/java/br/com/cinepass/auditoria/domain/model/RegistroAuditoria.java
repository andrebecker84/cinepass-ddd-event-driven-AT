package br.com.cinepass.auditoria.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro imutável de um evento processado pela plataforma. Guarda o envelope inteiro e,
 * além dele, onde o evento estava no Kafka (tópico, partição, offset) e em qual trace foi
 * consumido. Com isso a auditoria responde "o que aconteceu com a reserva X, em que ordem,
 * a pedido de qual operação", e aponta para o trace correspondente no Zipkin.
 *
 * <p>É um registro, não um agregado: não tem comportamento nem muda depois de gravado.
 */
public record RegistroAuditoria(UUID eventId, String eventType, int eventVersion, UUID reservaId, long sequence,
                                Instant occurredAt, String correlationId, String producer, String traceId,
                                String topico, int particao, long offset, String payload, Instant registradoEm) {
    public RegistroAuditoria {
        Objects.requireNonNull(eventId); Objects.requireNonNull(eventType); Objects.requireNonNull(reservaId);
        Objects.requireNonNull(occurredAt); Objects.requireNonNull(payload); Objects.requireNonNull(registradoEm);
    }
}
