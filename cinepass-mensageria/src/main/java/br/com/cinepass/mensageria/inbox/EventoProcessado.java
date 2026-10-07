package br.com.cinepass.mensageria.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbox: um registro por evento já processado por este serviço. A chave primária é o
 * {@code eventId}, e é ela que torna o reprocessamento inofensivo. Cada serviço tem o
 * seu banco, então a mesma chave não colide entre consumidores.
 *
 * <p>Implementa {@link Persistable} para que o Spring Data faça {@code insert} direto, sem o
 * {@code select} prévio do {@code merge}: com duas entregas simultâneas do mesmo evento, a
 * segunda falha na chave primária em vez de passar despercebida.
 */
@Entity
@Table(name = "eventos_processados")
public class EventoProcessado implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(nullable = false, length = 40)
    private String consumidor;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    @Transient
    private boolean novo = true;

    protected EventoProcessado() {
        this.novo = false;
    }

    public EventoProcessado(UUID eventId, String eventType, UUID reservaId, String correlationId,
                            String consumidor, Instant processadoEm) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.reservaId = reservaId;
        this.correlationId = correlationId;
        this.consumidor = consumidor;
        this.processadoEm = processadoEm;
    }

    @Override
    public UUID getId() { return eventId; }

    @Override
    public boolean isNew() { return novo; }

    public String getEventType() { return eventType; }
    public UUID getReservaId() { return reservaId; }
    public String getCorrelationId() { return correlationId; }
    public String getConsumidor() { return consumidor; }
    public Instant getProcessadoEm() { return processadoEm; }
}
