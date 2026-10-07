package br.com.cinepass.reputacao.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** {@code event_id} único: um evento gera no máximo uma linha de histórico, mesmo reentregue. */
@Entity
@Table(name = "historico_reputacao",
        uniqueConstraints = @UniqueConstraint(name = "uk_historico_evento", columnNames = "event_id"),
        indexes = @Index(name = "idx_historico_cliente", columnList = "cliente_id, ocorrido_em"))
public class HistoricoJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "cliente_id", nullable = false) private UUID clienteId;
    @Column(name = "reserva_id", nullable = false) private UUID reservaId;
    @Column(name = "tipo_evento", nullable = false, length = 80) private String tipoEvento;
    @Column(nullable = false, length = 200) private String descricao;
    @Column(nullable = false) private long pontos;
    @Column(name = "ocorrido_em", nullable = false) private Instant ocorridoEm;
    @Column(name = "correlation_id", length = 64) private String correlationId;

    protected HistoricoJpaEntity() {}

    HistoricoJpaEntity(UUID eventId, UUID clienteId, UUID reservaId, String tipoEvento, String descricao, long pontos,
                       Instant ocorridoEm, String correlationId) {
        this.eventId = eventId; this.clienteId = clienteId; this.reservaId = reservaId; this.tipoEvento = tipoEvento;
        this.descricao = descricao; this.pontos = pontos; this.ocorridoEm = ocorridoEm; this.correlationId = correlationId;
    }

    public UUID getEventId() { return eventId; } public UUID getClienteId() { return clienteId; } public UUID getReservaId() { return reservaId; }
    public String getTipoEvento() { return tipoEvento; } public String getDescricao() { return descricao; } public long getPontos() { return pontos; }
    public Instant getOcorridoEm() { return ocorridoEm; } public String getCorrelationId() { return correlationId; }
}
