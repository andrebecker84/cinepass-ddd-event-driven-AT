package br.com.cinepass.auditoria.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A chave primária é o próprio {@code eventId}: o mesmo evento não pode ser auditado duas
 * vezes. Os índices cobrem as consultas pedidas: por reserva, por correlação e por tipo.
 */
@Entity
@Table(name = "registros_auditoria", indexes = {
        @Index(name = "idx_auditoria_reserva", columnList = "reserva_id, sequence"),
        @Index(name = "idx_auditoria_correlation", columnList = "correlation_id"),
        @Index(name = "idx_auditoria_tipo", columnList = "event_type")})
public class RegistroAuditoriaJpaEntity {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "event_version", nullable = false) private int eventVersion;
    @Column(name = "reserva_id", nullable = false) private UUID reservaId;
    @Column(nullable = false) private long sequence;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "correlation_id", length = 64) private String correlationId;
    @Column(length = 80) private String producer;
    @Column(name = "trace_id", length = 64) private String traceId;
    @Column(nullable = false, length = 120) private String topico;
    @Column(nullable = false) private int particao;
    @Column(name = "kafka_offset", nullable = false) private long offset;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(name = "registrado_em", nullable = false) private Instant registradoEm;

    protected RegistroAuditoriaJpaEntity() {}

    RegistroAuditoriaJpaEntity(UUID eventId, String eventType, int eventVersion, UUID reservaId, long sequence, Instant occurredAt,
                               String correlationId, String producer, String traceId, String topico, int particao, long offset,
                               String payload, Instant registradoEm) {
        this.eventId = eventId; this.eventType = eventType; this.eventVersion = eventVersion; this.reservaId = reservaId;
        this.sequence = sequence; this.occurredAt = occurredAt; this.correlationId = correlationId; this.producer = producer;
        this.traceId = traceId; this.topico = topico; this.particao = particao; this.offset = offset; this.payload = payload;
        this.registradoEm = registradoEm;
    }

    public UUID getEventId() { return eventId; } public String getEventType() { return eventType; } public int getEventVersion() { return eventVersion; }
    public UUID getReservaId() { return reservaId; } public long getSequence() { return sequence; } public Instant getOccurredAt() { return occurredAt; }
    public String getCorrelationId() { return correlationId; } public String getProducer() { return producer; } public String getTraceId() { return traceId; }
    public String getTopico() { return topico; } public int getParticao() { return particao; } public long getOffset() { return offset; }
    public String getPayload() { return payload; } public Instant getRegistradoEm() { return registradoEm; }
}
